package com.icthh.xm.gate.listener;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.messaging.event.system.SystemEvent;
import com.icthh.xm.commons.tenant.JsonMapperUtils;
import com.icthh.xm.commons.tenant.TenantContextHolder;
import com.icthh.xm.commons.tenant.TenantContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemTopicConsumer {

    private final List<SystemTopicAbstractHandler> handlers;
    private final TenantContextHolder tenantContextHolder;

    public void consumeEvent(ConsumerRecord<String, String> message) {
        MdcUtils.putRid();
        try {
            log.info("Consume event from topic [{}]", message.topic());
            ObjectMapper mapper = JsonMapperUtils.getJsonMapperWithIgnore();
            SystemEvent event = mapper.readValue(message.value(), SystemEvent.class);
            handlers.forEach(handler -> executeHandler(handler, event));
        } catch (JacksonException e) {
            log.error("System topic message has incorrect format: '{}'", message.value(), e);
        } finally {
            MdcUtils.removeRid();
        }
    }

    private void executeHandler(SystemTopicAbstractHandler handler, SystemEvent event) {
        if (!handler.supports(event.getEventType())) {
            return;
        }
        String tenantKey = event.getTenantKey();
        Runnable task = () -> handler.execute(event);
        if (StringUtils.isBlank(tenantKey)) {
            task.run();
        } else {
            tenantContextHolder.getPrivilegedContext().execute(TenantContextUtils.buildTenant(tenantKey), task);
        }
    }
}
