package com.team2.mosoo_backend.user.controller;

import com.team2.mosoo_backend.user.dto.TokenDto;
import com.team2.mosoo_backend.user.dto.UserRequestDto;
import com.team2.mosoo_backend.user.dto.UserResponseDto;
import com.team2.mosoo_backend.user.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입
    @PostMapping("")
    public ResponseEntity<UserResponseDto> signup(@Validated @RequestBody UserRequestDto requestDto) {
        return ResponseEntity.ok(authService.signup(requestDto));
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<TokenDto> login(
            @Validated @RequestBody UserRequestDto requestDto,
            HttpServletResponse response,
            HttpServletRequest request) {
        TokenDto tokenDto = authService.login(request, response, requestDto);
        return ResponseEntity.ok(tokenDto);
    }
}