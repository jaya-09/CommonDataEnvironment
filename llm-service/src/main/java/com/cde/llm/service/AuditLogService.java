package com.cde.llm.service;

import com.cde.llm.entity.UserProfile;
import com.cde.llm.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final ObjectMapper objectMapper;

    @Transactional
    public void log(String entityType, UUID entityId, String action,
                    Object oldValue, Object newValue,
                    UUID performedBy, String eventSource, String correlationId) {
        try {
            // Audit log writing — in production persist to training_audit_log table
            // Simplified: just log for now; wiring to repo left for next iteration
            log.info("AUDIT [{}/{}] action={} by={} correlationId={}",
                    entityType, entityId, action, performedBy, correlationId);
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }
}
