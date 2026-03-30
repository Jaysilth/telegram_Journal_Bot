package com.tradingJournalBot.journalBot.service;

import com.tradingJournalBot.journalBot.dto.ReportDTO;
import com.tradingJournalBot.journalBot.model.Trade;
import com.tradingJournalBot.journalBot.repository.TradeRepository;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Data
public class ReportService {

    private final TradeRepository tradeRepository;



    public ReportDTO generateReport() {

        List<Trade> trades = tradeRepository.findAllByOrderByLocalDateTimeAsc();

        ReportDTO report = new ReportDTO();

        if (trades.isEmpty()) return report;

        int wins = 0;
        int losses = 0;

        double totalRR = 0;
        double bestRR = Double.MIN_VALUE;
        double worstRR = Double.MAX_VALUE;

        int currentStreak = 0;
        int maxWinStreak = 0;
        int maxLossStreak = 0;

        int tempWinStreak = 0;
        int tempLossStreak = 0;

        for (Trade t : trades) {

            if (Boolean.TRUE.equals(t.getWin())) {
                wins++;
                tempWinStreak++;
                tempLossStreak = 0;

                if (tempWinStreak > maxWinStreak)
                    maxWinStreak = tempWinStreak;

                currentStreak = tempWinStreak;

            } else {
                losses++;
                tempLossStreak++;
                tempWinStreak = 0;

                if (tempLossStreak > maxLossStreak)
                    maxLossStreak = tempLossStreak;

                currentStreak = -tempLossStreak;
            }

            if (t.getRiskReward() != null) {
                totalRR += t.getRiskReward();

                if (t.getRiskReward() > bestRR) bestRR = t.getRiskReward();
                if (t.getRiskReward() < worstRR) worstRR = t.getRiskReward();
            }
        }

        report.totalTrades = trades.size();
        report.wins = wins;
        report.losses = losses;
        report.winRate = (wins * 100.0) / trades.size();

        report.avgRR = totalRR / trades.size();
        report.bestRR = bestRR;
        report.worstRR = worstRR;

        report.currentStreak = currentStreak;
        report.maxWinStreak = maxWinStreak;
        report.maxLossStreak = maxLossStreak;

        return report;
    }

}
