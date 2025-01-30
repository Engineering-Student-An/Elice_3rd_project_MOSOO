package com.team2.mosoo_backend.post.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PostResponseDto {

    private Long id;
    private String title;
    private String description;
    private int price;
    private String duration;
    private String address;
    private boolean isOffer;
    private String status;
    private LocalDateTime updatedAt;

    private Long userId;
    private String fullName;

    private String businessName;

    private List<String> imgUrls;

}
