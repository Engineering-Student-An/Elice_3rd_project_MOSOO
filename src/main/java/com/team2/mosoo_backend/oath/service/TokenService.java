package com.team2.mosoo_backend.oath.service;


import com.team2.mosoo_backend.exception.CustomException;
import com.team2.mosoo_backend.exception.ErrorCode;
import com.team2.mosoo_backend.jwt.TokenProvider;
import com.team2.mosoo_backend.oath.entity.RefreshToken;
import com.team2.mosoo_backend.oath.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;


@RequiredArgsConstructor
@Service
public class TokenService {

    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public String reissueAccessToken(HttpServletRequest request, HttpServletResponse response, String refreshToken) {

        String userId = "null";

        // redis 에 저장된 refresh token 조회
        Set<String> keysWithValue = redisTemplate.keys("*"); // 모든 키 가져옴

        for (String key : keysWithValue) {
            Object storedValue = redisTemplate.opsForValue().get(key); // 각 키의 값 가져옴
            if (storedValue.equals(refreshToken)) { // 값이 일치하는지 확인
                userId = key;   // 값이 일치하는 키를 저장
                break;
            }
        }

        Optional<RefreshToken> savedRefreshToken = refreshTokenRepository.findByUserId(userId);
        // redis에 저장된 refresh token과 일치하는 경우가 없는 경우
        if(savedRefreshToken.isEmpty() || userId.equals("null")) {
            // 재로그인 해야 함 => 토큰 권한 정보 관련 예외 던짐
            throw new CustomException(ErrorCode.INVALID_AUTH_TOKEN);
        }

        // refresh token 재발급 및 redis 에 저장
        tokenProvider.generateRefreshToken(request, response, userId);

        // access token 재발급
        return tokenProvider.generateAccessToken(userId).getAccessToken();

    }
}
