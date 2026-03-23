package com.tradingJournalBot.journalBot.service;

import com.tradingJournalBot.journalBot.model.Trade;
import com.tradingJournalBot.journalBot.repository.TradeRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AnalyticsService {
    private final TradeRepository repo;

    public AnalyticsService(TradeRepository repo) {
        this.repo = repo;
    }

    // 🔥 RUNS EVERY WEEK (MONDAY 00:00)
    @Scheduled(cron =  "0 */1 * * * *")// "0 0 0 * * MON")
    public void weeklyAnalysis() {

        LocalDateTime oneWeekAgo = LocalDateTime.now().minusDays(7);

        List<Trade> trades = repo.findByLocalDateTimeAfter(oneWeekAgo);

        if (trades.isEmpty()) {
            System.out.println("No trades this week.");
            return;
        }

        int total = trades.size();
        int wins = 0;
        double totalRR = 0;

        for (Trade trade : trades) {
            if (Boolean.TRUE.equals(trade.getWin())) {
                wins++;
            }
            totalRR += trade.getPnl(); // already in R format
        }

        double winRate = (wins * 100.0) / total;
        double expectancy = totalRR / total;

        System.out.println("===== WEEKLY ANALYTICS =====");
        System.out.println("Total Trades: " + total);
        System.out.println("Win Rate: " + winRate + "%");
        System.out.println("Expectancy: " + expectancy + "R");

        if (expectancy > 0) {
            System.out.println("✅ Strategy has an edge");
        } else {
            System.out.println("❌ No edge detected");
        }
    }
}





