package com.cloud.disk.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration — exchanges, queues, dead-letter setup
 */
@Configuration
public class RabbitMQConfig {

    // ── Registration notification ──
    public static final String REGISTER_EXCHANGE = "register.exchange";
    public static final String REGISTER_QUEUE = "register.queue";
    public static final String REGISTER_ROUTING_KEY = "register.notify";
    public static final String REGISTER_DLQ = "register.dlq";
    public static final String REGISTER_DLX = "register.dlx";

    /**
     * Dead letter exchange for failed messages
     */
    @Bean
    public DirectExchange registerDlx() {
        return new DirectExchange(REGISTER_DLX);
    }

    /**
     * Dead letter queue
     */
    @Bean
    public Queue registerDlq() {
        return QueueBuilder.durable(REGISTER_DLQ).build();
    }

    /**
     * Bind DLQ to DLX
     */
    @Bean
    public Binding registerDlqBinding() {
        return BindingBuilder.bind(registerDlq()).to(registerDlx()).with(REGISTER_ROUTING_KEY);
    }

    /**
     * Main register exchange
     */
    @Bean
    public DirectExchange registerExchange() {
        return new DirectExchange(REGISTER_EXCHANGE);
    }

    /**
     * Main register queue with dead-letter config.
     * Failed messages retry up to 3 times, then go to DLQ.
     */
    @Bean
    public Queue registerQueue() {
        return QueueBuilder.durable(REGISTER_QUEUE)
                .deadLetterExchange(REGISTER_DLX)
                .deadLetterRoutingKey(REGISTER_ROUTING_KEY)
                .build();
    }

    /**
     * Bind register queue to register exchange
     */
    @Bean
    public Binding registerBinding() {
        return BindingBuilder.bind(registerQueue()).to(registerExchange()).with(REGISTER_ROUTING_KEY);
    }
}
