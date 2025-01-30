package com.team2.mosoo_backend.oath;


import com.team2.mosoo_backend.exception.CustomException;
import com.team2.mosoo_backend.exception.ErrorCode;
import com.team2.mosoo_backend.jwt.TokenProvider;
import com.team2.mosoo_backend.user.entity.UserInfo;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.repository.UserInfoRepository;
import com.team2.mosoo_backend.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;


@RequiredArgsConstructor
@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final TokenProvider tokenProvider;
    private static final String URI = "https://mosooo.netlify.app/tokenCheck";
    private final UserRepository userRepository;
    private final UserInfoRepository userInfoRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal(); // 인증된 사용자의 정보를 가져옴
        Users users = userRepository.findByEmail((String) oAuth2User.getAttributes().get("email"))
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND)); // 사용자 정보 조회

        // accessToken, refreshToken 발급
        String accessToken = tokenProvider.generateAccessToken(users.getId().toString()).getAccessToken();
        tokenProvider.generateRefreshToken(request, response, users.getEmail());

        // UserInfo 조회 및 생성
        UserInfo userInfo = userInfoRepository.findByUsersId(users.getId())
                .orElseGet(() -> {
                    UserInfo newUserInfo = new UserInfo();
                    newUserInfo.setUsers(users); // 사용자와 연관된 UserInfo 생성
                    newUserInfo.setAddress(""); // 기본값 설정 (필요 시 수정 가능)
                    newUserInfo.setIsGosu(false); // 기본값 설정 (필요 시 수정 가능)
                    // UserInfo 저장
                    return userInfoRepository.save(newUserInfo);
                });

        // 토큰 전달을 위한 redirect
        String redirectUrl = UriComponentsBuilder.fromUriString(URI)
                .queryParam("accessToken", accessToken)
                .build().toUriString();

        response.sendRedirect(redirectUrl);

    }


}
