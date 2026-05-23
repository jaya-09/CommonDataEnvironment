package com.cde.qlm.event;

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
 * Publishes all QLM domain events to the single QLM domain topic (cde.qlm.events).
 *
 * This includes the phase gate check result (cde.plm.phase.gate.check.result) —
 * even though PLM consumes it, QLM is the publisher, so it goes on QLM's topic.
 * PLM subscribes to cde.qlm.events and routes by eventType.
 *
 * Reliability: @Retry (3 attempts, exponential backoff) + @CircuitBreaker.
 * Failure propagates so @Transactional rolls back the DB write.
 */
@Service @Slf4j @RequiredArgsConstructor
public class QlmEventPublisher {

    private final ObjectMapper objectMapper;

    @Value("${app.gcp.project-id}") private String projectId;

    /** Single domain topic — all QLM events go here. */
    @Value("${app.topics.qlm-events}") private String qlmEventsTopic;

    private final AtomicReference<Publisher> publisherRef = new AtomicReference<>();

    // ── Public publish methods ────────────────────────────────────────────────

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishNcrRaised(NcrRaisedEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.qlm.ncr.raised", e, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishNcrClosed(NcrClosedEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.qlm.ncr.closed", e, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishCapaRaised(CapaRaisedEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.qlm.capa.raised", e, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishCapaClosed(CapaClosedEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.qlm.capa.closed", e, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishAuditFinding(AuditFindingEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.qlm.audit.finding", e, correlationId));
    }

    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishRiskRegistered(RiskRegisteredEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.qlm.risk.registered", e, correlationId));
    }

    /**
     * Phase gate check result published back toward PLM.
     * Still goes on cde.qlm.events because QLM is the publisher.
     * PLM's subscription to cde.qlm.events picks this up.
     */
    @CircuitBreaker(name = "pubsub-publish", fallbackMethod = "publishFallback")
    @Retry(name = "pubsub-publish", fallbackMethod = "publishFallback")
    public void publishPhaseGateCheckResult(PhaseGateCheckResultEvent e, String correlationId) {
        publish(EventEnvelope.of("cde.plm.phase.gate.check.result", e, correlationId));
    }

    // ── Core publish logic ────────────────────────────────────────────────────

    private void publish(EventEnvelope envelope) {
        try {
            String json = objectMapper.writeValueAsString(envelope);
            ByteString data = ByteString.copyFrom(json, StandardCharsets.UTF_8);
            PubsubMessage msg = PubsubMessage.newBuilder()
                    .setData(data)
                    .putAttributes("eventType", envelope.getType())
                    .build();
            getOrCreatePublisher().publish(msg);
            log.info("Published [{}] to {}", envelope.getType(), qlmEventsTopic);
        } catch (Exception ex) {
            log.warn("Publish attempt failed for [{}]: {}", envelope.getType(), ex.getMessage());
            throw new RuntimeException("Pub/Sub publish failed: " + ex.getMessage(), ex);
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
                        p = Publisher.newBuilder(ProjectTopicName.of(projectId, qlmEventsTopic)).build();
                        publisherRef.set(p);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create publisher for: " + qlmEventsTopic, e);
                    }
                }
            }
        }
        return p;
    }
}
