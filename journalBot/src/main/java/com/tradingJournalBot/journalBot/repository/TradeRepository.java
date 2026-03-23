package com.tradingJournalBot.journalBot.repository;

import com.tradingJournalBot.journalBot.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByLocalDateTimeAfter(LocalDateTime date);
}
