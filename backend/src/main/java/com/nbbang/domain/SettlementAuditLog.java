package com.nbbang.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.Getter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Getter @Entity @Table(name="settlement_audit_log")
@NoArgsConstructor(access=AccessLevel.PROTECTED)
public class SettlementAuditLog {
    @Id @UuidGenerator private UUID id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="room_id") private SettlementRoom room;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="actor_user_id") private User actor;
    @Column(name="event_type",nullable=false) private String eventType;
    @Column(name="entity_type",nullable=false) private String entityType;
    @Column(name="entity_id") private UUID entityId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="before_data",columnDefinition="jsonb") private Map<String,Object> beforeData;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name="after_data",columnDefinition="jsonb") private Map<String,Object> afterData;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    public SettlementAuditLog(SettlementRoom room, User actor, String eventType, String entityType, UUID entityId, Map<String,Object> before, Map<String,Object> after){this.room=room;this.actor=actor;this.eventType=eventType;this.entityType=entityType;this.entityId=entityId;this.beforeData=before;this.afterData=after;this.createdAt=Instant.now();}
}
