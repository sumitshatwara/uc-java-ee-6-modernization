package io.daytrader.repository;

import io.daytrader.entity.OrderDataBean;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderDataBean, Integer> {
    List<OrderDataBean> findByAccount_Profile_UserIDOrderByOpenDateDesc(String userID);
    List<OrderDataBean> findByAccount_Profile_UserIDAndOrderStatusOrderByOpenDateDesc(String userID, String orderStatus);
}
