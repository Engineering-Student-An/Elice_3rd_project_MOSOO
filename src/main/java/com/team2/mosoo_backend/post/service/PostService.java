package com.team2.mosoo_backend.post.service;


import com.team2.mosoo_backend.category.entity.Category;
import com.team2.mosoo_backend.category.repository.CategoryRepository;
import com.team2.mosoo_backend.exception.CustomException;
import com.team2.mosoo_backend.exception.ErrorCode;
import com.team2.mosoo_backend.post.dto.*;
import com.team2.mosoo_backend.post.entity.Post;
import com.team2.mosoo_backend.post.mapper.PostMapper;
import com.team2.mosoo_backend.post.repository.PostRepository;
import com.team2.mosoo_backend.user.entity.Authority;
import com.team2.mosoo_backend.user.entity.Gosu;
import com.team2.mosoo_backend.user.entity.UserInfo;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.repository.GosuRepository;
import com.team2.mosoo_backend.user.repository.UserInfoRepository;
import com.team2.mosoo_backend.user.repository.UserRepository;
import com.team2.mosoo_backend.utils.s3bucket.service.S3BucketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {

    private final UserInfoRepository userInfoRepository;
    private final GosuRepository gosuRepository;
    @Value("${default.image.url}")
    private String defaultImageUrl;

    private final S3BucketService s3BucketService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final PostMapper postMapper;


    // 게시글 전체 조회
    public PostListResponseDto getAllPosts(Long userId, int page) {

        Pageable pageable = PageRequest.of(page - 1, 10, Sort.by("id").descending());

        Page<Post> posts = postRepository.findAll(pageable);
        Users user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        // 권한 확인
        if(user.getAuthority() != Authority.ROLE_ADMIN){
            throw new CustomException(ErrorCode.USER_NOT_AUTHORIZED);
        }

        List<PostResponseDto> postResponseDtoList = new ArrayList<>();

        for (Post post : posts) {
            PostResponseDto postResponseDto = postMapper.postToPostResponseDto(post);
            postResponseDto.setUserId(post.getUser().getId());
            postResponseDto.setFullName(post.getUser().getFullName());
            postResponseDtoList.add(postResponseDto);
        }

        int totalPages = (posts.getTotalPages() == 0 ? 1 : posts.getTotalPages());

        return new PostListResponseDto(postResponseDtoList, totalPages);

    }

    // 게시글 생성
    @Transactional
    public CreatePostResponseDto createPost(Long userId, CreatePostRequestDto createPostRequestDto, boolean isOffer) throws IOException {

        Post post = postMapper.createPostRequestDtoToPost(createPostRequestDto);
        Users user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        UserInfo userInfo = userInfoRepository.findByUsers(user).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        Category category = categoryRepository.findById(createPostRequestDto.getCategoryId()).orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));

        // 연관관계 매핑
        post.setMapping(user, category);

        // 고수/ 일반 게시글 분류
        post.setIsOffer(isOffer);

        // 유저 정보로 주소 저장
        post.setAddress(userInfo.getAddress());

        // 게시글 상태 초기화
        post.setStatus(createPostRequestDto.getStatus());

        // 이미지 저장
        if (createPostRequestDto.getImageUrls() != null) {
            List<MultipartFile> imageList = createPostRequestDto.getImageUrls();

            List<String> postImageUrls = s3BucketService.uploadFileList(imageList);

            post.setImgUrls(postImageUrls);
        } else {
            List<String> postImageUrls = new ArrayList<>();

            postImageUrls.add(defaultImageUrl);

            post.setImgUrls(postImageUrls);
        }


        return postMapper.postToCreatePostResponseDto(postRepository.save(post));
    }

    // 고수 / 일반 게시글 조회 + 페이지네이션
    public PostListResponseDto getPostsByIsOffer(int page, boolean isOffer) {

        Pageable pageable = PageRequest.of(page - 1, 9, Sort.by("id").descending());

        Page<Post> posts = postRepository.findByIsOffer(pageable, isOffer);

        List<PostResponseDto> postResponseDtoList = new ArrayList<>();

        for (Post post : posts) {
            postResponseDtoList.add(postMapper.postToPostResponseDto(post));
        }

        int totalPages = (posts.getTotalPages() == 0 ? 1 : posts.getTotalPages());

        return new PostListResponseDto(postResponseDtoList, totalPages);
    }

    // 게시글 수정
    @Transactional
    public PostResponseDto updatePost(Long userId, PostUpdateRequestDto postUpdateRequestDto) {
        Post post = postRepository.findById(postUpdateRequestDto.getId()).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        Users users = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 작성자 인증
        if(post.getUser() != users && users.getAuthority() != Authority.ROLE_ADMIN){
            throw new CustomException(ErrorCode.USER_NOT_AUTHORIZED);
        }

        Post savedPost = update(post, postUpdateRequestDto);

        return postMapper.postToPostResponseDto(savedPost);
    }

    // 게시글 수정 로직
    private Post update(Post existPost, PostUpdateRequestDto postUpdateRequestDto) {
        existPost.setTitle(postUpdateRequestDto.getTitle());
        existPost.setDescription(postUpdateRequestDto.getDescription());
        existPost.setPrice(postUpdateRequestDto.getPrice());
        existPost.setDuration(postUpdateRequestDto.getDuration());
        return postRepository.save(existPost);
    }

    // 게시글 삭제
    @Transactional
    public PostResponseDto deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        Users users = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 작성자 인증
        if(post.getUser() != users){
            throw new CustomException(ErrorCode.USER_NOT_AUTHORIZED);
        }

        PostResponseDto postResponseDto = postMapper.postToPostResponseDto(post);

        postRepository.delete(post);

        return postResponseDto;
    }

    // 게시글 상태 수정
    @Transactional
    public PostResponseDto updatePostStatus(Long userId, UpdatePostStatusRequestDto updatePostStatusRequestDto) {

        Post post = postRepository.findById(updatePostStatusRequestDto.getId()).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        Users users = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 작성자 인증
        if(post.getUser() != users){
            throw new CustomException(ErrorCode.USER_NOT_AUTHORIZED);
        }

        post.setStatus(updatePostStatusRequestDto.getStatus());

        return postMapper.postToPostResponseDto(postRepository.save(post));
    }

    //단건 상세 조회
    public PostResponseDto getPostById(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        PostResponseDto postResponseDto = postMapper.postToPostResponseDto(post);

        postResponseDto.setUserId(post.getUser().getId());
        postResponseDto.setFullName(post.getUser().getFullName());

        if(post.isOffer()){
            UserInfo userInfo = userInfoRepository.findByUsers(post.getUser()).orElseThrow(() -> new CustomException(ErrorCode.USER_INFO_NOT_FOUND));
            Gosu gosu = gosuRepository.findByUserInfoId(userInfo.getId()).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
            postResponseDto.setBusinessName(gosu.getBusinessName());
        }


        return postResponseDto;
    }

    //조건 필터링 게시글 목록 조회
    public SearchedPostListResponseDto getSearchedPost(int page, Long categoryId, boolean isOffer, String keyword, String address) {
        Pageable pageable = PageRequest.of(page - 1, 9, Sort.by("id").descending());

        // Category 확인
        Category category = null;
        if (categoryId != null) {
            category = categoryRepository.findById(categoryId).orElseThrow(() -> new CustomException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        // 키워드와 주소 여부 확인
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        boolean hasAddress = address != null && !address.isBlank();

        // 조건에 따라 Post 검색
        Page<Post> posts;

        if (category != null && hasKeyword && hasAddress) {
            // 카테고리, 키워드, 주소 모두 있는 경우
            posts = postRepository.findByTitleContainingAndAddressContainingAndIsOfferAndCategory(keyword, address, isOffer, category, pageable);
        } else if (category != null && hasKeyword) {
            // 카테고리와 키워드만 있는 경우
            posts = postRepository.findByTitleContainingAndIsOfferAndCategory(keyword, isOffer, category, pageable);
        } else if (category != null && hasAddress) {
            // 카테고리와 주소만 있는 경우
            posts = postRepository.findByAddressContainingAndIsOfferAndCategory(address, isOffer, category, pageable);
        } else if (category != null) {
            // 카테고리만 있는 경우
            posts = postRepository.findByIsOfferAndCategory(pageable, isOffer, category);
        } else if (hasKeyword && hasAddress) {
            // 키워드와 주소만 있는 경우
            posts = postRepository.findByTitleContainingAndAddressContainingAndIsOffer(keyword, address, isOffer, pageable);
        } else if (hasKeyword) {
            // 키워드만 있는 경우
            posts = postRepository.findByTitleContainingAndIsOffer(keyword, isOffer, pageable);
        } else if (hasAddress) {
            // 주소만 있는 경우
            posts = postRepository.findByAddressContainingAndIsOffer(address, isOffer, pageable);
        } else {
            // 조건이 없는 경우 (기본 검색)
            posts = postRepository.findByIsOffer(pageable, isOffer);
        }

        // 검색된 Post를 PostResponseDto로 변환
        List<PostResponseDto> postResponseDtoList = posts.getContent().stream().map(postMapper::postToPostResponseDto).collect(Collectors.toList());

        int totalPages = Math.max(posts.getTotalPages(), 1);

        return new SearchedPostListResponseDto(postResponseDtoList, totalPages, keyword, address, category != null ? category.getName() : null);
    }

    // 로그인 유저 게시글 조회
    public PostListResponseDto getPostsByUser(Long userId, int page) {
        Pageable pageable = PageRequest.of(page - 1, 9, Sort.by("id").descending());

        Users user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Page<Post> posts = postRepository.findByUser(user, pageable);

        List<PostResponseDto> postResponseDtoList = new ArrayList<>();

        for (Post post : posts) {
            postResponseDtoList.add(postMapper.postToPostResponseDto(post));
        }

        int totalPages = (posts.getTotalPages() == 0 ? 1 : posts.getTotalPages());

        return new PostListResponseDto(postResponseDtoList, totalPages);

    }

    public PostResponseDto updateStatus(Long userId, Long postId, String status) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        Users user = userRepository.findById(userId).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        if(post.getUser() != user){
            throw new CustomException(ErrorCode.USER_NOT_AUTHORIZED);
        }
        post.setStatus(status);
        return postMapper.postToPostResponseDto(postRepository.save(post));
    }
}
