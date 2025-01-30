package com.team2.mosoo_backend.post.controller;


import com.team2.mosoo_backend.post.dto.*;
import com.team2.mosoo_backend.post.service.PostService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/post")
public class PostController {

    private final PostService postService;

    // 게시글 전체 조회 요청
    @GetMapping("/admin/posts")
    public ResponseEntity<PostListResponseDto> getAllPosts(
            @RequestParam(required = false, value = "page", defaultValue = "1") @Positive int page,
            @AuthenticationPrincipal UserDetails userDetails) {

        PostListResponseDto postList = postService.getAllPosts(Long.parseLong(userDetails.getUsername()), page);

        return ResponseEntity.status(200).body(postList);
    }

    // 게시글 작성 요청
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CreatePostResponseDto> createRequestPost(
            @AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute CreatePostRequestDto createPostRequestDto,
            @RequestParam("isOffer") boolean isOffer) throws IOException {

        CreatePostResponseDto createPost = postService.createPost(Long.parseLong(userDetails.getUsername()), createPostRequestDto, isOffer);

        return ResponseEntity.status(201).body(createPost);
    }

    // 고수 / 일반 게시글 조회 요청
    @GetMapping("/postList")
    public ResponseEntity<PostListResponseDto> getOfferPosts(
            @RequestParam(required = false, value = "page", defaultValue = "1") @Positive int page,
            @RequestParam(value = "isOffer") boolean isOffer) {

        PostListResponseDto postList = postService.getPostsByIsOffer(page, isOffer);

        return ResponseEntity.status(200).body(postList);
    }

    // 조건 별 게시글 목록 조회 요청
    @GetMapping("/filterPosts")
    public ResponseEntity<SearchedPostListResponseDto> getPostsBy(
            @RequestParam(required = false, value = "page", defaultValue = "1") @Positive int page,
            @RequestParam(required = false, value = "categoryId") Long categoryId,
            @RequestParam(value = "isOffer") boolean isOffer,
            @RequestParam(required = false, value = "keyword", defaultValue = "") String keyword,
            @RequestParam(required = false, value = "address", defaultValue = "") String address) {

        SearchedPostListResponseDto postList = postService.getSearchedPost(page, categoryId, isOffer, keyword, address);

        return ResponseEntity.status(200).body(postList);
    }

    // 게시글 단건 조회
    @GetMapping("/{postId}")
    public ResponseEntity<PostResponseDto> getPostById(
            @PathVariable("postId") Long postId){
        PostResponseDto postResponseDto = postService.getPostById(postId);

        return ResponseEntity.status(200).body(postResponseDto);
    }

    // 로그인 회원 게시글 조회
    @GetMapping("/myPosts")
    public ResponseEntity<PostListResponseDto> getMyPosts(@AuthenticationPrincipal UserDetails userDetails,
                                                          @RequestParam(required = false, value = "page", defaultValue = "1") @Positive int page) {
        PostListResponseDto postListResponseDto = postService.getPostsByUser(Long.parseLong(userDetails.getUsername()), page);

        return ResponseEntity.status(200).body(postListResponseDto);
    }

    // 게시글 수정 요청
    @PutMapping
    public ResponseEntity<PostResponseDto> updatePost(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody PostUpdateRequestDto postUpdateRequestDto) {
        PostResponseDto postResponseDto = postService.updatePost(Long.parseLong(userDetails.getUsername()), postUpdateRequestDto);

        return ResponseEntity.status(201).body(postResponseDto);
    }

    // 게시글 삭제 요청
    @DeleteMapping("/{postId}")
    public ResponseEntity<PostResponseDto> deletePost(@PathVariable("postId") Long postId,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        PostResponseDto postResponseDto = postService.deletePost(Long.parseLong(userDetails.getUsername()), postId);
        return ResponseEntity.status(204).body(postResponseDto);
    }

    // 게시글 상태 수정 요청
    @PutMapping("/status/{postId}")
    public ResponseEntity<PostResponseDto> updatePostStatus(@RequestBody UpdatePostStatusRequestDto updatePostStatusRequestDto,
                                                            @AuthenticationPrincipal UserDetails userDetails){
        PostResponseDto postResponseDto = postService.updatePostStatus(Long.parseLong(userDetails.getUsername()), updatePostStatusRequestDto);

        return ResponseEntity.status(201).body(postResponseDto);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostResponseDto> updatePostStatus(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("postId") Long userId, @RequestParam("status") String status) {
        PostResponseDto postResponseDto = postService.updateStatus(Long.parseLong(userDetails.getUsername()), userId, status);
        return ResponseEntity.status(201).body(postResponseDto);
    }

}
