package io.daytrader.repository;

import io.daytrader.entity.QuoteDataBean;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuoteRepository extends JpaRepository<QuoteDataBean, String> {
}
