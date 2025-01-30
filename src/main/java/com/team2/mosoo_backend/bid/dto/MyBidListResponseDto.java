package com.team2.mosoo_backend.bid.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MyBidListResponseDto {
    private List<MyBidResponseDto> bids;
}