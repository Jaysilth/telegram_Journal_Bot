package com.tradingJournalBot.journalBot.service;

import com.tradingJournalBot.journalBot.model.Trade;
import com.tradingJournalBot.journalBot.repository.TradeRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
@Service
public class AnalyticsService {
    private final TradeRepository repo;
    private final TelegramService telegramService;


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

        Map<String, Integer> sessionWins = new HashMap<>();
        Map<String, Integer> sessionTotal = new HashMap<>();

        Map<String, Double> strategyRR = new HashMap<>();
        Map<String, Integer> strategyCount = new HashMap<>();



            for (Trade trade : trades) {

                // ✅ EXISTING LOGIC
                if (Boolean.TRUE.equals(trade.getWin())) {
                    wins++;
                }
                totalRR += trade.getPnl();



                // 🔥 SESSION TRACKING
                String session = trade.getSession().name();

                sessionTotal.put(session, sessionTotal.getOrDefault(session, 0) + 1);

                if (Boolean.TRUE.equals(trade.getWin())) {
                    sessionWins.put(session, sessionWins.getOrDefault(session, 0) + 1);
                }

                // 🔥 STRATEGY TRACKING
                String strategy = trade.getStrategy(); // assuming String

                strategyRR.put(strategy,
                        strategyRR.getOrDefault(strategy, 0.0) + trade.getPnl());

                strategyCount.put(strategy,
                        strategyCount.getOrDefault(strategy, 0) + 1);
            }


        double winRate = (wins * 100.0) / total;
        double expectancy = totalRR / total;

      //  BEST & WORST SESSION
        String bestSession = null;
        double bestSessionWR = 0;

        String worstSession = null;
        double worstSessionWR = 100;

        for (String session : sessionTotal.keySet()) {

            int totalTrades = sessionTotal.get(session);
            int winCount = sessionWins.getOrDefault(session, 0);

            double wr = (winCount * 100.0) / totalTrades;

            if (wr > bestSessionWR) {
                bestSessionWR = wr;
                bestSession = session;
            }

            if (wr < worstSessionWR) {
                worstSessionWR = wr;
                worstSession = session;
            }
        }

// 🔍 BEST & WORST STRATEGY
        String bestStrategy = null;
        double bestStrategyExp = Double.MIN_VALUE;

        String worstStrategy = null;
        double worstStrategyExp = Double.MAX_VALUE;

        for (String strategy : strategyCount.keySet()) {

            double totalStrategyRR = strategyRR.get(strategy);
            int count = strategyCount.get(strategy);

            expectancy = totalStrategyRR / count;

            if (expectancy > bestStrategyExp) {
                bestStrategyExp = expectancy;
                bestStrategy = strategy;
            }

            if (expectancy < worstStrategyExp) {
                worstStrategyExp = expectancy;
                worstStrategy = strategy;
            }
        }


        StringBuilder report = new StringBuilder();

        report.append("📊WEEKLY TRADING REPORT\n\n");
        report.append("💯Total Trades: ").append(total).append("\n");
        report.append("🏆Win Rate: ").append(String.format("%.2f",winRate)).append("%\n");
        report.append("🔬Expectancy: ").append(String.format("%.2f",expectancy)).append("R\n\n");

        report.append("💡SESSION PERFORMANCE\n");

        for (String session : sessionTotal.keySet()) {

            int totalTrades = sessionTotal.get(session);
            int winCount = sessionWins.getOrDefault(session, 0);

            double wr = (winCount * 100.0) / totalTrades;

           report.append(session)
                   .append(": ")
                   .append(String.format("%.2f",wr))
                   .append("% (")
                   .append(totalTrades)
                   .append(" trades)\n");
        }

        report.append("\n 🏹STRATEGY PERFORMANCE\n");

        for (String strategy : strategyCount.keySet()) {

            double totalStrategyRR = strategyRR.get(strategy);
            int count = strategyCount.get(strategy);

            double exp = totalStrategyRR / count;

            report.append(strategy)
                    .append(": ")
                    .append(String.format("%.2f", exp))
                    .append("R (")
                    .append(String.format("%.2f", totalStrategyRR))
                    .append("R total, ")
                    .append(count)
                    .append(" trades)\n");


        }



        report.append("\n🗿 AI INSIGHTS\n");

        report.append("🔥Best Session: ")
                .append(bestSession)
                .append(" (")
                .append(String.format("%.2f", bestSessionWR))
                .append("%)\n");

        report.append("⚠️Worst Session: ")
                .append(worstSession)
                .append(" (")
                .append(String.format("%.2f", worstSessionWR))
                .append("%)\n");

        report.append("✅Best Strategy: ").append(bestStrategy).append("\n");
        report.append("⚠️Worst Strategy: ").append(worstStrategy).append("\n");

        if (expectancy > 0) {
            report.append("✅ Strategy has an edge");
        } else {
            report.append("❌ No edge detected");
        }

       // telegramService.sendMessage(report.toString());

        Long myChatId = 8295076839L;
        telegramService.sendMessage(myChatId, report.toString());
    }
}





