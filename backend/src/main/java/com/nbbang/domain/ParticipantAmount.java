package com.nbbang.domain;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "participant_amount")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantAmount {
    @Id @Column(name = "participant_id") private UUID participantId;
    @OneToOne(fetch = FetchType.LAZY, optional = false) @MapsId @JoinColumn(name = "participant_id")
    private RoomParticipant participant;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "room_id")
    private SettlementRoom room;
    @Column(nullable = false) private long amount;
    @Version private long version;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public ParticipantAmount(SettlementRoom room, RoomParticipant participant, long amount) {
        this.participantId = participant.getId();
        this.room = room;
        this.participant = participant;
        this.amount = amount;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void update(long amount) {
        this.amount = amount;
        this.updatedAt = Instant.now();
    }
}
