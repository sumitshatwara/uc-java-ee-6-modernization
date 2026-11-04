package io.daytrader.repository;

import io.daytrader.entity.AccountDataBean;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<AccountDataBean, Integer> {
}
