package com.hireconnect.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ configuration for notification-service.
 *
 * Exchange  : hireconnect.events  (topic exchange)
 * Queues    : notification.application.queue
 *             notification.interview.queue
 *             notification.job.queue
 * Routing   : application.status.*
 *             interview.*
 *             job.alert
 */
@Configuration
public class RabbitMQConfig {

    // ── Exchange ───────────────────────────────────────────────────────────
    public static final String EXCHANGE = "hireconnect.events";

    // ── Queues ─────────────────────────────────────────────────────────────
    public static final String APPLICATION_QUEUE  = "notification.application.queue";
    public static final String INTERVIEW_QUEUE    = "notification.interview.queue";
    public static final String JOB_ALERT_QUEUE    = "notification.job.queue";

    // ── Routing keys (producers use these) ────────────────────────────────
    public static final String APPLICATION_ROUTING_KEY = "application.status.changed";
    public static final String INTERVIEW_ROUTING_KEY   = "interview.scheduled";
    public static final String JOB_ALERT_ROUTING_KEY   = "job.alert";

    @Bean
    public TopicExchange hireconnectExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    @Bean
    public Queue applicationQueue() {
        return QueueBuilder.durable(APPLICATION_QUEUE).build();
    }

    @Bean
    public Queue interviewQueue() {
        return QueueBuilder.durable(INTERVIEW_QUEUE).build();
    }

    @Bean
    public Queue jobAlertQueue() {
        return QueueBuilder.durable(JOB_ALERT_QUEUE).build();
    }

    @Bean
    public Binding applicationBinding() {
        return BindingBuilder.bind(applicationQueue())
                .to(hireconnectExchange())
                .with("application.status.*");
    }

    @Bean
    public Binding interviewBinding() {
        return BindingBuilder.bind(interviewQueue())
                .to(hireconnectExchange())
                .with("interview.*");
    }

    @Bean
    public Binding jobAlertBinding() {
        return BindingBuilder.bind(jobAlertQueue())
                .to(hireconnectExchange())
                .with(JOB_ALERT_ROUTING_KEY);
    }

    // ── JSON message converter ─────────────────────────────────────────────
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf) {
        RabbitTemplate template = new RabbitTemplate(cf);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
