package com.tradingJournalBot.journalBot.repository;

import com.tradingJournalBot.journalBot.model.ResultType;
import com.tradingJournalBot.journalBot.model.Session;
import com.tradingJournalBot.journalBot.model.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByLocalDateTimeAfter(LocalDateTime date);

    List<Trade> findByChatIdOrderByLocalDateTimeAsc(Long chatId);

    List<Trade> findAllByOrderByLocalDateTimeAsc();

    List<Trade> findByChatIdAndResultTypeOrderByLocalDateTimeAsc(Long chatId, ResultType resultType);

    long countByChatIdAndResultType(Long chatId, ResultType resultType);

    List<Trade> findByChatIdAndLocalDateTimeBetween(
            Long chatId,
            LocalDateTime start,
            LocalDateTime end
    );
    long countByChatIdAndSessionAndResultType(
            Long chatId,
            Session session,
            ResultType resultType
    );

    long countByChatIdAndStrategyAndResultType(
            Long chatId,
            String strategy,
            ResultType resultType
    );

    List<Trade> findByChatId(Long chatId);
}
