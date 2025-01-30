package com.team2.mosoo_backend.chatting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team2.mosoo_backend.bid.dto.BidResponseDto;
import com.team2.mosoo_backend.chatting.dto.*;
import com.team2.mosoo_backend.chatting.service.ChatRoomService;
import com.team2.mosoo_backend.jwt.TokenProvider;
import com.team2.mosoo_backend.post.dto.PostResponseDto;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChatRoomController.class)
@AutoConfigureMockMvc(addFilters = false) // 필터 제외 (JWT 검증 제외)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("채팅방 컨트롤러 단위 테스트")
class ChatRoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TokenProvider tokenProvider;    // TokenProvider 빈을 로드하지 못하는 에러 해결 위해서 명시적으로 빈으로 등록

    @MockBean
    private ChatRoomService chatRoomService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    private Users user;

    @BeforeEach
    public void setUp() {
        // 테스트용 유저 정보 미리 저장
        user = Users.builder().email("email1234").id(1L).build();

        userRepository.save(user);
    }

    static final String URL = "/api/chatroom";
    static final Long chatRoomId = 1L;


    static ChatRoomResponseWrapperDto createChatRoomResponseWrapperDto() {

        List<ChatRoomResponseDto> list = new ArrayList<>();
        for (int i = 1; i < 6; i++) {
            ChatRoomResponseDto dto = new ChatRoomResponseDto((long) i, i + "번 유저", i + "번 채팅", LocalDateTime.now(), false, 1L, "게시글 제목");
            list.add(dto);
        }

        return new ChatRoomResponseWrapperDto(list, 1);
    }

    static ChatRoomInfoResponseDto createChatRoomInfoResponseDto() {

        PostResponseDto postResponseDto = new PostResponseDto();
        BidResponseDto bidResponseDto = new BidResponseDto();
        boolean isGosu = true;
        int price = 10000;

        return new ChatRoomInfoResponseDto(postResponseDto, bidResponseDto, isGosu, price);
    }

    static ChatRoomOpponentInfoResponseDto createChatRoomOpponentInfoResponseDto() {

        return new ChatRoomOpponentInfoResponseDto("상대 일반 유저", "test@test.com");
    }

    @Test
    @Order(1)
    @DisplayName("채팅방 전체 조회 메서드 테스트")
    @WithMockUser(username = "1", roles = "AUTHORITY_USER") // Mock 사용자 추가
    public void findChatRoomTest() throws Exception {

        //given
        given(chatRoomService.findAllChatRooms(eq(user.getId()), anyInt()))
                .willReturn(createChatRoomResponseWrapperDto());

        //when
        ResultActions resultActions = mockMvc.perform(get(URL + "s"));

        //then
        resultActions
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.chatRoomResponseDtoList.length()").value(5))
                .andExpect(jsonPath("$.chatRoomResponseDtoList[0].chatRoomId").value(1L))
                .andExpect(jsonPath("$.chatRoomResponseDtoList[0].opponentFullName").value("1번 유저"))
                .andExpect(jsonPath("$.chatRoomResponseDtoList[0].lastChatMessage").value("1번 채팅"))
                .andExpect(jsonPath("$.chatRoomResponseDtoList[0].existUnchecked").value(false));
    }

    @Test
    @Order(2)
    @DisplayName("채팅방 관련 정보 조회 메서드 테스트")
    @WithMockUser(username = "1", roles = "AUTHORITY_USER") // Mock 사용자 추가
    public void findChatRoomInfoTest() throws Exception {
        //given
        given(chatRoomService.findChatRoomInfo(user.getId(), chatRoomId))
                .willReturn(createChatRoomInfoResponseDto());

        //when
        ResultActions resultActions = mockMvc.perform(get(URL + "/" + chatRoomId + "/info"));

        //then
        resultActions
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.postResponseDto").exists())
                .andExpect(jsonPath("$.bidResponseDto").exists())
                .andExpect(jsonPath("$.gosu").value(true))
                .andExpect(jsonPath("$.price").value(10000));
    }

    @Test
    @Order(3)
    @DisplayName("채팅방 상대방 정보 조회 메서드 테스트")
    @WithMockUser(username = "1", roles = "AUTHORITY_USER") // Mock 사용자 추가
    public void findChatRoomOpponentInfoTest() throws Exception {
        //given
        given(chatRoomService.findChatRoomOpponentInfo(user.getId(), chatRoomId))
                .willReturn(createChatRoomOpponentInfoResponseDto());

        //when
        ResultActions resultActions = mockMvc.perform(get(URL + "/" + chatRoomId + "/user-info"));

        //then
        resultActions
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.opponentGosu").value(false))
                .andExpect(jsonPath("$.fullName").value("상대 일반 유저"))
                .andExpect(jsonPath("$.email").value("test@test.com"));
    }

    @Test
    @Order(4)
    @DisplayName("채팅방 생성 메서드 테스트")
    @WithMockUser(username = "1", roles = "AUTHORITY_USER") // Mock 사용자 추가
    public void createChatRoomTest() throws Exception {
        //given
        given(chatRoomService.createChatRoom(eq(user.getId()), any()))
                .willReturn(new ChatRoomCreateResponseDto(chatRoomId));

        //when
        ResultActions resultActions = mockMvc.perform(post(URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new ChatRoomRequestDto(3L, 1L, 1L))));

        //then
        resultActions
                .andExpect(status().is(201))
                .andExpect(jsonPath("$.chatRoomId").value(chatRoomId));
    }

    @Test
    @Order(5)
    @DisplayName("채팅방 나가기 메서드 테스트")
    @WithMockUser(username = "1", roles = "AUTHORITY_USER") // Mock 사용자 추가
    public void quitChatRoomTest() throws Exception {
        //given
        given(chatRoomService.quitChatRoom(user.getId(), chatRoomId))
                .willReturn(new ChatRoomDeleteResponseDto(chatRoomId));

        //when
        ResultActions resultActions = mockMvc.perform(delete(URL + "/" + chatRoomId));

        //then
        resultActions
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.chatRoomId").value(chatRoomId));
    }

    @Test
    @Order(6)
    @DisplayName("채팅방 내부 가격 수정 메서드 테스트")
    @WithMockUser(username = "1", roles = "AUTHORITY_USER") // Mock 사용자 추가
    public void updatePriceTest() throws Exception {
        //given
        int updatePrice = 20000;
        given(chatRoomService.updatePrice(user.getId(), chatRoomId, updatePrice))
                .willReturn(new ChatRoomPriceResponseDto(updatePrice));

        //when
        ResultActions resultActions = mockMvc.perform(patch(URL + "/" + chatRoomId + "/price")
                .param("price", String.valueOf(updatePrice)));

        //then
        resultActions
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.price").value(updatePrice));
    }
}