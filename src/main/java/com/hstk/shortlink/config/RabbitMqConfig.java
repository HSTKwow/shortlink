package com.hstk.shortlink.config;


import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.MessageConverter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;


@Slf4j
@Configuration
public class RabbitMqConfig {
    public static final String VISIT_LOG_EXCHANGE="shortlink.visit.exchange";
    public static final String VISIT_LOG_QUEUE="shortlink.visit.log.queue";
    public static final String VISIT_LOG_ROUTING_KEY="shortlink.visit.log";

    public static final String VISIT_LOG_DLX="shortlink.visit.dlx";
    public static final String VISIT_LOG_DLQ="shortlink.visit.log.dlq";
    public static final String VISIT_LOG_DEAD_ROUTING_KEY="shortlink.visit.dead";


    @PostConstruct
    public void init() {
        log.info("rabbitmq stating");
    }

    //消息交换机
    @Bean
    public DirectExchange visitLogExchange(){
        return new DirectExchange(VISIT_LOG_EXCHANGE,true,false);
    }
    //消息队列
    @Bean
    public Queue visitLogQueue(){
        return QueueBuilder.durable(VISIT_LOG_QUEUE)
                .deadLetterExchange(VISIT_LOG_DLX)
                .deadLetterRoutingKey(VISIT_LOG_DEAD_ROUTING_KEY)
                .build();

    }
    //消息绑定
    @Bean
    public Binding visitLogBinding(){
        return BindingBuilder.bind(visitLogQueue())
                .to(visitLogExchange())
                .with(VISIT_LOG_ROUTING_KEY);
    }
    //消息转换
    @Bean
    public MessageConverter messageConverter(){
        return new JacksonJsonMessageConverter("com.hstk.shortlink.model.message");
    }
    //死信交换机
    @Bean
    public DirectExchange visitLogDeadExchange(){
        return new DirectExchange(VISIT_LOG_DLX,true,false);
    }
    //死信队列
    @Bean
    public Queue visitLogDeadQueue(){
        return QueueBuilder.durable(VISIT_LOG_DLQ).build();
    }
    //死信绑定
    @Bean
    public Binding visitLogDeadBinding(){
        return BindingBuilder.bind(visitLogDeadQueue())
                .to(visitLogDeadExchange())
                .with(VISIT_LOG_DEAD_ROUTING_KEY);
    }

    //监听容器工厂
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory, MessageConverter messageConverter){
        SimpleRabbitListenerContainerFactory factory=new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setDefaultRequeueRejected(false);

        factory.setAdviceChain(
                RetryInterceptorBuilder.stateless().maxRetries(3).backOffOptions(1000,2.0,5000).recoverer(new RejectAndDontRequeueRecoverer()).build()
        );
        return factory;
    }
}
