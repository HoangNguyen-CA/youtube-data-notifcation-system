package com.example;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.ProcessorSupplier;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DuplicateProcessorSupplier implements ProcessorSupplier<String, String, String, String> {
    @Override

    public Processor<String, String, String, String> get() {
        return new Processor<String, String, String, String>() {
            private KeyValueStore<String, String> store;
            private ProcessorContext<String, String> context;
            private ObjectMapper objectMapper;

            @Override
            public void init(ProcessorContext<String, String> context) {
                this.context = context;
                this.store = context.getStateStore(App.STORE_NAME);
                this.objectMapper = new ObjectMapper();
                this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

            }

            @Override
            public void process(Record<String, String> record) {

                System.out.println("Processing record: " + record.key() + " " + record.value());
                if (record.value() == null || record.key() == null) {
                    return;
                }

                try {
                    VideoDetails oldVideoDetails;
                    if (store.get(record.key()) == null) {
                        oldVideoDetails = new VideoDetails();
                    } else {
                        oldVideoDetails = objectMapper.readValue(store.get(record.key()), VideoDetails.class);
                    }

                    VideoDetails recordVideoDetails = objectMapper.readValue(record.value(), VideoDetails.class);

                    VideoDetailChanges changes = new VideoDetailChanges();
                    changes.title = recordVideoDetails.title;
                    int numChanges = 0;
                    if (oldVideoDetails.likes != recordVideoDetails.likes) {
                        changes.likes = recordVideoDetails.likes;
                        changes.oldLikes = oldVideoDetails.likes;
                        numChanges++;
                    }

                    if (oldVideoDetails.comments != recordVideoDetails.comments) {
                        changes.comments = recordVideoDetails.comments;
                        changes.oldComments = oldVideoDetails.comments;
                        numChanges++;
                    }

                    if (oldVideoDetails.views != recordVideoDetails.views) {
                        changes.views = recordVideoDetails.views;
                        changes.oldViews = oldVideoDetails.views;
                        numChanges++;
                    }

                    if (!Objects.equals(recordVideoDetails.title, oldVideoDetails.title)) {
                        changes.oldTitle = oldVideoDetails.title;
                        numChanges++;
                    }

                    if (numChanges > 0) {
                        context.forward(record.withValue(objectMapper.writeValueAsString(changes)));
                    }

                    store.put(record.key(), record.value());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    @Override
    public Set<StoreBuilder<?>> stores() {
        return Collections.singleton(App.storeBuilder);
    }
}
