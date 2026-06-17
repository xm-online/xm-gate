package com.icthh.xm.gate.listener;

import com.icthh.xm.commons.logging.util.MdcUtils;
import com.icthh.xm.commons.permission.inspector.PrivilegeInspector;
import com.icthh.xm.gate.config.properties.ApplicationProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("unused")
public class ApplicationStartup implements ApplicationListener<ApplicationReadyEvent> {

    private static final String SYSTEM_TOPIC_CONSUMER_GROUP_ID = "gate-system-topic-consumer";

    private final ApplicationProperties applicationProperties;
    private final SystemTopicConsumer systemTopicConsumer;
    private final KafkaProperties kafkaProperties;
    private final PrivilegeInspector privilegeInspector;

    private ConcurrentMessageListenerContainer<String, String> systemTopicContainer;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (applicationProperties.isKafkaEnabled()) {
            createSystemTopicConsumer();
            privilegeInspector.readPrivileges(MdcUtils.getRid());
        } else {
            log.warn("WARNING! Privileges inspection is disabled by "
                + "configuration parameter 'application.kafka-enabled'");
        }
    }

    private void createSystemTopicConsumer() {
        String topic = applicationProperties.getKafkaSystemTopic();
        log.info("Creating kafka consumer for topic {}", topic);

        ContainerProperties containerProps = new ContainerProperties(topic);
        containerProps.setObservationEnabled(true);

        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, SYSTEM_TOPIC_CONSUMER_GROUP_ID);
        props.put(ConsumerConfig.METADATA_MAX_AGE_CONFIG, applicationProperties.getKafkaMetadataMaxAge());
        ConsumerFactory<String, String> factory = new DefaultKafkaConsumerFactory<>(props);

        MessageListener<String, String> listener = systemTopicConsumer::consumeEvent;

        systemTopicContainer = new ConcurrentMessageListenerContainer<>(factory, containerProps);
        systemTopicContainer.setupMessageListener(listener);
        systemTopicContainer.start();
        log.info("Successfully created kafka consumer for topic {}", topic);
    }

    @PreDestroy
    public void stopSystemTopicConsumer() {
        if (systemTopicContainer != null) {
            systemTopicContainer.stop();
        }
    }
}
