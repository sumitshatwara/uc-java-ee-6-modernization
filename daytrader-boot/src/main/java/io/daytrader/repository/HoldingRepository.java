package io.daytrader.repository;

import io.daytrader.entity.HoldingDataBean;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingRepository extends JpaRepository<HoldingDataBean, Integer> {
    List<HoldingDataBean> findByAccount_Profile_UserID(String userID);
}
