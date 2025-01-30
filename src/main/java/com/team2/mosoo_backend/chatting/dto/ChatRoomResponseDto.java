package com.team2.mosoo_backend.chatting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatRoomResponseDto {

    private Long chatRoomId;            // 채팅방 id
    private String opponentFullName;    // 채팅에 참여하고 있는 상대방의 이름
    private String lastChatMessage;     // 마지막 채팅 메세지 (메세지 타입이 아니면 타입을 넣음)
    private LocalDateTime lastChatDate; // 마지막 채팅 시간

    private boolean existUnchecked;     // 읽지 않은 메세지 존재 여부

    private Long postId;
    private String postTitle;
}
