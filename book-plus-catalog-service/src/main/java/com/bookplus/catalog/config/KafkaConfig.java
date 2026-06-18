package com.bookplus.catalog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // ── Producer Factory ──────────────────────────────────────────────────

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,  bootstrapServers);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "none");

        // ObjectMapper con soporte Java Time (Instant, LocalDate) — sin él, la
        // serialización de eventos con fechas falla síncronamente y rompe el request.
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(mapper);
        valueSerializer.setAddTypeInfo(false);

        return new DefaultKafkaProducerFactory<>(props, new StringSerializer(), valueSerializer);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ── Consumer (para la proyección de compras del usuario) ───────────────

    @Bean
    public org.springframework.kafka.core.ConsumerFactory<String, java.util.Map<String, Object>> consumerFactory() {
        java.util.Map<String, Object> props = new HashMap<>();
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG, "catalog-service");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                org.apache.kafka.common.serialization.StringDeserializer.class);
        props.put(org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                org.springframework.kafka.support.serializer.JsonDeserializer.class);
        props.put(org.springframework.kafka.support.serializer.JsonDeserializer.VALUE_DEFAULT_TYPE, "java.util.Map");
        props.put(org.springframework.kafka.support.serializer.JsonDeserializer.TRUSTED_PACKAGES, "*");
        return new org.springframework.kafka.core.DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public org.springframework.kafka.listener.DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        var recoverer = new org.springframework.kafka.listener.DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, ex) -> new org.apache.kafka.common.TopicPartition(record.topic() + ".DLT", -1));
        return new org.springframework.kafka.listener.DefaultErrorHandler(recoverer,
                new org.springframework.util.backoff.FixedBackOff(1000L, 3L));
    }

    @Bean
    public org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, java.util.Map<String, Object>>
    kafkaListenerContainerFactory(org.springframework.kafka.listener.DefaultErrorHandler kafkaErrorHandler) {
        var factory = new org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory<String, java.util.Map<String, Object>>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(2);
        factory.setCommonErrorHandler(kafkaErrorHandler);
        return factory;
    }

    // ── Topics ────────────────────────────────────────────────────────────

    @Bean public NewTopic topicBookCreated() {
        return TopicBuilder.name("catalog.book.created").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic topicBookUpdated() {
        return TopicBuilder.name("catalog.book.updated").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic topicBookDeleted() {
        return TopicBuilder.name("catalog.book.deleted").partitions(3).replicas(1).build();
    }

    @Bean public NewTopic topicReviewAdded() {
        return TopicBuilder.name("catalog.review.added").partitions(3).replicas(1).build();
    }
}
