package com.hstk.shortlink.config;


import org.springframework.amqp.support.converter.MessageConverter;;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;

@Slf4j
@Configuration
public class RabbitMqConfig {
    public static final String VISIT_LOG_EXCHANGE="shortlink.visit.exchange";
    public static final String VISIT_LOG_QUEUE="shortlink.visit.log.queue";
    public static final String VISIT_LOG_ROUTING_KEY="shortlink.visit.log";


    @PostConstruct
    public void init() {
        log.info("rabbitmq stating");
    }

    @Bean
    public DirectExchange visitLogExchange(){
        return new DirectExchange(VISIT_LOG_EXCHANGE,true,false);
    }

    @Bean
    public Queue visitLogQueue(){
        return new Queue(VISIT_LOG_QUEUE,true);
    }

    @Bean
    public Binding visitLogBinding(){
        return BindingBuilder.bind(visitLogQueue())
                .to(visitLogExchange())
                .with(VISIT_LOG_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter(){
        return new JacksonJsonMessageConverter("com.hstk.shortlink.model.message");

    }



}
