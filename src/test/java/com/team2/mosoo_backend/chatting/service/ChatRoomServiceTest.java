package com.team2.mosoo_backend.chatting.service;

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
import com.team2.mosoo_backend.post.entity.Post;
import com.team2.mosoo_backend.post.mapper.PostMapper;
import com.team2.mosoo_backend.post.repository.PostRepository;
import com.team2.mosoo_backend.user.entity.Authority;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.repository.GosuRepository;
import com.team2.mosoo_backend.user.repository.UserInfoRepository;
import com.team2.mosoo_backend.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("채팅방 서비스 단위 테스트")
class ChatRoomServiceTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private BidRepository bidRepository;

    @Mock
    private ChatRoomMapper chatRoomMapper;

    @Mock
    private PostMapper postMapper;

    @Mock
    private BidMapper bidMapper;

    @Mock
    private ChatRoomUtils chatRoomUtils;

    @Mock
    private ChatMessageMapper chatMessageMapper;

    @Mock
    private ChatMessageService chatMessageService;

    @Mock
    private ChatRoomConnectionRepository chatRoomConnectionRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private GosuRepository gosuRepository;

    @Mock
    private ValueOperations<String, Object> valueOperations; // ValueOperations 모킹

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private ChatRoomService chatRoomService;

    static final Long chatRoomId = 1L;
    static final String userSessionId = "session123";

    private ChatRoomConnection chatRoomConnection;
    private ChatMessageRequestDto chatMessageRequestDto;
    private ListOperations<String, Object> listOperations;
    private Users user;
    private Users gosu;
    private Post post;
    private Bid bid;
    private ChatRoom chatRoom;
    private ChatMessage chatMessage;


    @BeforeEach
    public void setUp() {
        chatRoomConnection = new ChatRoomConnection("1", 1);

        chatMessageRequestDto = ChatMessageRequestDto.builder()
                .sourceUserId(1L)
                .type(ChatMessageType.FILE)
                .base64File("base64test")
                .fileName("testFile")
                .createdAt(LocalDateTime.now()).build();

        // Mockito 사용 -> ListOperations 클래스의 mock 객체 생성
        listOperations = mock(ListOperations.class);

        user = Users.builder().id(1L).fullName("일반 유저").authority(Authority.ROLE_USER).build();
        gosu = Users.builder().id(2L).fullName("고수 유저").authority(Authority.ROLE_GOSU).build();

        post = Post.builder().id(1L).build();
        bid = Bid.builder().id(1L).build();

        chatRoom = ChatRoom.builder().id(1L).userId(1L).gosuId(2L).post(post).bid(bid).price(10000).userDeletedAt(null).build();
        chatMessage = ChatMessage.builder().id(1L).chatRoom(chatRoom).content("내용").type(ChatMessageType.MESSAGE).sourceUserId(1L).createdAt(LocalDateTime.now()).checked(false).build();
    }

    @Test
    @Order(1)
    @DisplayName("채팅방 연결 메서드 테스트")
    public void connectToChatRoomTest() {

        //given
        given(chatRoomConnectionRepository.findById(String.valueOf(chatRoomId))).willReturn(Optional.empty());
        // opsForValue() 메서드가 valueOperations를 반환하도록 모킹
        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        //when
        chatRoomService.connectToChatRoom(user.getId(), chatRoomId, userSessionId);

        //then
        verify(chatRoomConnectionRepository, times(1)).save(any());

        // 읽지 않은 메시지를 읽음으로 변경하는 메서드가 호출되었는지 검증
        verify(chatMessageService, times(1)).setChatMessagesToRead(chatRoomId, user.getId());
    }

    @Test
    @Order(2)
    @DisplayName("채팅방 연결 해제 메서드 테스트")
    public void disconnectFromChatRoomTest() {

        //given
        given(chatRoomConnectionRepository.findById(String.valueOf(chatRoomId))).willReturn(Optional.ofNullable(chatRoomConnection));

        //when
        // 접속 인원: 1->0명 이 되는 상황
        chatRoomService.disconnectFromChatRoom(chatRoomId, userSessionId);

        //then
        // Redis에서 사용자 접속 정보 삭제 검증
        verify(redisTemplate, times(1)).delete(userSessionId);

        // 연결 수 감소 및 저장 검증
        verify(chatRoomConnectionRepository, times(1)).save(chatRoomConnection);
        assertEquals(0, chatRoomConnection.getConnectionCount()); // 연결 수가 감소해야 함

        // db에 채팅 저장 메서드 호출했는지 검증
        verify(chatMessageService).saveChatMessagesToDb(chatRoomId);

        // Redis 키 삭제 검증
        verify(redisTemplate).delete("chatRoom:" + chatRoomId + ":messages");
        verify(redisTemplate).delete("chatRoomConnection:" + chatRoomId);
    }

    @Test
    @Order(3)
    @DisplayName("레디스에 저장된 사용자 세션 ID로 채팅방 ID 조회 메서드 테스트")
    public void getUserChatRoomTest() {
        //given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        Long expectedChatRoomId = 1L;
        given(redisTemplate.opsForValue().get(userSessionId)).willReturn(expectedChatRoomId);

        //when
        Long chatRoomId = chatRoomService.getUserChatRoom(userSessionId);

        //then
        assertEquals(expectedChatRoomId, chatRoomId);
    }

    @Test
    @Order(4)
    @DisplayName("채팅방 세부 정보 조회 메서드 테스트")
    public void findChatRoomInfoTest() {

        //given
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(chatRoomUtils.validateChatRoomOwnership(chatRoomId, user)).willReturn(chatRoom);

        //when
        ChatRoomInfoResponseDto result = chatRoomService.findChatRoomInfo(user.getId(), chatRoomId);

        //then
        assertThat(result.isGosu()).isEqualTo(false);
        assertThat(result.getPrice()).isEqualTo(10000);
    }

    @Test
    @Order(5)
    @DisplayName("채팅방 상대방 정보 조회 메서드 테스트")
    public void findChatRoomOpponentInfoTest() {

        //given
        given(userRepository.findById(gosu.getId())).willReturn(Optional.of(gosu));
        given(chatRoomUtils.validateChatRoomOwnership(chatRoomId, gosu)).willReturn(chatRoom);

        given(userRepository.findById(chatRoom.getUserId())).willReturn(Optional.ofNullable(user));

        //when
        ChatRoomOpponentInfoResponseDto result = chatRoomService.findChatRoomOpponentInfo(gosu.getId(), chatRoomId);

        //then
        assertThat(result.getFullName()).isEqualTo(user.getFullName());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
    }

    @Test
    @Order(6)
    @DisplayName("채팅방 나가기 메서드 테스트")
    public void quitChatRoomTest() {

        // given
        ChatRoom spyChatRoom = spy(chatRoom);
        given(userRepository.findById(user.getId())).willReturn(Optional.of(user));
        given(chatRoomUtils.validateChatRoomOwnership(any(), any())).willReturn(spyChatRoom);
        given(applicationContext.getBean(SimpMessagingTemplate.class)).willReturn(messagingTemplate);
        given(redisTemplate.opsForList()).willReturn(listOperations);
        when(listOperations.leftPush(anyString(), any())).thenReturn(0L);

        // when
        ChatRoomDeleteResponseDto result = chatRoomService.quitChatRoom(user.getId(), chatRoomId);

        // then
        verify(spyChatRoom, times(1)).quitChatRoom(false);
        verify(chatMessageRepository, times(1)).save(any());
        assertThat(result.getChatRoomId()).isEqualTo(chatRoomId);
    }

    @Test
    @Order(7)
    @DisplayName("채팅방 가격 변경 메서드 테스트")
    public void updatePriceTest() {

        //given
        int updatePrice = 9999;
        ChatRoom spyChatRoom = spy(chatRoom);
        given(userRepository.findById(gosu.getId())).willReturn(Optional.of(gosu));
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.ofNullable(spyChatRoom));

        //when
        ChatRoomPriceResponseDto result = chatRoomService.updatePrice(gosu.getId(), chatRoomId, updatePrice);

        //then
        verify(spyChatRoom, times(1)).setPrice(updatePrice);
        assertThat(result.getPrice()).isEqualTo(updatePrice);
    }
}