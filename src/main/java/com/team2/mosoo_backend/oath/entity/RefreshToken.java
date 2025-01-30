package com.team2.mosoo_backend.oath.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Getter @Builder
@NoArgsConstructor
@AllArgsConstructor
@RedisHash(value = "refreshToken", timeToLive = 60 * 60 * 24 * 7)   // ttl 7일로 설정
public class RefreshToken{

    @Id
    private String userId;

    @Indexed
    private String refreshToken;

}