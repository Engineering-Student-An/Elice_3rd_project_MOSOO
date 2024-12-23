package com.team2.mosoo_backend.chatting.entity;


import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter @Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
// 채팅 수정 기능이 없으므로 BaseEntity 상속 x
// 기능고도화로 채팅 수정 기능을 넣는다면 상속으로 변경할 예정
public class ChatMessage {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_message_id")
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ChatMessageType type;

    @Column(nullable = false)
    @Setter
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private Long sourceUserId;  // 보내는 유저의 id

    @Column(nullable = false)
    @Setter
    private boolean checked;     // 상대방의 읽음 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_room_id")
    private ChatRoom chatRoom;

    // 연관관계 편의 메서드
    public void setChatRoom(ChatRoom chatRoom) {
        this.chatRoom = chatRoom;
        chatRoom.getChatMessageList().add(this);
    }
}
