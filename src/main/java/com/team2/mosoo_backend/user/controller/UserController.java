package com.team2.mosoo_backend.user.controller;


import com.team2.mosoo_backend.user.dto.ChangePasswordRequestDto;
import com.team2.mosoo_backend.user.dto.UserInfoRequestDto;
import com.team2.mosoo_backend.user.dto.UserResponseDto;
import com.team2.mosoo_backend.user.entity.UserInfo;
import com.team2.mosoo_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    // 유저 개인 정보 가져오기
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyMemberInfo(@AuthenticationPrincipal UserDetails userDetails) {
        UserResponseDto myInfoBySecurity = userService.getMyInfoBySecurity(Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.ok((myInfoBySecurity));
    }


    // 유저 비밀번호 변경
    @PutMapping("/password")
    public ResponseEntity<UserResponseDto> setMemberPassword(@Validated @RequestBody ChangePasswordRequestDto request) {
        return ResponseEntity.ok(userService.changeMemberPassword(request.getExPassword(), request.getNewPassword()));
    }

    // 유저 정보 변경
    @PutMapping("/userinfo")
    public ResponseEntity<UserInfo> updateUserInfoAddress(@RequestBody UserInfoRequestDto userInfoRequestDto) {
        Long userId = userService.getUserIdByEmail(userInfoRequestDto.getEmail());
        UserInfo userInfo = userService.updateUserInfoAddress(userId, userInfoRequestDto);
        return ResponseEntity.ok(userInfo);
    }



    // 유저 탈퇴
    @DeleteMapping("/deleted")
    public ResponseEntity<UserResponseDto> deleteUser(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.deleteUser(Long.parseLong(userDetails.getUsername())));
    }

}