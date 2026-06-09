package com.cloud.disk.mq.consumer;

import com.cloud.disk.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Listens for user registration events.
 * Failed messages are retried via RabbitMQ dead-letter mechanism.
 */
@Component
public class UserRegisterConsumer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisterConsumer.class);

    @RabbitListener(queues = RabbitMQConfig.REGISTER_QUEUE)
    public void onRegister(Message message) {
        String body = new String(message.getBody());
        log.info("New user registered: {}", body);

        // Simulate: if message contains "test-fail", throw to test DLQ
        if (body.contains("\"test-fail\"")) {
            throw new RuntimeException("Simulated consumer failure for DLQ test");
        }
    }
}
