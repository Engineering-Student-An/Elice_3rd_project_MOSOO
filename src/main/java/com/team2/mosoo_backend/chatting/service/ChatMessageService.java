package com.team2.mosoo_backend.chatting.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.mosoo_backend.chatting.dto.*;
import com.team2.mosoo_backend.chatting.entity.ChatMessage;
import com.team2.mosoo_backend.chatting.entity.ChatMessageType;
import com.team2.mosoo_backend.chatting.entity.ChatRoom;
import com.team2.mosoo_backend.chatting.entity.ChatRoomConnection;
import com.team2.mosoo_backend.chatting.mapper.ChatMessageMapper;
import com.team2.mosoo_backend.chatting.repository.ChatMessageQueryRepository;
import com.team2.mosoo_backend.chatting.repository.ChatMessageRepository;
import com.team2.mosoo_backend.chatting.repository.ChatRoomConnectionRepository;
import com.team2.mosoo_backend.chatting.repository.ChatRoomRepository;
import com.team2.mosoo_backend.exception.CustomException;
import com.team2.mosoo_backend.exception.ErrorCode;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatRoomUtils chatRoomUtils;
    private final ChatMessageQueryRepository chatMessageQueryRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatRoomConnectionRepository chatRoomConnectionRepository;

    // SimpMessageBroker를 직접 주입받기 위함
    private final ApplicationContext applicationContext;

    // 레디스에 저장된 채팅방 연결 수 반환하는 메서드
    public Integer getConnectionCount(Long chatRoomId) {

        // ChatRoomConnection 엔티티를 가져옴
        ChatRoomConnection connection = chatRoomConnectionRepository.findById(String.valueOf(chatRoomId))
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND)); // 채팅방이 없을 경우 예외 처리

        // 연결 수 반환
        return connection.getConnectionCount();
    }

    // 레디스에 채팅 저장 메서드
    @Transactional
    public void saveChatMessageToRedis(StompHeaderAccessor accessor, Long chatRoomId, ChatMessageRequestDto chatMessageRequestDto) {

        // UserDetails가 WebSocket 메세지에서 역직렬화될 수 없음 => 직접 로그인 유저 id 가져오는 방법
        // 세션에서 사용자 ID 가져오기
        Long loginUserId = (Long) accessor.getSessionAttributes().get("loginUserId");

        // 로그인 유저 정보 가져옴
        userRepository.findById(loginUserId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        String redisKey = "chatRoom:" + chatRoomId + ":messages";

        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId).orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        chatRoom.setLastChatDate(chatMessageRequestDto.getCreatedAt());

        // 전송된 파일이 있다면 1. base64File 삭제 2. S3에 파일 업로드 3. content를 업로드 된 url로 설정
        if(chatMessageRequestDto.getBase64File() != null) {
            chatMessageRequestDto = chatRoomUtils.convertToMultipartFile(chatMessageRequestDto);
        }

        // 2명 모두 접속중인 경우: 읽음, 1명만 접속중인 경우: 읽지 않음
        chatMessageRequestDto.setChecked(getConnectionCount(chatRoomId) == 2);

        // Redis에 메세지 저장
        redisTemplate.opsForList().leftPush(redisKey, chatMessageRequestDto);
    }

    // db에 채팅 저장 메서드
    @Transactional
    public void saveChatMessagesToDb(Long chatRoomId) {

        // 채팅방 정보 조회
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        List<ChatMessageRequestDto> chatMessageRequestDtos = findChatMessagesFromRedis(chatRoomId, false);

        // 저장할 리스트
        List<ChatMessage> chatMessagesToSave = new ArrayList<>();

        // 생성시간 기준 내림차순으로 저장되었으므로 역순으로 순회함 => 생성시간 기준 오름차순으로 저장
        for (int i = chatMessageRequestDtos.size() - 1; i >= 0; i--) {
            ChatMessageRequestDto chatMessageRequestDto = chatMessageRequestDtos.get(i);

            ChatMessage createdChatMessage = chatMessageMapper.toEntity(chatMessageRequestDto);

            createdChatMessage.setChatRoom(chatRoom);

            chatMessagesToSave.add(createdChatMessage);
        }

        // Batch 처리한 저장
        chatMessageRepository.saveAll(chatMessagesToSave);
    }

    // 레디스에서 채팅 메세지 조회하는 메서드
    public List<ChatMessageRequestDto> findChatMessagesFromRedis(Long chatRoomId, boolean isOne) {
        String redisKey = "chatRoom:" + chatRoomId + ":messages";

        // Redis에서 메세지를 가져옴
        List<Object> messages;
        if(isOne) {     // 찾는 메세지 수가 1개라면
            messages = redisTemplate.opsForList().range(redisKey, 0, 0);
        } else {        // 찾는 메세지 수가 1개가 아니라면
            messages = redisTemplate.opsForList().range(redisKey, 0, -1);
        }

        // LinkedHashMap을 ChatMessageRequestDto로 변환
        return messages.stream()
                .map(msg -> objectMapper.convertValue(msg, ChatMessageRequestDto.class)) // 변환
                .collect(Collectors.toList());
    }

    // 채팅 내역 조회 메서드 ( (db) no-offset + Redis)
    // 첫 조회 : 레디스에서 조회
    // 이후 조회 or 레디스의 체팅 메세지가 적은 경우 : db에서 추가적으로 조회
    @Transactional
    public ChatMessageResponseWrapperDto findChatMessages(Long loginUserId, Long chatRoomId, Long index, boolean isInit) {

        // 로그인 유저 정보 가져옴
        Users loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 채팅방 접근 권한 검증
        ChatRoom foundChatRoom = chatRoomUtils.validateChatRoomOwnership(chatRoomId, loginUser);

        // 채팅 상대 이름 저장
        String opponentFullName = chatRoomUtils.getOpponentFullName(foundChatRoom, loginUser);

        // 조회한 채팅 메세지를 응답 dto로 변환
        List<ChatMessageResponseDto> result = new ArrayList<>();

        if(isInit) {    // 처음 채팅 메세지 목록 조회 => Redis에서 조회함
            List<ChatMessageRequestDto> chatMessagesFromRedis = findChatMessagesFromRedis(chatRoomId,false);
            for (ChatMessageRequestDto dto : chatMessagesFromRedis) {
                ChatMessageResponseDto responseDto = chatMessageMapper.requestDtoToResponseDto(dto);
                result.add(responseDto);
            }
        }
        if (!isInit || result.size() <= 10) {       // 첫 조회가 아니거나 Redis에서 조회한 목록의 크기가 10개 이하일 때

            // 생성일 기준 내림차순, 페이지 당 20개씩 조회
            Pageable pageable = PageRequest.of(0, 20 - result.size(), Sort.by(Sort.Direction.DESC, "created_at")); // Pageable 객체 생성

            // 채팅 메세지를 생성시간 기준 오름차순으로 조회
            Page<ChatMessage> chatMessageList = chatMessageQueryRepository.findChatMessagesByChatRoomIdUsingNoOffset(pageable, chatRoomId, index);

            for (ChatMessage chatMessage : chatMessageList) {

                ChatMessageResponseDto dto = chatMessageMapper.toChatMessageResponseDto(chatMessage);

                // 파일인 경우에만 fileName 필드 set
                if (chatMessage.getType() == ChatMessageType.FILE) {
                    String[] split = chatMessage.getContent().split("-");
                    dto.setFileName(URLDecoder.decode(split[split.length - 1], StandardCharsets.UTF_8));     // s3 업로드 url 형태 : "url-파일이름"
                }
                result.add(dto);
            }
        }
        return new ChatMessageResponseWrapperDto(opponentFullName, result, result.size());
    }

    // Redis or DB의 읽지 않음 메세지 -> 읽음으로 변환
    @Transactional
    public void setChatMessagesToRead(Long chatRoomId, Long loginUserId) {
        // 레디스에 읽지 않음 메세지가 있는 경우
        String redisKey = "chatRoom:" + chatRoomId + ":messages";

        // Redis에서 메시지 목록 가져오기
        List<Object> messages = redisTemplate.opsForList().range(redisKey, 0, -1);

        // 업데이트할 메시지를 저장할 리스트 생성
        List<Object> updatedMessages = new ArrayList<>();

        for (Object obj : messages) {

            // Object를 Map으로 캐스팅
            Map<String, Object> message = (Map<String, Object>) obj;

            Long sourceUserId = Long.parseLong(message.get("sourceUserId").toString());
            boolean checked = (Boolean) message.get("checked");

            // sourceUserId가 loginUserId와 다르고 checked가 false인 경우 checked 값을 true로 변경
            if (!sourceUserId.equals(loginUserId) && !checked) {
                message.put("checked", true); // checked 값을 true로 변경
            }
            // 업데이트된 메시지를 새로운 리스트에 추가
            updatedMessages.add(message);
        }

        // Redis 리스트를 비우고 업데이트된 메시지를 저장
        redisTemplate.delete(redisKey); // 기존 데이터를 삭제
        for (Object message : updatedMessages) {
            redisTemplate.opsForList().rightPush(redisKey, message); // 업데이트된 메시지 저장
        }

        // db에 읽지 않음 메세지가 있는 경우
        if(chatMessageRepository.existsByChatRoomIdAndSourceUserIdNotAndCheckedIs(chatRoomId, loginUserId, false)) {
            List<ChatMessage> unReadChatMessages = chatMessageRepository.findChatMessagesByChatRoomIdAndSourceUserIdNotAndCheckedIs(chatRoomId, loginUserId, false);

            // 모두 읽음 상태로 저장
            for (ChatMessage unReadChatMessage : unReadChatMessages) {
                unReadChatMessage.setChecked(true);
                chatMessageRepository.save(unReadChatMessage);
            }
        }

        // 메세지 읽었음을 알리는 메세지 전송
        SimpMessagingTemplate template = applicationContext.getBean(SimpMessagingTemplate.class);
        template.convertAndSend("/sub/" + chatRoomId, "message read!");
    }


}
