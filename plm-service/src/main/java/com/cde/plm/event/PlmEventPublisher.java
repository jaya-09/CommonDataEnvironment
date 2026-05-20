package com.cde.plm.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service @Slf4j @RequiredArgsConstructor
public class PlmEventPublisher {

    private final ObjectMapper objectMapper;

    @Value("${app.gcp.project-id}") private String projectId;
    @Value("${app.topics.version-released}") private String versionReleasedTopic;
    @Value("${app.topics.phase-transitioned}") private String phaseTransitionedTopic;
    @Value("${app.topics.change-request-approved}") private String crApprovedTopic;
    @Value("${app.topics.phase-gate-check-requested}") private String phaseGateCheckRequestedTopic;
    @Value("${app.topics.version-status-changed}") private String versionStatusChangedTopic;

    private final Map<String, Publisher> cache = new ConcurrentHashMap<>();

    public void publishVersionReleased(VersionReleasedEvent e, String correlationId) {
        publish(versionReleasedTopic, EventEnvelope.of("cde.plm.version.released", e, correlationId));
    }

    public void publishPhaseTransitioned(PhaseTransitionedEvent e, String correlationId) {
        publish(phaseTransitionedTopic, EventEnvelope.of("cde.plm.phase.transitioned", e, correlationId));
    }

    public void publishChangeRequestApproved(ChangeRequestApprovedEvent e, String correlationId) {
        publish(crApprovedTopic, EventEnvelope.of("cde.plm.change.request.approved", e, correlationId));
    }

    /**
     * Publishes a phase gate check request to both QLM and LLM via Pub/Sub.
     * Replaces the old synchronous REST calls in checkPhaseGate().
     */
    public void publishPhaseGateCheckRequested(PhaseGateCheckRequestedEvent e, String correlationId) {
        publish(phaseGateCheckRequestedTopic,
                EventEnvelope.of("cde.plm.phase.gate.check.requested", e, correlationId));
    }

    public void publishVersionStatusChanged(VersionStatusChangedEvent e, String correlationId) {
        publish(versionStatusChangedTopic,
                EventEnvelope.of("cde.plm.version.status.changed", e, correlationId));
    }

    private void publish(String topicName, EventEnvelope envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            ByteString data = ByteString.copyFrom(json, StandardCharsets.UTF_8);
            PubsubMessage msg = PubsubMessage.newBuilder().setData(data)
                    .putAttributes("eventType", envelope.getType()).build();
            getOrCreate(topicName).publish(msg);
            log.info("Published [{}] to {}", envelope.getType(), topicName);
        } catch (Exception ex) {
            log.error("Failed to publish event to {}: {}", topicName, ex.getMessage(), ex);
        }
    }

    private Publisher getOrCreate(String topic) {
        return cache.computeIfAbsent(topic, t -> {
            try { return Publisher.newBuilder(ProjectTopicName.of(projectId, t)).build(); }
            catch (Exception e) { throw new RuntimeException(e); }
        });
    }
}
