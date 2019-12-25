package sk.task.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import sk.task.exec.TaskExecutorService;
import sk.task.msg.Input;

@Singleton
public class StringConsumer implements Callable<Long> {

    private static Logger logger = LoggerFactory.getLogger(StringConsumer.class);

    private final ObjectMapper mapper;

    private final TaskExecutorService tes;

    private final Duration pollTimeout;

    private final KafkaConsumer<String, String> consumer;

    private boolean consumerPaused = false;

    private long counter = 0;

    // TopicPartition to offset map - used to keep track of offsets of processed messages.
    private final Map<TopicPartition, Long> tp2o = new ConcurrentHashMap<>();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    @Inject
    public StringConsumer(final TaskExecutorService tes,
                          @Named("NS") final Namespace ns) {
        mapper = new ObjectMapper();
        this.tes = tes;
        pollTimeout = Duration.ofMillis(ns.getInt("poll.timeout.ms"));
        consumer = new KafkaConsumer<>(getProps(ns));
        consumer.subscribe(Collections.singletonList(ns.getString("topic")), getListener());
    }

    private Properties getProps(final Namespace ns) {
        final Properties props = new Properties();
        props.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ns.getString(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        props.setProperty(ConsumerConfig.GROUP_ID_CONFIG, ns.getString(ConsumerConfig.GROUP_ID_CONFIG));
        props.setProperty(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, ns.getString(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
        props.setProperty(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, ns.getString(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG));

        props.setProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.setProperty(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.setProperty(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        return props;
    }

    private ConsumerRebalanceListener getListener() {
        return new ConsumerRebalanceListener() {
            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                logger.info("Partitions revoked: {}", partitions);
                commitOffsets();
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                logger.info("Partitions assigned: {}", partitions);

                tp2o.clear();

                for (TopicPartition tp : partitions) {
                    final OffsetAndMetadata metadata = consumer.committed(tp);
                    final long offset = metadata != null ? metadata.offset() : -1;
                    logger.info("Partition assigned: topic {}, partition {}, offset {}",
                            tp.topic(), tp.partition(), offset);
                    tp2o.put(tp, offset);
                }
            }
        };
    }

    private void commitOffsets() {
        consumer.commitSync(tp2o.entrySet().stream()
                .filter(e -> e.getValue() > -1)
                .map(e -> {
                    logger.info("Committing offset TopicPartition {} Offset {}", e.getKey(), e.getValue());
                    return new AbstractMap.SimpleEntry<>(e.getKey(), new OffsetAndMetadata(e.getValue() + 1));
                })
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue)));
    }

    public void shutdown() {
        logger.info("Signalling to shutdown consumer");
        closed.set(true);
        consumer.wakeup();
    }

    private void processTest(final ConsumerRecords<String, String> records) {
        try {
            logger.info("Sleeping");
            Thread.sleep(10000);
            logger.info("Wokeup");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // logger.info("Pausing consumer");
        //consumer.pause(consumer.assignment());
    }

    private List<Input> inputs(final ConsumerRecords<String, String> records) {
        return StreamSupport.stream(records.spliterator(), false)
                .flatMap(rec -> {
                    TopicPartition tp = new TopicPartition(rec.topic(), rec.partition());
                    tp2o.put(tp, rec.offset());
                    List<Input> input;
                    try {
                        input = Collections.singletonList(Input.fromJson(mapper, rec.value()));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        input = Collections.<Input>emptyList();
                    }
                    return input.stream();
                })
                .collect(Collectors.toList());
    }

    private void pause() {
        logger.info("Pausing consumer");
        consumer.pause(consumer.assignment());
        consumerPaused = true;
    }

    private void resume() {
        tp2o.entrySet().stream()
                .filter(e -> e.getValue() > -1)
                .forEach(e -> logger.info("Processed messages - TopicPartition {} Offset {}", e.getKey(), e.getValue()));
        logger.info("Resuming consumer");
        consumer.resume(consumer.assignment());
        consumerPaused = false;
    }

    private void process(final ConsumerRecords<String, String> records) {
        logger.info("Submitting {} records", records.count());
        tes.submit(inputs(records));
        pause();
    }

    private void mainLoop() throws InterruptedException {
        while (!closed.get()) {
            logger.info("Polling for records");

            final ConsumerRecords<String, String> records = consumer.poll(pollTimeout);
            counter += records.count();

            logger.info("Poll returned {} records", records.count());
            if (!records.isEmpty()) {
                process(records);
            }

            if (consumerPaused && tes.ready()) {
                resume();
            }

            if (consumerPaused) {
                // Thread.sleep(pollTimeout.toMillis());
            }
        }
    }

    @Override
    public Long call() throws Exception {
        logger.info("Starting to process messages");
        boolean wakeupCaught = false;

        try {
            mainLoop();
            logger.info("mainLoop finished");
        } catch (WakeupException e) {
            wakeupCaught = true;
            logger.info("Received WakeupException");
            if (!closed.get()) {
                logger.error("Unexpected error", e);
                throw e;
            }
            logger.info("Shutting down consumer");
        } catch (Exception e) {
            logger.error("Unexpected exception", e);
        } finally {
            if (closed.get() && !wakeupCaught) {
                try {
                    // Poll once to absorb WakeupException, else commitOffsets will fail.
                    consumer.poll(pollTimeout);
                } catch (WakeupException e) {
                    logger.debug("Received expected WakeupException", e);
                }
            }
            commitOffsets();
            logger.info("Closing consumer");
            consumer.close();
        }

        logger.info("Processed {} messages", counter);
        return counter;
    }
}
