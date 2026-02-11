package com.orderms.order.config;

import com.orderms.order.kafka.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private Map<String, Object> baseProps() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "order-service-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }

    @Bean
    public ConsumerFactory<String, PaymentCompletedEvent> paymentCompletedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(PaymentCompletedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> paymentCompletedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentCompletedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentCompletedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, PaymentFailedEvent> paymentFailedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(PaymentFailedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentFailedEvent> paymentFailedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, PaymentFailedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentFailedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, InventoryReservedEvent> inventoryReservedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(InventoryReservedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> inventoryReservedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, InventoryReservedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryReservedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, InventoryFailedEvent> inventoryFailedConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(InventoryFailedEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InventoryFailedEvent> inventoryFailedKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, InventoryFailedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(inventoryFailedConsumerFactory());
        return factory;
    }

    @Bean
    public ConsumerFactory<String, NotificationSentEvent> notificationSentConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(
                baseProps(),
                new StringDeserializer(),
                new JsonDeserializer<>(NotificationSentEvent.class, false));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationSentEvent> notificationSentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationSentEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationSentConsumerFactory());
        return factory;
    }
}
