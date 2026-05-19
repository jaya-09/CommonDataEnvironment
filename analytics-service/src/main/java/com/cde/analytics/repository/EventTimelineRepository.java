package com.cde.analytics.repository;

import com.cde.analytics.entity.EventTimeline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventTimelineRepository extends JpaRepository<EventTimeline, UUID> {
    List<EventTimeline> findTop50ByOrderByOccurredAtDesc();
    List<EventTimeline> findByProductCodeOrderByOccurredAtDesc(String productCode);
    List<EventTimeline> findByVersionIdOrderByOccurredAtDesc(UUID versionId);
    List<EventTimeline> findBySourceServiceOrderByOccurredAtDesc(String service);
    boolean existsByEventId(String eventId);
}
