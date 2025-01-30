package com.team2.mosoo_backend.chatting.service;

import com.team2.mosoo_backend.bid.dto.BidResponseDto;
import com.team2.mosoo_backend.bid.entity.Bid;
import com.team2.mosoo_backend.bid.mapper.BidMapper;
import com.team2.mosoo_backend.bid.repository.BidRepository;
import com.team2.mosoo_backend.chatting.dto.*;
import com.team2.mosoo_backend.chatting.entity.ChatMessage;
import com.team2.mosoo_backend.chatting.entity.ChatMessageType;
import com.team2.mosoo_backend.chatting.entity.ChatRoom;
import com.team2.mosoo_backend.chatting.entity.ChatRoomConnection;
import com.team2.mosoo_backend.chatting.mapper.ChatMessageMapper;
import com.team2.mosoo_backend.chatting.mapper.ChatRoomMapper;
import com.team2.mosoo_backend.chatting.repository.ChatMessageRepository;
import com.team2.mosoo_backend.chatting.repository.ChatRoomConnectionRepository;
import com.team2.mosoo_backend.chatting.repository.ChatRoomRepository;
import com.team2.mosoo_backend.exception.CustomException;
import com.team2.mosoo_backend.exception.ErrorCode;
import com.team2.mosoo_backend.post.dto.PostResponseDto;
import com.team2.mosoo_backend.post.entity.Post;
import com.team2.mosoo_backend.post.mapper.PostMapper;
import com.team2.mosoo_backend.post.repository.PostRepository;
import com.team2.mosoo_backend.user.entity.Authority;
import com.team2.mosoo_backend.user.entity.Gosu;
import com.team2.mosoo_backend.user.entity.UserInfo;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.repository.GosuRepository;
import com.team2.mosoo_backend.user.repository.UserInfoRepository;
import com.team2.mosoo_backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final BidRepository bidRepository;
    private final ChatRoomMapper chatRoomMapper;
    private final PostMapper postMapper;
    private final BidMapper bidMapper;
    private final ChatRoomUtils chatRoomUtils;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatMessageService chatMessageService;
    private final ChatRoomConnectionRepository chatRoomConnectionRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final UserInfoRepository userInfoRepository;
    private final GosuRepository gosuRepository;

    private final ApplicationContext applicationContext;

    // 채팅방 연결 시 메서드
    public void connectToChatRoom(Long loginUserId, Long chatRoomId, String userSessionId) {

        // 레디스에 유저의 채팅방 접속 정보 저장 => 사용자 ID를 키로 하고, 채팅방 ID를 값으로 저장
        redisTemplate.opsForValue().set(userSessionId, chatRoomId);

        String chatRoomIdString = String.valueOf(chatRoomId);

        // ChatRoomConnection 엔티티를 가져오거나 새로 생성
        ChatRoomConnection connection = chatRoomConnectionRepository.findById(chatRoomIdString)
                .orElse(ChatRoomConnection.builder()
                        .id(chatRoomIdString)
                        .connectionCount(0) // 초기 연결 수 0
                        .build());

        // 연결 수 증가
        connection.incrementConnectionCount();

        // 레디스에 연결 정보 저장
        chatRoomConnectionRepository.save(connection);

        // 읽지 않은 메세지가 있다면 읽음으로 변경
        chatMessageService.setChatMessagesToRead(chatRoomId, loginUserId);
    }

    // 채팅방 연결 해제 시 메서드
    public void disconnectFromChatRoom(Long chatRoomId, String userSessionId) {

        // 레디스에 있는 유저의 채팅방 접속 정보 삭제 => 사용자 ID로 저장된 채팅방 ID 삭제
        redisTemplate.delete(userSessionId);

        // ChatRoomConnection 엔티티 가져옴
        ChatRoomConnection connection = chatRoomConnectionRepository.findById(String.valueOf(chatRoomId))
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 연결 수 감소
        connection.decrementConnectionCount();

        // 레디스에 연결 정보 저장
        chatRoomConnectionRepository.save(connection);

        // 채팅방에 접속중인 유저가 0명이라면 레디스의 채팅 메세지를 db로 저장
        if(connection.getConnectionCount() == 0) {
            // 레디스 -> db 저장
            chatMessageService.saveChatMessagesToDb(chatRoomId);

            // Redis 키 삭제
            String redisKey = "chatRoom:" + chatRoomId + ":messages";
            redisTemplate.delete(redisKey); // redisTemplate을 사용하여 Redis에서 키 삭제

            // 채팅방 연결 정보 키 삭제
            redisTemplate.delete("chatRoomConnection:" + chatRoomId);
        }
    }

    // 레디스에 저장된 사용자 세션 ID로 채팅방 ID를 가져오는 메서드
    public Long getUserChatRoom(String userSessionId) {
        Object value = redisTemplate.opsForValue().get(userSessionId);
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Integer) {
            return ((Integer) value).longValue(); // Integer를 Long으로 변환
        }
        return null; // null 처리
    }

    // 채팅방 목록 조회 메서드
    public ChatRoomResponseWrapperDto findAllChatRooms(Long loginUserId, int page) {

        // 페이지 당 채팅방 5개
        PageRequest pageRequest = PageRequest.of(page - 1, 5);

        // 로그인 유저 정보 가져옴
        Users loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Page<ChatRoom> chatRooms;

        // 로그인 한 유저의 id가 채팅방의 고수 id 이거나 일반유저 id 인 채팅방 (즉, 참여하는 채팅방) 조회
        chatRooms = chatRoomRepository.findActiveChatRoomsByUserId(pageRequest, loginUserId);

        List<ChatRoomResponseDto> result = new ArrayList<>();
        for (ChatRoom chatRoom : chatRooms) {
            ChatRoomResponseDto dto = chatRoomMapper.toChatRoomResponseDto(chatRoom);

            // 채팅 상대 이름 저장
            dto.setOpponentFullName(chatRoomUtils.getOpponentFullName(chatRoom, loginUser));

            ChatMessage chatMessage;

            List<ChatMessageRequestDto> chatMessagesFromRedis = chatMessageService.findChatMessagesFromRedis(chatRoom.getId(), true);
            if(!chatMessagesFromRedis.isEmpty()) {  // 최신 채팅 메세지가 레디스에 존재할 때
                 chatMessage = chatMessageMapper.toEntity(chatMessagesFromRedis.get(0));
            } else {                                // 최신 채팅 메세지가 db에 존재할 때
                chatMessage = chatMessageRepository.findTopByChatRoomIdOrderByCreatedAtDesc(chatRoom.getId())
                        .orElse(null);
            }

            if(chatMessage.getType() == ChatMessageType.IMAGE) { // 메세지 타입이 이미지 인 경우: 가장 최근 채팅을 "이미지"로 설정
                dto.setLastChatMessage("이미지");
            } else if(chatMessage.getType() == ChatMessageType.VIDEO) { // 메세지 타입이 동영상 인 경우: 가장 최근 채팅을 "동영상"으로 설정
                dto.setLastChatMessage("동영상");
            } else if(chatMessage.getType() == ChatMessageType.FILE) { // 메세지 타입이 파일 인 경우: 가장 최근 채팅을 "파일"로 설정
                dto.setLastChatMessage("파일");
            } else {    // 메세지 타입이 메세지 인 경우: 가장 최근 채팅을 메세지 내용으로 설정
                dto.setLastChatMessage(chatMessage.getContent());
            }

            // 가장 마지막 채팅 시간 설정
            dto.setLastChatDate(chatRoom.getLastChatDate());

            // 안 읽은 메세지 존재 여부 설정
            dto.setExistUnchecked(!chatMessage.getSourceUserId().equals(loginUserId) && !chatMessage.isChecked());

            // 관련 게시글 정보 설정
            dto.setPostId(chatRoom.getPost().getId());
            dto.setPostTitle(chatRoom.getPost().getTitle());

            result.add(dto);

        }

        // 총 페이지 수
        int totalPages = (chatRooms.getTotalPages()==0 ? 1 : chatRooms.getTotalPages());

        return new ChatRoomResponseWrapperDto(result, totalPages);
    }

    // 특정 채팅방 관련 세부 정보 조회 메서드
    public ChatRoomInfoResponseDto findChatRoomInfo(Long loginUserId, Long chatRoomId) {

        // 로그인 유저 정보 가져옴
        Users loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 채팅방 접근 권한 검증 후 찾은 채팅방 받아옴
        ChatRoom foundChatRoom = chatRoomUtils.validateChatRoomOwnership(chatRoomId, loginUser);

        // 고수 여부 저장
        boolean isGosu = (loginUserId.equals(foundChatRoom.getGosuId()));

        // 게시글 정보
        PostResponseDto postResponseDto = postMapper.postToPostResponseDto(foundChatRoom.getPost());

        // 입찰 정보가 있다면 입찰 정보
        BidResponseDto bidResponseDto = null;
        if(foundChatRoom.getBid() != null) {
            bidResponseDto = bidMapper.bidToBidResponseDto(foundChatRoom.getBid());
        }

        return new ChatRoomInfoResponseDto(postResponseDto, bidResponseDto,
                isGosu, foundChatRoom.getPrice());
    }

    // 특정 채팅방 관련 상대 정보 조회 메서드
    public ChatRoomOpponentInfoResponseDto findChatRoomOpponentInfo(Long loginUserId, Long chatRoomId) {

        // 로그인 유저 정보 가져옴
        Users loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 채팅방 접근 권한 검증 후 찾은 채팅방 받아옴
        ChatRoom chatRoom = chatRoomUtils.validateChatRoomOwnership(chatRoomId, loginUser);

        ChatRoomOpponentInfoResponseDto chatRoomOpponentInfoResponseDto;
        // 로그인 유저가 고수 유저라면 => 상대 일반 유저 정보 담기
        if(loginUserId.equals(chatRoom.getGosuId())) {

            Users opponentUser = userRepository.findById(chatRoom.getUserId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            chatRoomOpponentInfoResponseDto = new ChatRoomOpponentInfoResponseDto(opponentUser.getFullName(), opponentUser.getEmail());
        } else {    // 로그인 유저가 일반 유저라면 => 상대 고수 유저 정보 담기

            Users opponentUser = userRepository.findById(chatRoom.getGosuId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            UserInfo userInfo = userInfoRepository.findByUsersId(opponentUser.getId())
                    .orElseThrow(() -> new CustomException(ErrorCode.USER_INFO_NOT_FOUND));
            Gosu gosu = gosuRepository.findByUserInfoId(userInfo.getId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

            chatRoomOpponentInfoResponseDto = new ChatRoomOpponentInfoResponseDto(gosu.getBusinessName(), gosu.getBusinessNumber(), gosu.getGosuInfoAddress(), gosu.getGosuInfoPhone(), gosu.getCategory().getName());
        }


        return chatRoomOpponentInfoResponseDto;
    }

    // 채팅방 생성 메서드
    @Transactional
    public ChatRoomCreateResponseDto createChatRoom(Long loginUserId, ChatRoomRequestDto chatRoomRequestDto) {

        // 로그인 유저 정보 가져옴
        Users loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 고수 정보 검증, 고수 유저가 존재하지 않으면 404 에러 반환
        Users gosu = userRepository.findById(chatRoomRequestDto.getGosuId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 게시글 정보 가져옴, 존재하지 않으면 404 에러 반환
        Post post = postRepository.findById(chatRoomRequestDto.getPostId())
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Bid bid = null;
        if(post.isOffer()) {    // 고수가 작성한 게시글인 경우 (게시글로부터 채팅방이 파생되어야 함)

            // 해당 게시글에 대한 채팅방이 이미 존재하는 경우
            if(chatRoomRepository.existsByPostIdAndUserIdAndGosuId(post.getId(), loginUserId, chatRoomRequestDto.getGosuId())) {
                ChatRoom existChatRoom = chatRoomRepository.findByPostIdAndUserIdAndGosuId(post.getId(), loginUserId, chatRoomRequestDto.getGosuId()).get();
                existChatRoom.reCreate();
                return new ChatRoomCreateResponseDto(existChatRoom.getId());
            }
        } else { // 유저가 작성한 게시글인 경우 (입찰로부터 채팅방이 파생되어야 함)

            // 입찰 정보 가져옴, 존재하지 않으면 404 에러 반환
            bid = bidRepository.findById(chatRoomRequestDto.getBidId())
                    .orElseThrow(() -> new CustomException(ErrorCode.BID_NOT_FOUND));

            // 해당 입찰에 대한 채팅방이 이미 존재하는 경우
            if(chatRoomRepository.existsByBidIdAndUserIdAndGosuId(bid.getId(), loginUserId, chatRoomRequestDto.getGosuId())) {
                ChatRoom existChatRoom = chatRoomRepository.findByBidIdAndUserIdAndGosuId(bid.getId(), loginUserId, chatRoomRequestDto.getGosuId()).get();
                existChatRoom.reCreate();
                return new ChatRoomCreateResponseDto(existChatRoom.getId());
            }
        }

        // 유저정보를 포함한 채팅방 생성
        ChatRoom chatRoom = chatRoomMapper.toEntity(chatRoomRequestDto, loginUserId);

        // 채팅방 <-> 게시글 + 입찰 (필요한 경우만) 연관관계 설정
        chatRoom.setPost(post);
        if(bid != null) {   // 입찰 정보 존재하는 경우
            chatRoom.setBid(bid);
        }

        // 채팅방에 초기 가격 설정
        chatRoom.setPrice( (bid != null) ? bid.getPrice() : post.getPrice());

        ChatRoom savedChatRoom = chatRoomRepository.save(chatRoom);

        // 채팅방 입장 메세지 생성
        ChatMessage userEnterChatMessage = chatMessageMapper.toEntity(ChatMessageRequestDto.builder()
                .sourceUserId(loginUserId).type(ChatMessageType.ENTER).content(loginUser.getFullName() + " 님이 입장했습니다.").build());
        ChatMessage gosuEnterChatMessage = chatMessageMapper.toEntity(ChatMessageRequestDto.builder()
                .sourceUserId(chatRoom.getGosuId()).type(ChatMessageType.ENTER).content(gosu.getFullName() + " 님이 입장했습니다.").build());

        userEnterChatMessage.setChatRoom(savedChatRoom);
        userEnterChatMessage.setCreatedAt(LocalDateTime.now());
        gosuEnterChatMessage.setChatRoom(savedChatRoom);
        gosuEnterChatMessage.setCreatedAt(LocalDateTime.now());

        // 채팅방 입장 메세지 저장
        chatMessageRepository.save(userEnterChatMessage);
        chatMessageRepository.save(gosuEnterChatMessage);

        return new ChatRoomCreateResponseDto(savedChatRoom.getId());
    }

    // 채팅방 나가기 메서드 - 일반유저, 고수유저 공통
    @Transactional
    public ChatRoomDeleteResponseDto quitChatRoom(Long loginUserId, Long chatRoomId) {

        // 로그인 유저 정보 가져옴
        Users loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 채팅방 유효성 검증
        ChatRoom chatRoom = chatRoomUtils.validateChatRoomOwnership(chatRoomId, loginUser);

        // USER의 고수 여부 포함하여 채팅방 나가기
        chatRoom.quitChatRoom((loginUserId.equals(chatRoom.getGosuId())));

        // 채팅방 퇴장 메세지 생성
        ChatMessageRequestDto chatMessageRequestDto = ChatMessageRequestDto.builder()
                .sourceUserId(loginUserId)
                .type(ChatMessageType.QUIT)
                .content(loginUser.getFullName() + " 님이 채팅방을 나갔습니다.")
                .createdAt(LocalDateTime.now())
                .build();

        String redisKey = "chatRoom:" + chatRoomId + ":messages";
        chatMessageRequestDto.setChecked(chatMessageService.getConnectionCount(chatRoomId) == 2);
        redisTemplate.opsForList().leftPush(redisKey, chatMessageRequestDto);

        // 채팅방 나갔음을 알리는 메세지 전송
        SimpMessagingTemplate template = applicationContext.getBean(SimpMessagingTemplate.class);
        template.convertAndSend("/sub/" + chatRoomId, "quit chatroom!");

        return new ChatRoomDeleteResponseDto(chatRoomId);
    }

    // 채팅방에서 가격 변경
    @Transactional
    public ChatRoomPriceResponseDto updatePrice(Long loginUserId, Long chatRoomId, int price) {

        // 로그인 유저 정보 가져옴
        Users loginUser = userRepository.findById(loginUserId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 채팅방 정보가 없다면 404 에러 반환
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        // 고수가 아니거나 채팅방 참여 인원이 아니면 403 에러 반환
        if(loginUser.getAuthority() != Authority.ROLE_GOSU || !chatRoom.getGosuId().equals(loginUserId)) {
            throw new CustomException(ErrorCode.USER_NOT_AUTHORIZED);
        }

        chatRoom.setPrice(price);

        return new ChatRoomPriceResponseDto(price);
    }
}
