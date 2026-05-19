package com.cde.plm.service;

import com.cde.plm.entity.PhaseGatePendingCheck;
import com.cde.plm.repository.PhaseGatePendingCheckRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PhaseGateTimeoutJob {

    private final PhaseGatePendingCheckRepository pendingCheckRepo;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void timeoutStalePendingChecks() {
        OffsetDateTime cutoff = OffsetDateTime.now().minusMinutes(5);
        List<PhaseGatePendingCheck> stale = pendingCheckRepo
                .findByStatusAndCreatedAtBefore(PhaseGatePendingCheck.CheckStatus.PENDING, cutoff);
        if (stale.isEmpty()) return;

        stale.forEach(check -> {
            check.setStatus(PhaseGatePendingCheck.CheckStatus.TIMEOUT);
            check.setCompletedAt(OffsetDateTime.now());
            log.warn("Phase gate check timed out [gateCorrelationId={}, versionId={}, qlmReceived={}, llmReceived={}]",
                    check.getGateCorrelationId(), check.getVersionId(),
                    check.isQlmResultReceived(), check.isLlmResultReceived());
        });
        pendingCheckRepo.saveAll(stale);
        log.info("Timed out {} stale phase gate pending check(s)", stale.size());
    }
}
