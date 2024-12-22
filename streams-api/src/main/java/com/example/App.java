package com.example;

import java.util.Collections;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;

import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;

import io.github.cdimascio.dotenv.Dotenv;

public class App {

    public final static String STORE_NAME = "Counts";

    static StoreBuilder<KeyValueStore<String, String>> storeBuilder = Stores.keyValueStoreBuilder(
            Stores.persistentKeyValueStore(STORE_NAME),
            Serdes.String(),
            Serdes.String());

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.load();

        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, dotenv.get("APPLICATION_ID"));
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, dotenv.get("BOOTSTRAP_SERVERS"));

        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        createTopics(dotenv.get("BOOTSTRAP_SERVERS"), dotenv.get("SOURCE_TOPIC"), 1, (short) 1);
        createTopics(dotenv.get("BOOTSTRAP_SERVERS"), dotenv.get("SINK_TOPIC"), 1, (short) 1);

        final Topology topology = new Topology();

        topology.addSource("Source", dotenv.get("SOURCE_TOPIC"));

        topology.addProcessor("Processor", new DuplicateProcessorSupplier(), "Source");

        topology.addSink("Sink", dotenv.get("SINK_TOPIC"), "Processor");

        final KafkaStreams streams = new KafkaStreams(topology, props);

        final CountDownLatch latch = new CountDownLatch(1);

        System.out.println("Starting Kafka Streams");

        // attach shutdown handler to catch control-c
        Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
            @Override
            public void run() {
                streams.close();
                latch.countDown();
            }
        });

        try {
            streams.start();
            latch.await();
        } catch (Throwable e) {
            System.exit(1);
        }
        System.exit(0);
    }

    public static void createTopics(String bootstrapServers, String topicName, int partitions,
            short replicationFactor) {
        // AdminClient configuration
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Create AdminClient
        try (AdminClient adminClient = AdminClient.create(props)) {
            // Define the new topic
            NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);

            // Create the topic
            adminClient.createTopics(Collections.singleton(newTopic)).all().get();

            System.out.println("Topic created: " + topicName);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                System.out.println("Topic already exists: " + topicName);
            } else {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
