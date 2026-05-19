package com.cde.llm.event;

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

@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisher {

    private final ObjectMapper objectMapper;

    @Value("${app.gcp.project-id}")
    private String projectId;

    @Value("${app.topics.certification-granted}")
    private String certGrantedTopic;

    @Value("${app.topics.user-profile-updated}")
    private String userProfileUpdatedTopic;

    @Value("${app.topics.phase-gate-check-result}")
    private String phaseGateResultTopic;

    @Value("${app.topics.enrollment-triggered}")
    private String enrollmentTriggeredTopic;

    private final Map<String, Publisher> publisherCache = new ConcurrentHashMap<>();

    public void publishCertificationGranted(CertificationGrantedEvent event, String correlationId) {
        publish(certGrantedTopic, EventEnvelope.of("cde.llm.certification.granted", event, correlationId));
    }

    public void publishUserProfileUpdated(UserProfileUpdatedEvent event, String correlationId) {
        publish(userProfileUpdatedTopic, EventEnvelope.of("cde.llm.user.profile.updated", event, correlationId));
    }

    /**
     * Publishes the certification readiness result back to PLM
     * after evaluating a phase gate check request.
     */
    public void publishPhaseGateCheckResult(PhaseGateCheckResultEvent event, String correlationId) {
        publish(phaseGateResultTopic, EventEnvelope.of("cde.plm.phase.gate.check.result", event, correlationId));
    }

    public void publishEnrollmentTriggered(EnrollmentTriggeredEvent event, String correlationId) {
        publish(enrollmentTriggeredTopic, EventEnvelope.of("cde.llm.enrollment.triggered", event, correlationId));
    }

    private void publish(String topicName, EventEnvelope envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            ByteString data = ByteString.copyFrom(json, StandardCharsets.UTF_8);

            PubsubMessage message = PubsubMessage.newBuilder()
                    .setData(data)
                    .putAttributes("eventType", envelope.getType())
                    .putAttributes("version", envelope.getVersion())
                    .putAttributes("correlationId", envelope.getCorrelationId() != null
                            ? envelope.getCorrelationId() : "")
                    .build();

            getOrCreatePublisher(topicName).publish(message);

            log.info("Published event [type={}, eventId={}, correlationId={}] to topic={}",
                    envelope.getType(), envelope.getEventId(), envelope.getCorrelationId(), topicName);

        } catch (Exception e) {
            log.error("Failed to publish event [type={}, topic={}]: {}",
                    envelope.getType(), topicName, e.getMessage(), e);
            throw new RuntimeException("Event publishing failed for topic: " + topicName, e);
        }
    }

    private Publisher getOrCreatePublisher(String topicName) {
        return publisherCache.computeIfAbsent(topicName, name -> {
            try {
                return Publisher.newBuilder(ProjectTopicName.of(projectId, name)).build();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create publisher for topic: " + name, e);
            }
        });
    }
}
