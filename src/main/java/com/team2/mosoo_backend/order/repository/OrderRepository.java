package com.team2.mosoo_backend.order.repository;


import com.team2.mosoo_backend.order.entity.Order;
import com.team2.mosoo_backend.order.entity.OrderStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Order findByMerchantUid(String merchantUid);

    List<Order> findOrdersByChatRoomUserIdAndOrderStatus(Long userId, OrderStatus status);

}
