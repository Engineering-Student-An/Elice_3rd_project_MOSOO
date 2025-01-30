package com.team2.mosoo_backend.user.service;

import com.team2.mosoo_backend.category.entity.Category;
import com.team2.mosoo_backend.category.repository.CategoryRepository;
import com.team2.mosoo_backend.exception.CustomException;
import com.team2.mosoo_backend.exception.ErrorCode;
import com.team2.mosoo_backend.user.dto.GosuInfoResponseDto;
import com.team2.mosoo_backend.user.dto.GosuRequestDto;
import com.team2.mosoo_backend.user.dto.GosuUpdateRequestDto;
import com.team2.mosoo_backend.user.entity.Authority;
import com.team2.mosoo_backend.user.entity.Gosu;
import com.team2.mosoo_backend.user.entity.UserInfo;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.mapper.UserMapper;
import com.team2.mosoo_backend.user.repository.GosuRepository;
import com.team2.mosoo_backend.user.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GosuService {

    private final GosuRepository gosuRepository;
    private final UserMapper userMapper;
    private final UserInfoRepository userInfoRepository;
    private final CategoryRepository categoryRepository;

    // 모든 고수 정보 호출
    public List<Gosu> getAllGosu() {
        return gosuRepository.findAll();
    }

    // 특정 고수를 ID로 가져오는 메서드
    public GosuInfoResponseDto getGosuByUserInfoId(Long userInfoId) {
        Gosu foundGosu = gosuRepository.findByUserInfoId(userInfoId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return userMapper.gosuToResponseDto(foundGosu);
    }

    // 고수 정보 생성
    public Long createGosu(GosuRequestDto gosuRequestDto) {

        // 매퍼 통해서 고수로 변환
        Gosu gosu = userMapper.requestToGosu(gosuRequestDto);

        // 유저인포 가져오기
        UserInfo userInfo = userInfoRepository.findById(gosuRequestDto.getUserInfoId())
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 고수 <-> 유저인포 연관관계 맺기
        gosu.setUserInfo(userInfo);

        // 유저인포에서 isGosu => true
        userInfo.setIsGosu(true);

        // 해당하는 유저의 권한을 GOSU로 변경
        Users user = userInfo.getUsers();
        user.setAuthority(Authority.ROLE_GOSU);


        Category category = categoryRepository.findById(gosuRequestDto.getCategoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        gosu.setCategory(category);

        gosu.setCreatedAt(LocalDateTime.now());

        Gosu savedGosu = gosuRepository.save(gosu);
        return savedGosu.getId();
    }

    // 고수 정보 업데이트
    public Long updateGosu(Long id, GosuUpdateRequestDto gosuUpdateRequestDto) {

        Gosu gosu = gosuRepository.findById(id).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        gosu.setGender(gosuUpdateRequestDto.getGender());
        gosu.setGosuInfoAddress(gosuUpdateRequestDto.getGosuInfoAddress());
        gosu.setGosuInfoPhone(gosuUpdateRequestDto.getGosuInfoPhone());

        gosu.setBusinessName(gosuUpdateRequestDto.getBusinessName());
        gosu.setBusinessNumber(gosuUpdateRequestDto.getBusinessNumber());
        gosu.setCategory(categoryRepository.findById(gosuUpdateRequestDto.getCategoryId())
                .orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND)) );

        gosuRepository.save(gosu);

        return gosu.getId();
    }

    // 고수 정보 삭제
    public void deleteGosu(Long userinfoId) {
        // 고수 찾기
        Gosu gosu = gosuRepository.findByUserInfoId(userinfoId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 찾은 고수 내부에서 UserInfo 찾기
        UserInfo userInfo = gosu.getUserInfo();
        userInfo.setIsGosu(false);

        Users user = userInfo.getUsers();
        user.setAuthority(Authority.ROLE_USER);

        gosuRepository.delete(gosu);
    }
}
