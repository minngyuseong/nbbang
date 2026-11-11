package com.nbbang.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SettlementRoomTests {
    private SettlementRoom room() {
        User host = new User("test", "host-1", null, "방장", null);
        PayoutAccount account = new PayoutAccount(host, "004", "국민은행", "encrypted", "******1234", "방장", null, true);
        return new SettlementRoom("invite-token", "테스트 정산", host, account, 10_000);
    }

    @Test
    void openRoomCanBeCancelledOnlyOnce() {
        SettlementRoom room = room();

        room.cancel();

        assertThat(room.getStatus()).isEqualTo(SettlementRoom.Status.CANCELLED);
        assertThat(room.getSettledAt()).isNotNull();
        assertThatThrownBy(room::cancel).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(room::complete).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void completedRoomCannotBeCancelled() {
        SettlementRoom room = room();

        room.complete();

        assertThat(room.getStatus()).isEqualTo(SettlementRoom.Status.COMPLETED);
        assertThatThrownBy(room::cancel).isInstanceOf(IllegalArgumentException.class);
    }
}
