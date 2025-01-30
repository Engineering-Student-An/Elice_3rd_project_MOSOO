package com.team2.mosoo_backend.user.controller;

import com.team2.mosoo_backend.user.dto.GosuInfoResponseDto;
import com.team2.mosoo_backend.user.dto.GosuRequestDto;
import com.team2.mosoo_backend.user.dto.GosuUpdateRequestDto;
import com.team2.mosoo_backend.user.entity.Gosu;
import com.team2.mosoo_backend.user.repository.GosuRepository;
import com.team2.mosoo_backend.user.service.GosuService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import retrofit2.http.Path;

import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/gosu")
public class GosuController {

    private final GosuRepository gosuRepository;
    private final GosuService gosuService;

    // 모든 고수 정보 조회
    @GetMapping("/")
    public List<Gosu> getAllGosu() {
        return gosuService.getAllGosu();
    }

    // 특정 고수 정보 ID 조회
    @GetMapping("/{userInfoId}")
    public ResponseEntity<GosuInfoResponseDto> getGosuById(@PathVariable(value = "userInfoId") Long userInfoId) {
        GosuInfoResponseDto dto = gosuService.getGosuByUserInfoId(userInfoId);
        return ResponseEntity.status(HttpStatus.OK).body(dto);
    }

    // 고수 정보 저장
    @PostMapping("")
    public ResponseEntity<Long> createGosu(@RequestBody GosuRequestDto gosuRequestDto) {
        Long gosuId = gosuService.createGosu(gosuRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(gosuId);
    }

    // 고수 정보 업데이트
    @PutMapping("/{id}")
    public ResponseEntity<Long> updateGosu(@PathVariable(value = "id") Long id, @RequestBody GosuUpdateRequestDto gosuDetails) {
        Long updatedGosu = gosuService.updateGosu(id, gosuDetails);
        return ResponseEntity.status(HttpStatus.OK).body(updatedGosu);
    }

    // 고수 정보 삭제
    @DeleteMapping("/deleted/{userInfoId}")
    public ResponseEntity<Void> deleteGosu(@PathVariable(value = "userInfoId") Long userinfoId) {
        gosuService.deleteGosu(userinfoId);
        return ResponseEntity.noContent().build();
    }
}
