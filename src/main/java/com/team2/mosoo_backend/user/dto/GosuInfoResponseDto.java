package com.team2.mosoo_backend.user.dto;

import com.team2.mosoo_backend.category.entity.Category;
import com.team2.mosoo_backend.user.entity.UserInfo;
import jakarta.persistence.Column;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GosuInfoResponseDto {

    private Long userInfoId;
    private Long gosuId;
    private String gender;
    private String businessName;
    private String businessNumber;
    private String gosuInfoAddress;
    private String gosuInfoPhone;
    private Long categoryId;

    @NotNull
    @Column(nullable = false)
    private LocalDateTime createdAt; // 생성일

    private LocalDateTime deletedAt; // 탈퇴 시간

    @Column
    private Integer points; // 포인트
}
