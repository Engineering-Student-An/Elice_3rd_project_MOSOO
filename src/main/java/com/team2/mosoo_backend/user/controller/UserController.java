package com.team2.mosoo_backend.user.controller;


import com.team2.mosoo_backend.user.dto.ChangeNameRequestDto;
import com.team2.mosoo_backend.user.dto.ChangePasswordRequestDto;
import com.team2.mosoo_backend.user.dto.UserResponseDto;
import com.team2.mosoo_backend.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    // 유저 개인 정보 가져오기
    @GetMapping("/me")
    public ResponseEntity<UserResponseDto> getMyMemberInfo() {
        UserResponseDto myInfoBySecurity = userService.getMyInfoBySecurity();
        System.out.println(myInfoBySecurity.getFullName());
        return ResponseEntity.ok((myInfoBySecurity));
    }

    // 유저 이름 변경
    @PutMapping("/fullname")
    public ResponseEntity<UserResponseDto> setMemberUserName(@RequestBody ChangeNameRequestDto request) {
        return ResponseEntity.ok(userService.changeMemberUserName(request.getFullName()));
    }

    // 유저 비밀번호 변경
    @PutMapping("/password")
    public ResponseEntity<UserResponseDto> setMemberPassword(@Validated @RequestBody ChangePasswordRequestDto request) {
        return ResponseEntity.ok(userService.changeMemberPassword(request.getExPassword(), request.getNewPassword()));
    }


    // 유저 탈퇴
    @DeleteMapping("/delete")
    public ResponseEntity<UserResponseDto> deleteMember() {
        return ResponseEntity.ok(userService.deleteMember());
    }

}