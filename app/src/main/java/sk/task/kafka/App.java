package sk.task.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Guice;
import com.google.inject.Injector;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sk.task.exec.Task;
import sk.task.exec.TaskExecutorService;
import sk.task.exec.TaskExecutorServiceImpl;
import sk.task.exec.TaskLocatorService;
import sk.task.msg.Input;
import sk.task.msg.Output;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class App {
    private static Logger logger = LoggerFactory.getLogger(App.class);

    private static Namespace parseArgs(String[] args) {
        final ArgumentParser parser = ArgumentParsers.newFor("KafkaTask").build()
                .defaultHelp(true)
                .description("Task queue implementation off Kafka");

        parser.addArgument("--" + ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)
                .help("Kafka bootstrap servers")
                .setDefault("127.0.0.1:9092");

        parser.addArgument("--" + ConsumerConfig.GROUP_ID_CONFIG)
                .help("Kafka consumer group id")
                .setDefault("task1");
        //.setDefault("task" + "_" + System.nanoTime());

        parser.addArgument("--" + ConsumerConfig.MAX_POLL_RECORDS_CONFIG)
                .help("The maximum number of records returned in a single call to poll")
                .setDefault("100");

        parser.addArgument("--" + ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG)
                .help("The maximum delay between invocations of poll")
                .setDefault("20000");

        parser.addArgument("--poll.timeout.ms")
                .help("Poll timeout in milliseconds")
                .type(Integer.class)
                .setDefault(5000);

        parser.addArgument("--topic")
                .help("Kafka topic name")
                .setDefault("test");

        parser.addArgument("--nt")
                .help("Number of threads to execute tasks")
                .type(Integer.class)
                .setDefault(50);

        parser.addArgument("--task")
                .help("Name of task to execute");

        parser.addArgument("--input")
                .help("Serialized Input class for task to execute");

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        return ns;
    }

    private static void execTask(final Namespace ns) {
        final String tid = ns.getString("task");
        final String in = ns.getString("input");
        if (tid != null && in != null) {
            final ObjectMapper mapper = new ObjectMapper();
            try {
                final Input input = Input.fromJson(mapper, in);
                TaskLocatorService tls = new TaskLocatorService();
                final Task task = tls.task(tid);
                final Output out = task.exec(input);
                System.out.print("Output:" + out.toJson(mapper));
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        }
    }

    private static void runDaemon(final Namespace ns) throws InterruptedException {
        final TaskExecutorService tes = new TaskExecutorServiceImpl(new TaskLocatorService(), ns.getInt("nt"));
        final StringConsumer consumer = new StringConsumer(tes, ns);
        run(ns, consumer, tes);
    }

    private static void run(final Namespace ns,
                            final StringConsumer consumer,
                            final TaskExecutorService tes) throws InterruptedException {
        final long sleepFor = 300000;

        logger.info("Starting Kafka task consumer - topic {} group id {}",
                ns.getString("topic"),
                ns.getString(ConsumerConfig.GROUP_ID_CONFIG));

        final Future<Long> cf = submit(consumer);

        Thread st = new Thread(() -> shutdown(tes, consumer, cf), "shutdown-handler");
        st.setDaemon(false);
        Runtime.getRuntime().addShutdownHook(st);

        logger.info("Sleeping for {} millis", sleepFor);
        Thread.sleep(sleepFor);
        shutdown(tes, consumer, cf);
    }

    private static void runModule(final Namespace ns) throws InterruptedException {
        final Injector injector = Guice.createInjector(new KafkaTaskModule(ns));
        final TaskExecutorService tes = injector.getInstance(TaskExecutorService.class);
        final StringConsumer consumer = injector.getInstance(StringConsumer.class);
        run(ns, consumer, tes);
    }

    public static void main(String[] args) throws Exception {
        final Namespace ns = parseArgs(args);

        if (ns.getString("task") != null) {
            execTask(ns);
        } else {
            // runDaemon(ns);
            runModule(ns);
        }
    }

    private static void shutdown(
            final TaskExecutorService tes,
            final StringConsumer consumer,
            final Future<Long> cf) {
        consumer.shutdown();
        tes.shutdown();

        try {
            cf.get();
        } catch (Exception e) {
            logger.info("Unexpected exception", e);
        }
        logger.info("Goodbye");
    }

    private static Future<Long> submit(final StringConsumer consumer) {
        return Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "consumer-thread");
            t.setDaemon(true);
            return t;
        }).submit(consumer);
    }
}
