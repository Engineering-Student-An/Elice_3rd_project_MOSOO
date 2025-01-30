package com.team2.mosoo_backend.order.dto;
import com.team2.mosoo_backend.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderGosuResponseDto {
    private String userFullName;
    private String userEmail;
    private String workDate;
    private BigDecimal price;
    private OrderStatus orderStatus;
    private LocalDateTime paidAt;
}