package com.tradingJournalBot.journalBot.service;

import com.tradingJournalBot.journalBot.dto.ParsedTradeDTO;
import com.tradingJournalBot.journalBot.model.Trade;
import com.tradingJournalBot.journalBot.repository.TradeRepository;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Data
public class TradeService {

    private final TradeRepository repo;


    public Trade saveTrade(ParsedTradeDTO dto) {

        double rr = calculateRR(dto);
        double pnl = calculatePnL(dto);

        Trade trade = Trade.builder()
                .symbol(dto.symbol)
                .direction(dto.direction)
                .entryPrice(dto.entry)
                .stopLoss(dto.sl)
                .takeProfit(dto.tp)
                .win(dto.win)
                .riskReward(rr)
                .pnl(pnl)
                .session(dto.session)
                .strategy(dto.strategy)
                .confident(dto.confident)
                .anxious(dto.anxious)
                .fearful(dto.fearful)
                .notes(dto.notes)
                .localDateTime(LocalDateTime.now())
                .build();

        return repo.save(trade);
    }

    private double calculateRR(ParsedTradeDTO dto) {

        if (dto.direction == null) return 0;

        if (dto.direction.name().equals("BUY")) {
            return (dto.tp - dto.entry) / (dto.entry - dto.sl);
        } else {
            return (dto.entry - dto.tp) / (dto.sl - dto.entry);
        }
    }

    private double calculatePnL(ParsedTradeDTO dto) {

       double rr = calculateRR(dto);

        if (dto.win) {
            return rr;
        } else {
            return -1;
        }
    }
}






