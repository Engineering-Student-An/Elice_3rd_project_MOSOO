package com.team2.mosoo_backend.oath.util;


import com.team2.mosoo_backend.oath.entity.RefreshToken;
import com.team2.mosoo_backend.oath.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.team2.mosoo_backend.jwt.TokenProvider.REFRESH_TOKEN_EXPIRE_TIME;


@RequiredArgsConstructor
@Component
public class RefreshTokenCookieUtil {

    // 리프레시 토큰을 쿠키에 추가하는 메서드
    public void addRefreshTokenToCookie(HttpServletRequest request, HttpServletResponse response, String refreshToken) {
        int cookieMaxAge = (int) REFRESH_TOKEN_EXPIRE_TIME; // 쿠키 유효 기간 설정

        CookieUtil.deleteCookie(request, response, "refreshToken"); // 기존 쿠키 삭제
        CookieUtil.addCookie(response, "refreshToken", refreshToken, cookieMaxAge); // 새 쿠키 추가
    }
}
