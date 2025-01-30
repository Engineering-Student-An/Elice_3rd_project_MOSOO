package com.team2.mosoo_backend.order.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderGosuListResponseDto {
    private List<OrderGosuResponseDto> orders;
}