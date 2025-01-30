package com.team2.mosoo_backend.bid.service;


import com.team2.mosoo_backend.bid.dto.*;
import com.team2.mosoo_backend.bid.entity.Bid;
import com.team2.mosoo_backend.bid.mapper.BidMapper;
import com.team2.mosoo_backend.bid.repository.BidRepository;
import com.team2.mosoo_backend.exception.CustomException;
import com.team2.mosoo_backend.exception.ErrorCode;
import com.team2.mosoo_backend.post.entity.Post;
import com.team2.mosoo_backend.post.repository.PostRepository;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BidService {

    private final BidRepository bidRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final BidMapper bidMapper;


    // 입찰 목록 조회
    public BidListResponseDto getBidByPost(Long postId) {

        List<BidResponseDto> dtoList = new ArrayList<>();
        List<Bid> bidList = bidRepository.findAllByPostId(postId);

        for(Bid bid : bidList) {
            BidResponseDto bidResponseDto = bidMapper.bidToBidResponseDto(bid);
            bidResponseDto.setFullName(bid.getUser().getFullName());
            bidResponseDto.setUserId(bid.getUser().getId());
            dtoList.add(bidResponseDto);
        }

        return new BidListResponseDto(dtoList);
    }


    // 입찰 생성
    @Transactional
    public BidResponseDto createBidByPost(Long userId, Long postId, CreateBidRequestDto createBidRequestDto) {

        // 요청 DTO를 Bid 엔티티로 변환
        Bid bid = bidMapper.createBidRequestDtoToBid(createBidRequestDto);
        Post post = postRepository.findById(postId).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        Users user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 기존 Bid 조회
        Bid existingBid = bidRepository.findByPostAndUser(post, user);
        if (existingBid != null) {
            existingBid.setPrice(createBidRequestDto.getPrice());
            existingBid.setDate(createBidRequestDto.getDate());
            bid = existingBid;
        }
        else{
            // entity 연관 매핑
            bid.setPost(post);
            bid.serUser(user);
        }

        Bid createdBid = bidRepository.save(bid);

        BidResponseDto bidResponseDto = bidMapper.bidToBidResponseDto(createdBid);
        bidResponseDto.setFullName(user.getFullName());
        bidResponseDto.setUserId(user.getId());

        return bidResponseDto;
    }

    //입찰 수정
    @Transactional
    public BidResponseDto updateBid(UpdateBidRequestDto updateBidRequestDto) {

        Bid bid = bidRepository.findById(updateBidRequestDto.getBidId()).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        bid.updateBid(updateBidRequestDto);

        return bidMapper.bidToBidResponseDto(bidRepository.save(bid));

    }

    // 입찰 삭제
    @Transactional
    public BidResponseDto deleteBid(Long bidId) {
        Bid bid = bidRepository.findById(bidId).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        BidResponseDto bidResponseDto = bidMapper.bidToBidResponseDto(bid);

        bidRepository.delete(bid);

        return bidResponseDto;
    }

    // 회원 입찰 조회
    public MyBidListResponseDto getMyBid(Long userId) {
        List<MyBidResponseDto> dtoList = new ArrayList<>();
        Users user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        List<Bid> bidList = bidRepository.findByUser(user);

        for(Bid bid : bidList) {
            MyBidResponseDto bidResponseDto = bidMapper.bidToMyBidResponseDto(bid);
            Long postId = bid.getPost().getId();
            Post post = postRepository.findById(postId).orElseGet(() -> null);
            bidResponseDto.setPostId(postId);
            bidResponseDto.setPostTitle( (post != null) ? post.getTitle() : null);
            dtoList.add(bidResponseDto);
        }

        return new MyBidListResponseDto(dtoList);
    }
}