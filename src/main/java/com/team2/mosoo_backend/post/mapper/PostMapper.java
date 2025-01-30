package com.team2.mosoo_backend.post.mapper;


import com.team2.mosoo_backend.post.dto.CreatePostRequestDto;
import com.team2.mosoo_backend.post.dto.CreatePostResponseDto;
import com.team2.mosoo_backend.post.dto.PostResponseDto;
import com.team2.mosoo_backend.post.entity.Post;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface PostMapper {

    PostResponseDto postToPostResponseDto(Post post);

    Post createPostRequestDtoToPost(CreatePostRequestDto CreatePostRequestDto);

    CreatePostResponseDto postToCreatePostResponseDto(Post post);

}
