package com.team2.mosoo_backend.user.mapper;



import com.team2.mosoo_backend.category.entity.Category;
import com.team2.mosoo_backend.user.dto.*;
import com.team2.mosoo_backend.user.entity.Gosu;
import com.team2.mosoo_backend.user.entity.Users;
import com.team2.mosoo_backend.user.entity.UserInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.List;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "password", source = "password", qualifiedByName = "encryptPassword")
    Users requestToUser(UserRequestDto userRequestDto);
    @Mapping(target = "authority", source = "users.authority") // authority 매핑 추가
    UserResponseDto userToResponse(Users users);

    @Mapping(source = "users", target = "userId")
    UserInfoDto userInfoToDto(UserInfo userInfo);

    @Mapping(target = "category", source = "categoryId")
    Gosu requestToGosu(GosuRequestDto gosuRequestDto);

    // Long을 Category로 변환하는 매핑 메서드
    default Category map(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        Category category = new Category();
        category.setCategoryId(categoryId);
        return category; // categoryId를 사용하여 Category 객체 생성
    }

    @Mapping(target = "categoryId", ignore = true)
    GosuInfoResponseDto gosuToResponseDto(Gosu gosu);


    default Long mapUsersToLong(Users users) { return users != null ? users.getId() : null; }

    List<UserResponseDto> toDtoList(List<Users> usersList);

    @Named("encryptPassword") // 2
    default String encryptPassword(String password) {
        return new BCryptPasswordEncoder().encode(password);
    }

}
