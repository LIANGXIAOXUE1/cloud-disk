package com.cloud.disk.mq.producer;

import com.cloud.disk.config.RabbitMQConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Sends a notification message after a new user registers
 */
@Component
public class UserRegisterProducer {

    private static final Logger log = LoggerFactory.getLogger(UserRegisterProducer.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendRegisterNotification(String username, Long userId) {
        String message = String.format("{\"event\":\"user.register\",\"username\":\"%s\",\"userId\":%d}", username, userId);
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.REGISTER_EXCHANGE,
                RabbitMQConfig.REGISTER_ROUTING_KEY,
                message
        );
        log.info("RabbitMQ message sent: user.register for {}", username);
    }
}
