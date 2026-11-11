package com.nbbang.config;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import com.nbbang.repository.RoomParticipantRepository;
import com.nbbang.security.NbbangPrincipal;

@Component
public class RoomSubscriptionInterceptor implements ChannelInterceptor {
    private static final Pattern ROOM_TOPIC = Pattern.compile("^/topic/rooms/([0-9a-fA-F-]{36})$");
    private final RoomParticipantRepository participants;
    public RoomSubscriptionInterceptor(RoomParticipantRepository participants) { this.participants = participants; }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (accessor.getCommand() != StompCommand.SUBSCRIBE) return message;
        Matcher matcher = ROOM_TOPIC.matcher(accessor.getDestination() == null ? "" : accessor.getDestination());
        if (!matcher.matches()) return message;
        if (!(accessor.getUser() instanceof Authentication authentication)
                || !(authentication.getPrincipal() instanceof NbbangPrincipal principal)
                || !participants.existsByRoomIdAndUserId(UUID.fromString(matcher.group(1)), principal.userId())) {
            throw new IllegalArgumentException("정산방 참가자만 실시간 상태를 구독할 수 있습니다.");
        }
        return message;
    }
}

