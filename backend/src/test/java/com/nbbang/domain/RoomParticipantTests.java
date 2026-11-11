package com.nbbang.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RoomParticipantTests {
    private final User user = new User("test", "user-1", null, "참가자", null);

    @Test
    void memberMustRequestPaymentConfirmationBeforeHostCanConfirm() {
        RoomParticipant participant = new RoomParticipant(null, user, RoomParticipant.Role.MEMBER);

        assertThatThrownBy(participant::confirm)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("확인 요청");

        participant.markSent();
        participant.confirm();
        assertThat(participant.getPaymentStatus()).isEqualTo(RoomParticipant.PaymentStatus.CONFIRMED);
    }

    @Test
    void changingAmountResetsMemberPaymentState() {
        RoomParticipant participant = new RoomParticipant(null, user, RoomParticipant.Role.MEMBER);
        participant.markSent();

        participant.resetPaymentForAmountChange();

        assertThat(participant.getPaymentStatus()).isEqualTo(RoomParticipant.PaymentStatus.PENDING);
        assertThat(participant.getPaidAt()).isNull();
    }

    @Test
    void hostNeverHasARequiredPayment() {
        RoomParticipant host = new RoomParticipant(null, user, RoomParticipant.Role.HOST);

        host.confirmAnyway();
        assertThat(host.getPaymentStatus()).isEqualTo(RoomParticipant.PaymentStatus.NOT_REQUIRED);
        assertThatThrownBy(host::markSent).isInstanceOf(IllegalArgumentException.class);
    }
}
