package com.agent.appointmentscheduler.repository;

import com.agent.appointmentscheduler.model.Event;
import com.agent.appointmentscheduler.model.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    
    List<Event> findBySessionId(String sessionId);
    
    List<Event> findByGoogleUserId(String googleUserId);
    
    List<Event> findBySessionIdAndStartTimeBetween(String sessionId, LocalDateTime start, LocalDateTime end);
    
    List<Event> findByGoogleUserIdAndStartTimeBetween(String googleUserId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT e FROM Event e WHERE e.sessionId = :sessionId AND e.status != 'CANCELLED' AND " +
           "((e.startTime <= :endTime AND e.endTime >= :startTime))")
    List<Event> findConflictingEvents(@Param("sessionId") String sessionId, 
                                      @Param("startTime") LocalDateTime startTime, 
                                      @Param("endTime") LocalDateTime endTime);
    
    @Query("SELECT e FROM Event e WHERE e.googleUserId = :googleUserId AND e.status != 'CANCELLED' AND " +
           "((e.startTime <= :endTime AND e.endTime >= :startTime))")
    List<Event> findConflictingEventsByGoogleUser(@Param("googleUserId") String googleUserId, 
                                                   @Param("startTime") LocalDateTime startTime, 
                                                   @Param("endTime") LocalDateTime endTime);
    
    List<Event> findBySessionIdAndStartTimeAfterOrderByStartTimeAsc(String sessionId, LocalDateTime startTime);
    
    List<Event> findByGoogleUserIdAndStartTimeAfterOrderByStartTimeAsc(String googleUserId, LocalDateTime startTime);
    
    List<Event> findBySessionIdAndStatusNot(String sessionId, EventStatus status);
    
    List<Event> findByGoogleUserIdAndStatusNot(String googleUserId, EventStatus status);
}

