package io.daytrader.repository;

import io.daytrader.entity.AccountProfileDataBean;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountProfileRepository extends JpaRepository<AccountProfileDataBean, String> {
}
