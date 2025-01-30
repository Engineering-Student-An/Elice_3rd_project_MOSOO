package com.team2.mosoo_backend.user.entity;

import com.team2.mosoo_backend.category.entity.Category;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "gosu_info")
public class Gosu {
 @Id
 @GeneratedValue(strategy = GenerationType.IDENTITY)
 private Long id;

 @OneToOne
 @JoinColumn(name = "user_info_id", nullable = false) // UserInfo 엔티티와의 관계
 private UserInfo userInfo;

 @NotNull
 @Column(nullable = false)
 private String gender; // 성별

 @NotNull
 @Column(nullable = false)
 private String businessName; // 사업자명

 @NotNull
 @Column(nullable = false)
 private String businessNumber; // 사업자 번호

 @NotNull
 @Column( nullable = false)
 private String gosuInfoAddress; // 기술 제공 주소

 @NotNull
 @Column(nullable = false)
 private String gosuInfoPhone; // 기술 제공 번호

 @ManyToOne(fetch = FetchType.EAGER)
 @JoinColumn(name = "category_id")
 private Category category; // 기술 제공 카테고리

 @NotNull
 @Column(nullable = false)
 private LocalDateTime createdAt; // 생성일

 private LocalDateTime deletedAt; // 탈퇴 시간

 @Column
 private Integer points; // 포인트
}
