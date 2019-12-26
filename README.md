## Have you seen one or both following messages and wondering what is going on and how to resolve it?

15:26:33.588 [kafka-coordinator-heartbeat-thread | task1] WARN  o.a.k.c.c.i.AbstractCoordinator - [Consumer clientId=consumer-1, groupId=task1] This member will leave the group because consumer poll timeout has expired. This means the time between subsequent calls to poll() was longer than the configured max.poll.interval.ms, which typically implies that the poll loop is spending too much time processing messages. You can address this either by increasing max.poll.interval.ms or by reducing the maximum size of batches returned in poll() with max.poll.records.

15:49:01.398 [consumer-thread] WARN  o.a.k.c.c.i.ConsumerCoordinator - [Consumer clientId=consumer-1, groupId=task1] Synchronous auto-commit of offsets {test-1=OffsetAndMetadata{offset=5000, leaderEpoch=0, metadata=''}, test-0=OffsetAndMetadata{offset=2700, leaderEpoch=0, metadata=''}, test-3=OffsetAndMetadata{offset=2700, leaderEpoch=0, metadata=''}, test-2=OffsetAndMetadata{offset=2800, leaderEpoch=0, metadata=''}} failed: Commit cannot be completed since the group has already rebalanced and assigned the partitions to another member. This means that the time between subsequent calls to poll() was longer than the configured max.poll.interval.ms, which typically implies that the poll loop is spending too much time message processing. You can address this either by increasing max.poll.interval.ms or by reducing the maximum size of batches returned in poll() with max.poll.records.

## When you use https://kafka.apache.org/24/javadoc/index.html?org/apache/kafka/clients/consumer/KafkaConsumer.html API to subscribe to a Kafka topic, two things happen on the consumer side:

1. There is a thread created which keep sending (every heartbeat.interval.ms) heartbeat to coordinator. If the heartbeat is not received by the coordinator, then Kafka thinks that the consumer process or host died, and it removes this process from the consumer group and starts rebalancing of partitions to available consumers within the consumer group. heartbeat.interval.ms value is small to ensure that if process or host dies, Kafka can detect it fast and assigns the partitions to other members of the same consumer group.

2. If the consumer does not call poll method every max.poll.interval.ms, then Kafka thinks that the thread processing the messages died and it removes this process from the consumer group and starts rebalancing of partitions to available consumers within the consumer group. Ideally, max.poll.interval.ms value should be large enough to ensure that max.poll.records number of messages, received in one call of poll, can be processed within this time and such that consumer will call poll method again within max.poll.interval.ms time. If this value is very large then relabancing will take longer time.

If you have different kinds of messages in a topic and the processing time of messages vary depending on its type, then it is tricky to configure max.poll.interval.ms. If you set max.poll.interval.ms to worst case time interval, then rebalancing will take longer time, which is not ideal.

### Here is one way to solve the issue:
    while true:
        records = consumer.poll()
        submit records to process in a different thread pool
        consumer.pause
        if thread pool is ready for more messages, consumer.resume

If consumer has paused itself, then poll method does not fetch any message, and it tells Kafka that the consumer is alive. This way there is no need to set max.poll.interval.ms to a very large value (and pay for greater rebalancing time), since consumer.poll() will be called within the expected time interval. Look at https://github.com/soumitrak/kafka-task/blob/master/app/src/main/java/sk/task/kafka/StringConsumer.java for the implementation.

## Details of relevant configs from https://kafka.apache.org/documentation/

heartbeat.interval.ms: The expected time between heartbeats to the consumer coordinator when using Kafka's group management facilities. Heartbeats are used to ensure that the consumer's session stays active and to facilitate rebalancing when new consumers join or leave the group. The value must be set lower than session.timeout.ms, but typically should be set no higher than 1/3 of that value. It can be adjusted even lower to control the expected time for normal rebalances.

session.timeout.ms: The timeout used to detect client failures when using Kafka's group management facility. The client sends periodic heartbeats to indicate its liveness to the broker. If no heartbeats are received by the broker before the expiration of this session timeout, then the broker will remove this client from the group and initiate a rebalance. Note that the value must be in the allowable range as configured in the broker configuration by group.min.session.timeout.ms and group.max.session.timeout.ms.

max.poll.interval.ms: The maximum delay between invocations of poll() when using consumer group management. This places an upper bound on the amount of time that the consumer can be idle before fetching more records. If poll() is not called before expiration of this timeout, then the consumer is considered failed and the group will rebalance in order to reassign the partitions to another member. For consumers using a non-null group.instance.id which reach this timeout, partitions will not be immediately reassigned. Instead, the consumer will stop sending heartbeats and partitions will be reassigned after expiration of session.timeout.ms. This mirrors the behavior of a static consumer which has shutdown.

max.poll.records: The maximum number of records returned in a single call to poll().
