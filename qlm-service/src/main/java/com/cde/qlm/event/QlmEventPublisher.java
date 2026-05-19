package com.cde.qlm.event;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@Service @Slf4j @RequiredArgsConstructor
public class QlmEventPublisher {
    private final ObjectMapper objectMapper;
    @Value("${app.gcp.project-id}") private String projectId;
    @Value("${app.topics.ncr-raised}") private String ncrTopic;
    @Value("${app.topics.capa-closed}") private String capaTopic;
    @Value("${app.topics.audit-finding}") private String auditTopic;
    private final Map<String, Publisher> cache = new ConcurrentHashMap<>();

    public void publishNcrRaised(NcrRaisedEvent e, String correlationId) {
        publish(ncrTopic, EventEnvelope.of("cde.qlm.ncr.raised", e, correlationId));
    }
    public void publishCapaClosed(CapaClosedEvent e, String correlationId) {
        publish(capaTopic, EventEnvelope.of("cde.qlm.capa.closed", e, correlationId));
    }
    public void publishAuditFinding(AuditFindingEvent e, String correlationId) {
        publish(auditTopic, EventEnvelope.of("cde.qlm.audit.finding", e, correlationId));
    }
    private void publish(String topicName, EventEnvelope env) {
        try {
            String json = objectMapper.writeValueAsString(env);
            PubsubMessage msg = PubsubMessage.newBuilder()
                    .setData(ByteString.copyFrom(json, StandardCharsets.UTF_8))
                    .putAttributes("eventType", env.getType()).build();
            getOrCreate(topicName).publish(msg);
            log.info("Published [{}]", env.getType());
        } catch (Exception ex) { log.error("Publish failed: {}", ex.getMessage(), ex); }
    }
    private Publisher getOrCreate(String topic) {
        return cache.computeIfAbsent(topic, t -> {
            try { return Publisher.newBuilder(ProjectTopicName.of(projectId, t)).build(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
