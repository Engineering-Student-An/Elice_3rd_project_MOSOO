package com.team2.mosoo_backend.bid.controller;

import com.team2.mosoo_backend.bid.dto.*;
import com.team2.mosoo_backend.bid.service.BidService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/bid")
public class BidController {

    private final BidService bidService;


    // 게시글 내에 입찰 조회
    @GetMapping("/{postId}")
    public ResponseEntity<BidListResponseDto> getBidByPost(@PathVariable("postId") Long postId) {
        BidListResponseDto bidListResponseDto = bidService.getBidByPost(postId);
        return ResponseEntity.status(200).body(bidListResponseDto);
    }

    // 로그인 회원의 입찰 조회
    @GetMapping("/myBid")
    public ResponseEntity<MyBidListResponseDto> getMyBid(@AuthenticationPrincipal UserDetails userDetails) {
        MyBidListResponseDto myBidListResponseDto = bidService.getMyBid(Long.parseLong(userDetails.getUsername()));
        return ResponseEntity.status(200).body(myBidListResponseDto);
    }

    // 입찰 생성
    @PostMapping("/{postId}")
    public ResponseEntity<BidResponseDto> createBid(
            @PathVariable("postId") Long postId,
            @RequestBody CreateBidRequestDto createBidRequestDto,
            @AuthenticationPrincipal UserDetails userDetails) {

        BidResponseDto bidResponseDto = bidService.createBidByPost(Long.parseLong(userDetails.getUsername()), postId, createBidRequestDto);

        return ResponseEntity.status(201).body(bidResponseDto);
    }

    // 입찰 데이터 수정
    @PutMapping
    public ResponseEntity<BidResponseDto> updateBid(@RequestBody UpdateBidRequestDto updateBidRequestDto) {

        BidResponseDto bidResponseDto = bidService.updateBid(updateBidRequestDto);

        return ResponseEntity.status(201).body(bidResponseDto);
    }

    // 입찰 삭제
    @DeleteMapping("/{bidId}")
    public ResponseEntity<BidResponseDto> deleteBid(@PathVariable("bidId") Long bidId) {

        BidResponseDto bidResponseDto = bidService.deleteBid(bidId);

        return ResponseEntity.status(200).body(bidResponseDto);
    }


}
