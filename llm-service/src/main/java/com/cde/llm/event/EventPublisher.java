package com.cde.llm.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Publishes all LLM domain events to the single LLM domain topic (cde.llm.events).
 *
 * This includes phase gate check results back to PLM — LLM publishes them, so
 * they belong on cde.llm.events. PLM subscribes to cde.llm.events and routes
 * by eventType.
 *
 * Reliability: @Retry (3 attempts, exponential backoff) + @CircuitBreaker.
 * Failure propagates so @Transactional rolls back the DB write.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EventPublisher {

    private final ObjectMapper objectMapper;

    @Value("${app.gcp.project-id}")
    private String projectId;

    /** Single domain topic — all LLM events go here. */
    @Value("${app.topics.llm-events}")
    private String llmEventsTopic;

    private final AtomicReference<Publisher> publisherRef = new AtomicReference<>();

    // ── Public publish methods ────────────────────────────────────────────────

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishCertificationGranted(CertificationGrantedEvent event, String correlationId) {
        publish(EventEnvelope.of("cde.llm.certification.granted", event, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishUserProfileUpdated(UserProfileUpdatedEvent event, String correlationId) {
        publish(EventEnvelope.of("cde.llm.user.profile.updated", event, correlationId));
    }

    /**
     * Phase gate certification check result published back toward PLM.
     * Still goes on cde.llm.events because LLM is the publisher.
     * PLM's subscription to cde.llm.events picks this up.
     */
    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishPhaseGateCheckResult(PhaseGateCheckResultEvent event, String correlationId) {
        publish(EventEnvelope.of("cde.plm.phase.gate.check.result", event, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishEnrollmentTriggered(EnrollmentTriggeredEvent event, String correlationId) {
        publish(EventEnvelope.of("cde.llm.enrollment.triggered", event, correlationId));
    }

    // ── Core publish logic ────────────────────────────────────────────────────

    private void publish(EventEnvelope envelope) {
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

            getOrCreatePublisher().publish(message);

            log.info("Published event [type={}, eventId={}, correlationId={}] to topic={}",
                    envelope.getType(), envelope.getEventId(), envelope.getCorrelationId(), llmEventsTopic);

        } catch (Exception e) {
            log.error("Failed to publish event [type={}, topic={}]: {}",
                    envelope.getType(), llmEventsTopic, e.getMessage(), e);
            throw new RuntimeException("Event publishing failed: " + e.getMessage(), e);
        }
    }

    public void publishFallback(Object event, String correlationId, Exception ex) {
        log.error("CRITICAL: All Pub/Sub publish retries exhausted for correlationId={} event={}: {}",
                correlationId, event.getClass().getSimpleName(), ex.getMessage());
        throw new RuntimeException("Pub/Sub publish permanently failed. Transaction rolled back.", ex);
    }

    private Publisher getOrCreatePublisher() {
        Publisher p = publisherRef.get();
        if (p == null) {
            synchronized (this) {
                p = publisherRef.get();
                if (p == null) {
                    try {
                        p = Publisher.newBuilder(ProjectTopicName.of(projectId, llmEventsTopic)).build();
                        publisherRef.set(p);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create publisher for: " + llmEventsTopic, e);
                    }
                }
            }
        }
        return p;
    }
}
