package com.hstk.shortlink.mq;

import com.hstk.shortlink.config.RabbitMqConfig;
import com.hstk.shortlink.model.message.VisitLogMessage;
import com.hstk.shortlink.service.VisitLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class VisitLogConsumer {
    private final VisitLogService visitLogService;

    public VisitLogConsumer(VisitLogService visitLogService) {
        this.visitLogService = visitLogService;
    }

    @RabbitListener(queues = RabbitMqConfig.VISIT_LOG_QUEUE)
    public void consumeVisitLog(VisitLogMessage message){
        log.info("consume visit log");

        visitLogService.recordVisit(
                message.getShortCode(),
                message.getIp(),
                    message.getUserAgent(),
                message.getReferer()
        );
    }

}
