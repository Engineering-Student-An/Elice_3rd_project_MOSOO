package com.team2.mosoo_backend.bid.mapper;


import com.team2.mosoo_backend.bid.dto.BidResponseDto;
import com.team2.mosoo_backend.bid.dto.CreateBidRequestDto;
import com.team2.mosoo_backend.bid.dto.MyBidResponseDto;
import com.team2.mosoo_backend.bid.entity.Bid;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BidMapper {

    BidResponseDto bidToBidResponseDto(Bid bid);

    MyBidResponseDto bidToMyBidResponseDto(Bid bid);

    @Mapping(target = "post", ignore = true)
    Bid createBidRequestDtoToBid(CreateBidRequestDto createBidRequestDto);
}
