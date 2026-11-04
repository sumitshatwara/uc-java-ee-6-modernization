package io.daytrader.repository;

import io.daytrader.entity.QuoteDataBean;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface QuoteRepository extends JpaRepository<QuoteDataBean, String> {
    @Query("SELECT q FROM quoteejb q WHERE q.symbol LIKE 's:1__' ORDER BY q.change1 DESC")
    List<QuoteDataBean> findQuotesByChange();
}
