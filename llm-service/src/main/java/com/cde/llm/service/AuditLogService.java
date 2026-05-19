package com.cde.llm.service;

import com.cde.llm.audit.AuditAction;
import com.cde.llm.audit.AuditEntityType;
import com.cde.llm.entity.TrainingAuditLog;
import com.cde.llm.repository.TrainingAuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogService {

    private final TrainingAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(AuditEntityType entityType, UUID entityId, AuditAction action,
                    Object oldValue, Object newValue,
                    UUID performedBy, String eventSource, String correlationId) {
        try {
            auditLogRepository.save(TrainingAuditLog.builder()
                    .entityType(entityType.name())
                    .entityId(entityId)
                    .action(action.name())
                    .oldValue(toJson(oldValue))
                    .newValue(toJson(newValue))
                    .performedBy(performedBy)
                    .eventSource(eventSource)
                    .correlationId(correlationId)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write audit log: {}", e.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return value.toString();
        }
    }
}
