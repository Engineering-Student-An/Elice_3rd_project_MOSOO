package com.team2.mosoo_backend.oath.controller;


import com.team2.mosoo_backend.jwt.TokenProvider;
import com.team2.mosoo_backend.oath.dto.CreateAccessTokenResponse;
import com.team2.mosoo_backend.oath.service.TokenService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
public class TokenApiController {

    private final TokenService tokenService;
    private final TokenProvider tokenProvider;

    @GetMapping("/api/auth/token")
    public ResponseEntity<Void> validateAccessToken(@RequestParam(value = "accessToken") String accessToken) {

        if(!tokenProvider.validateAccessToken(accessToken)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    @PostMapping("/api/auth/token")
    public ResponseEntity<CreateAccessTokenResponse> reissueAccessToken(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        String refreshToken = "";
        for (Cookie cookie : cookies) {
            if ("refreshToken".equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        String newAccessToken = tokenService.reissueAccessToken(request, response, refreshToken);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new CreateAccessTokenResponse(newAccessToken));
    }
}
