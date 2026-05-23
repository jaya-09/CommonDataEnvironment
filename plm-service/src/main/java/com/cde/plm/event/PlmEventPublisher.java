package com.cde.plm.event;

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
 * Publishes all PLM domain events to the single PLM domain topic (cde.plm.events).
 *
 * Domain topic model: each service owns one topic; consumers subscribe to the
 * domains they need and route internally via the eventType field.
 *
 * Reliability contract:
 *  - @Retry retries up to 3 times with exponential backoff (500ms → 1000ms).
 *  - @CircuitBreaker opens after 50% failure rate over 10 calls, stays open 30s.
 *  - On failure the exception propagates so @Transactional rolls back the DB write —
 *    the database and the event bus never go out of sync.
 */
@Service @Slf4j @RequiredArgsConstructor
public class PlmEventPublisher {

    private final ObjectMapper objectMapper;

    @Value("${app.gcp.project-id}") private String projectId;

    /** Single domain topic — all PLM events go here. */
    @Value("${app.topics.plm-events}") private String plmEventsTopic;

    private final AtomicReference<Publisher> publisherRef = new AtomicReference<>();

    // ── Public publish methods ────────────────────────────────────────────────

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishVersionReleased(VersionReleasedEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.plm.version.released", e, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishPhaseTransitioned(PhaseTransitionedEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.plm.phase.transitioned", e, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishChangeRequestApproved(ChangeRequestApprovedEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.plm.change.request.approved", e, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishPhaseGateCheckRequested(PhaseGateCheckRequestedEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.plm.phase.gate.check.requested", e, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishVersionStatusChanged(VersionStatusChangedEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.plm.version.status.changed", e, correlationId));
    }

    // ── Core publish logic ────────────────────────────────────────────────────

    private void publish(EventEnvelope envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            ByteString data = ByteString.copyFrom(json, StandardCharsets.UTF_8);
            PubsubMessage msg = PubsubMessage.newBuilder()
                    .setData(data)
                    // eventType attribute enables subscription-level filtering in GCP if ever needed
                    .putAttributes("eventType", envelope.getType())
                    .build();
            getOrCreatePublisher().publish(msg);
            log.info("Published [{}] to {}", envelope.getType(), plmEventsTopic);
        } catch (Exception ex) {
            log.warn("Publish attempt failed for [{}]: {}", envelope.getType(), ex.getMessage());
            throw new RuntimeException("Pub/Sub publish failed: " + ex.getMessage(), ex);
        }
    }

    /**
     * Fallback after all retries are exhausted.
     * Rethrows so @Transactional rolls back — DB never changes without a corresponding event.
     */
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
                        p = Publisher.newBuilder(ProjectTopicName.of(projectId, plmEventsTopic)).build();
                        publisherRef.set(p);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create publisher for: " + plmEventsTopic, e);
                    }
                }
            }
        }
        return p;
    }
}
