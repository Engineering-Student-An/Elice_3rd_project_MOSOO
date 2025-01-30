package com.team2.mosoo_backend.bid.entity;

import com.team2.mosoo_backend.bid.dto.UpdateBidRequestDto;
import com.team2.mosoo_backend.common.entity.BaseEntity;
import com.team2.mosoo_backend.post.entity.Post;
import com.team2.mosoo_backend.user.entity.Users;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Bid extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bid_id", unique = true, nullable = false)
    private long id;

    private int price;

    private LocalDateTime date;

    private boolean isSelected;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    public void setPrice(int price) {
        this.price = price;
    }
    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public void setPost(Post post) {
        this.post = post;
    }

    public void serUser(Users user) {
        this.user = user;
    }

    public void updateBid(UpdateBidRequestDto updateBidRequestDto) {
        this.price = updateBidRequestDto.getPrice();
        this.date = updateBidRequestDto.getDate();
    }

}
