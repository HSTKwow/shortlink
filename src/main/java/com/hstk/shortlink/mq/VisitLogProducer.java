package com.hstk.shortlink.mq;


import com.hstk.shortlink.config.RabbitMqConfig;
import com.hstk.shortlink.model.message.VisitLogMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VisitLogProducer {
    private final RabbitTemplate rabbitTemplate;

    public VisitLogProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendVisitLog(String shortCode,String ip,String userAgent,String referer){
        VisitLogMessage message=new VisitLogMessage();
        message.setShortCode(shortCode);
        message.setIp(ip);
        message.setUserAgent(userAgent);
        message.setReferer(referer);

        try{
            rabbitTemplate.convertAndSend(
                    RabbitMqConfig.VISIT_LOG_EXCHANGE,
                    RabbitMqConfig.VISIT_LOG_ROUTING_KEY,
                    message
            );
        } catch (Exception e) {
            log.error("send visit log message fail",e);
        }

    }

}
