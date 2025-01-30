package com.team2.mosoo_backend.post.entity;


import com.team2.mosoo_backend.category.entity.Category;
import com.team2.mosoo_backend.common.entity.BaseEntity;
import com.team2.mosoo_backend.user.entity.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "post")
@EntityListeners(AuditingEntityListener.class)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id", unique = true, nullable = false)
    private Long id;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description",nullable = true, columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false)
    private int price;

    @Column(name = "duration")
    private String duration;

    private String address;

    @Column(name = "is_offer", nullable = false)
    private boolean isOffer;

    @Column(name = "is_selected", nullable = false)
    private boolean isSelected;

    @Enumerated(EnumType.STRING)
    private Status status;

    @ElementCollection
    @CollectionTable(name = "post_img_urls", joinColumns = @JoinColumn(name = "post_id"))
    @Column(name = "post_img_url", columnDefinition = "TEXT")
    private List<String> ImgUrls = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;


    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setAddress(String address){
        this.address = address;
    }

    public void setIsOffer(boolean isOffer) {
        this.isOffer = isOffer;
    }

    public void setIsSelected(boolean isSelected) {
        this.isSelected = isSelected;
    }

    public void setImgUrls(List<String> ImgUrls) {
        this.ImgUrls = ImgUrls;
    }

    public void setStatus(String status) {
        this.status = Status.valueOf(status);
    }

    public void setMapping(Users user, Category category) {
        this.user = user;
        this.category = category;
    }
}
