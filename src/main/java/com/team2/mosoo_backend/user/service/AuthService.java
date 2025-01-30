package com.team2.mosoo_backend.user.service;


import com.team2.mosoo_backend.exception.CustomException;
import com.team2.mosoo_backend.exception.ErrorCode;
import com.team2.mosoo_backend.jwt.TokenProvider;

import com.team2.mosoo_backend.oath.util.RefreshTokenCookieUtil;
import com.team2.mosoo_backend.user.dto.TokenDto;
import com.team2.mosoo_backend.user.dto.UserRequestDto;
import com.team2.mosoo_backend.user.dto.UserResponseDto;
import com.team2.mosoo_backend.user.entity.Authority;
import com.team2.mosoo_backend.user.entity.UserInfo;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.mapper.UserMapper;
import com.team2.mosoo_backend.user.repository.UserInfoRepository;
import com.team2.mosoo_backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final AuthenticationManagerBuilder managerBuilder;
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TokenProvider tokenProvider;
    private final RefreshTokenCookieUtil refreshTokenCookieUtil;
    private final UserInfoRepository userInfoRepository;

    public UserResponseDto signup(UserRequestDto requestDto) {
        // 이메일 중복 확인
        if (userRepository.existsByEmail(requestDto.getEmail())) {
            throw new CustomException(ErrorCode.DUPLICATE_RESOURCE);
        }

        // User 엔티티 생성 및 저장
        Users users = userMapper.requestToUser(requestDto);
        users.setAuthority(Authority.ROLE_USER);
        Users savedUser = userRepository.save(users);

        // UserInfo 엔티티 생성
        UserInfo userInfo = new UserInfo();
        userInfo.setUsers(savedUser); // 사용자와 연관된 UserInfo 생성
        userInfo.setAddress(""); // 기본값 설정 (필요 시 수정 가능)
        userInfo.setIsGosu(false); // 기본값 설정 (필요 시 수정 가능)

        // UserInfo 저장
        userInfoRepository.save(userInfo);

        return userMapper.userToResponse(savedUser); // 응답 DTO 반환
    }

    public TokenDto login(HttpServletRequest request, HttpServletResponse response, UserRequestDto requestDto) {

        UsernamePasswordAuthenticationToken authenticationToken = requestDto.toAuthentication();
        Authentication authentication = managerBuilder.getObject().authenticate(authenticationToken);

        // 매번 로그인 할 때마다 access token, refresh token 발급
        TokenDto tokenDto = tokenProvider.generateAccessToken(authentication.getName());
        Users users = userRepository.findByEmail(requestDto.getEmail()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        tokenProvider.generateRefreshToken(request, response, users.getId().toString());

        return tokenDto;
    }

}
