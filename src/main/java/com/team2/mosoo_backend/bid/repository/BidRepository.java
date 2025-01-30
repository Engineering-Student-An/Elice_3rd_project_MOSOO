package com.team2.mosoo_backend.bid.repository;


import com.team2.mosoo_backend.bid.entity.Bid;
import com.team2.mosoo_backend.post.entity.Post;
import com.team2.mosoo_backend.user.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findAllByPostId(Long postId);

    List<Bid> findByUser(Users user);

    Bid findByPostAndUser(Post post, Users user);
}
