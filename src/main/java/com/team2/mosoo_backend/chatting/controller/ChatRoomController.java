package com.team2.mosoo_backend.chatting.controller;

import com.team2.mosoo_backend.chatting.dto.*;
import com.team2.mosoo_backend.chatting.service.ChatRoomService;
import com.team2.mosoo_backend.config.swagger.ApiExceptionResponseExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import static com.team2.mosoo_backend.exception.ErrorCode.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@Validated
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    // 채팅방 전체 조회 (채팅 내역 조회)
    @GetMapping("/chatrooms")
    @Operation(summary = "채팅방 전체 조회", description = "로그인한 유저의 채팅방 전체 조회")
    @ApiResponse(responseCode = "200", description = "채팅방 전체 조회 성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChatRoomResponseWrapperDto.class)))
    public ResponseEntity<ChatRoomResponseWrapperDto> findAllChatRooms(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(value = "page", defaultValue = "1")
            @Positive int page) {

        ChatRoomResponseWrapperDto chatRoomResponseWrapperDto
                = chatRoomService.findAllChatRooms(Long.parseLong(userDetails.getUsername()),page);
        return ResponseEntity.status(HttpStatus.OK).body(chatRoomResponseWrapperDto);
    }

    // 채팅방 세부 정보 조회 (게시글, 입찰, 가격 정보 조회)
    @GetMapping("/chatroom/{chatRoomId}/info")
    @Operation(summary = "채팅방 관련 정보 조회", description = "채팅방 관련 게시글 정보, 입찰 정보, 가격 조회")
    @ApiExceptionResponseExamples({USER_NOT_AUTHORIZED, CHAT_ROOM_NOT_FOUND})
    /*
        403 에러 : 유저 정보가 일치하지 않는 경우
        404 에러 : 채팅방을 찾을 수 없는 경우
    */
    @ApiResponse(responseCode = "200", description = "채팅방 관련 세부 정보 조회 성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChatRoomInfoResponseDto.class)))
    public ResponseEntity<ChatRoomInfoResponseDto> findChatRoomInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("chatRoomId") Long chatRoomId) {

        ChatRoomInfoResponseDto chatRoomInfoResponseDto
                = chatRoomService.findChatRoomInfo(Long.parseLong(userDetails.getUsername()), chatRoomId);
        return ResponseEntity.status(HttpStatus.OK).body(chatRoomInfoResponseDto);
    }

    // 채팅방 상대방 정보 조회
    @GetMapping("/chatroom/{chatRoomId}/user-info")
    @Operation(summary = "채팅방 관련 정보 조회", description = "채팅방 관련 상대 정보 조회")
    @ApiExceptionResponseExamples({USER_NOT_AUTHORIZED, CHAT_ROOM_NOT_FOUND})
    /*
        403 에러 : 유저 정보가 일치하지 않는 경우
        404 에러 : 채팅방을 찾을 수 없는 경우
    */
    @ApiResponse(responseCode = "200", description = "채팅방 관련 상대 정보 조회 성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChatRoomOpponentInfoResponseDto.class)))
    public ResponseEntity<ChatRoomOpponentInfoResponseDto> findChatRoomOpponentInfo(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("chatRoomId") Long chatRoomId) {

        ChatRoomOpponentInfoResponseDto chatRoomOpponentInfoResponseDto
                = chatRoomService.findChatRoomOpponentInfo(Long.parseLong(userDetails.getUsername()), chatRoomId);
        return ResponseEntity.status(HttpStatus.OK).body(chatRoomOpponentInfoResponseDto);
    }

    @PostMapping("/chatroom")
    @Operation(summary = "채팅방 생성", description = "유저가 고수와의 채팅방 생성")
    @ApiExceptionResponseExamples({USER_NOT_FOUND, POST_NOT_FOUND, BID_NOT_FOUND, DUPLICATE_CHAT_ROOM})
    /*
        404 에러 : 유저(고수) 정보를 찾을 수 없는 경우
        404 에러 : 게시글 정보를 찾을 수 없는 경우
        404 에러 : 입찰 정보를 찾을 수 없는 경우
        409 에러 : 해당 입찰에 대한 채팅방이 이미 존재하는 경우 (변경 예정)
     */
    @ApiResponse(responseCode = "201", description = "채팅방 생성 성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChatRoomCreateResponseDto.class)))
    public ResponseEntity<ChatRoomCreateResponseDto> createChatRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ChatRoomRequestDto chatRoomRequestDto) {

        ChatRoomCreateResponseDto result
                = chatRoomService.createChatRoom(Long.parseLong(userDetails.getUsername()), chatRoomRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @DeleteMapping("/chatroom/{chatRoomId}")
    @Operation(summary = "채팅방 나가기", description = "(일반유저/고수유저) 채팅방 나가기")
    @ApiExceptionResponseExamples({USER_NOT_AUTHORIZED, CHAT_ROOM_NOT_FOUND, CHAT_ROOM_DELETED})
    /*
        403 에러 : 유저 정보가 일치하지 않는 경우
        404 에러 : 채팅방 정보를 찾을 수 없는 경우
        410 에러 : 이미 채팅방을 나간 경우
     */
    @ApiResponse(responseCode = "200", description = "채팅방 나가기 성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChatRoomDeleteResponseDto.class)))
    public ResponseEntity<ChatRoomDeleteResponseDto> quitChatRoom(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("chatRoomId") Long chatRoomId) {

        ChatRoomDeleteResponseDto result
                = chatRoomService.quitChatRoom(Long.parseLong(userDetails.getUsername()), chatRoomId);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

    @PatchMapping("/chatroom/{chatRoomId}/price")
    @Operation(summary = "채팅방에서 가격 변경", description = "주문서 작성을 위한 조정 가격 반영")
    @ApiExceptionResponseExamples({INVALID_PRODUCT_PRICE, USER_NOT_AUTHORIZED, CHAT_ROOM_NOT_FOUND})
    /*
        400 에러 : 가격이 0원보다 적은 경우
        403 에러 : 고수유저가 아니거나 채팅방 참여 고수가 아닌 경우
        404 에러 : 채팅방 정보를 찾을 수 없는 경우
     */
    @ApiResponse(responseCode = "200", description = "가격 변경 성공",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ChatRoomPriceResponseDto.class)))
    public ResponseEntity<ChatRoomPriceResponseDto> updatePrice(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable("chatRoomId") Long chatRoomId,
            @RequestParam(value = "price") @PositiveOrZero int price) {

        ChatRoomPriceResponseDto result
                = chatRoomService.updatePrice(Long.parseLong(userDetails.getUsername()), chatRoomId, price);
        return ResponseEntity.status(HttpStatus.OK).body(result);
    }

}
