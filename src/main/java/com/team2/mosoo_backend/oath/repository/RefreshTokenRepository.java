package com.team2.mosoo_backend.oath.repository;


import com.team2.mosoo_backend.oath.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Repository;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository{

    private final RedisTemplate<String, Object> redisTemplate;

    // refresh token 을 redis 에 저장하는 메서드
    public void save(RefreshToken refreshToken){
        ValueOperations<String, Object> valueOperations = redisTemplate.opsForValue();

        // 이미 해당 userId를 key로 하는 refresh token이 존재하는 경우 제거
        if(!Objects.isNull(valueOperations.get(refreshToken.getUserId()))){
            redisTemplate.delete(refreshToken.getUserId());
        }

        valueOperations.set(refreshToken.getUserId(), refreshToken.getRefreshToken());  // redis에 refresh token 저장
        redisTemplate.expire(refreshToken.getUserId(), 60 * 60 * 24 * 7, TimeUnit.SECONDS);  // ttl 7일 지정
    }

    // userId를 key로 하는 refresh token 조회 메서드
    public Optional<RefreshToken> findByUserId(String userId){

        String refreshToken = (String) redisTemplate.opsForValue().get(userId);     // 해당 userId를 key로 하는 refresh token 값

        return (refreshToken == null) ? Optional.empty() : Optional.of(new RefreshToken(userId, refreshToken));
    }

    // userId를 key로 하는 refresh token 삭제 메서드
    public void delete(String userId){

        redisTemplate.delete(userId);
    }
}

