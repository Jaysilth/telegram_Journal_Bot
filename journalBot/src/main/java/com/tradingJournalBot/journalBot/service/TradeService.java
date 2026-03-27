package com.tradingJournalBot.journalBot.service;

import com.tradingJournalBot.journalBot.dto.ParsedTradeDTO;
import com.tradingJournalBot.journalBot.model.Session;
import com.tradingJournalBot.journalBot.model.Trade;
import com.tradingJournalBot.journalBot.repository.TradeRepository;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

        System.out.println("rr calculated: " + rr);

        Trade savedTrade = repo.save(trade);

// 🔥 APPLY NEW SYSTEMS
        String quality = calculateTradeQuality(savedTrade);
        List<String> warnings = generateWarnings(savedTrade);

// 🔥 LOG OUTPUT (SAFE — DOES NOT BREAK API)
        System.out.println("\n📊 Trade Logged Successfully");
        System.out.println("Trade Quality: " + quality);

        if (!warnings.isEmpty()) {
            System.out.println("⚠️ Warnings:");
            for (String w : warnings) {
                System.out.println("- " + w);
            }
        }

        return savedTrade;

    }


    // ✅ STEP 1 FIXED RR (SAFE + VALIDATED)
    private double calculateRR(ParsedTradeDTO dto) {

        if (dto.direction == null)
            throw new IllegalArgumentException("Direction missing");

        if (dto.entry == null || dto.sl == null || dto.tp == null)
        throw new IllegalArgumentException("Entry, SL and TP must be provided");

        if (dto.entry <= 0 || dto.sl <= 0 || dto.tp <= 0)
        throw new IllegalArgumentException("Entry, SL, TP must be positive numbers");

        double risk;
        double reward;

        if (dto.direction.name().equals("BUY")) {

            if (dto.sl >= dto.entry)
                throw new IllegalArgumentException("For BUY: SL must be below Entry");

            if (dto.tp <= dto.entry)
                throw new IllegalArgumentException("For BUY: TP must be above Entry");

            risk = dto.entry - dto.sl;
            reward = dto.tp - dto.entry;

        } else {

            if (dto.sl <= dto.entry)
                throw new IllegalArgumentException("For SELL: SL must be above Entry");

            if (dto.tp >= dto.entry)
                throw new IllegalArgumentException("For SELL: TP must be below Entry");

            risk = dto.sl - dto.entry;
            reward = dto.entry - dto.tp;
        }

        return reward / risk;
    }

    private double calculatePnL(ParsedTradeDTO dto) {

        double rr = calculateRR(dto);

        if (dto.win) {
            return rr;
        } else {
            return -1;
        }
    }

    // ✅ STEP 5: REPORT GENERATOR
    public String generateReport() {

        List<Trade> trades = repo.findAllByOrderByLocalDateTimeAsc();

        if (trades.isEmpty()) {
            return "📭 No trades logged yet.";
        }

        int total = trades.size();

        long wins = trades.stream()
                .filter(trade -> Boolean.TRUE.equals(trade.getWin()))
                .count();

        long losses = total - wins;

// 🔥 STREAK LOGIC
        int currentWinStreak = 0;
        int currentLossStreak = 0;
        int maxWinStreak = 0;
        int maxLossStreak = 0;

        for (Trade trade : trades) {

            if (Boolean.TRUE.equals(trade.getWin())) {

                currentWinStreak++;
                currentLossStreak = 0;

                if (currentWinStreak > maxWinStreak) {
                    maxWinStreak = currentWinStreak;
                }

            } else {

                currentLossStreak++;
                currentWinStreak = 0;

                if (currentLossStreak > maxLossStreak) {
                    maxLossStreak = currentLossStreak;
                }
            }
        }

        double totalRR = trades.stream()
                .mapToDouble(Trade::getRiskReward)
                .sum();

        double totalPnL = trades.stream()
                .mapToDouble(Trade::getPnl)
                .sum();

        double winRate = (wins * 100.0) / total;

        double avgRR = totalRR/total;

        // 🔥 SESSION BREAKDOWN
        Map<Session, List<Trade>> sessionMap = trades.stream()
                .filter(t -> t.getSession() != null)
                .collect(Collectors.groupingBy(Trade::getSession));

        StringBuilder sessionReport = new StringBuilder("\n📊 Session Performance\n\n");

        Session bestSession = null;
        Session worstSession = null;
        double bestRate = Double.MIN_VALUE;
        double worstRate = Double.MAX_VALUE;

        for (Session session : sessionMap.keySet()) {

            List<Trade> sessionTrades = sessionMap.get(session);

            int sessionTotal = sessionTrades.size();

            long sessionWins = sessionTrades.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getWin()))
                    .count();

            double sessionWinRate = (sessionWins * 100.0) / sessionTotal;


            sessionReport.append(session)
                    .append(" → Trades: ").append(sessionTotal)
                    .append(" | Win Rate: ")
                    .append(String.format("%.2f", sessionWinRate))
                    .append("%\n");

            if (sessionWinRate >= bestRate){
                bestRate = sessionWinRate;
                bestSession = session;
            }
            if (sessionWinRate <= worstRate){
                worstRate = sessionWinRate;
                worstSession = session;
            }
        }

        sessionReport.append("\n🏆 Best Session: ")
                .append(bestSession)
                .append(" (")
                .append(String.format("%.2f", bestRate))
                .append("%)\n");

        sessionReport.append("⚠️ Worst Session: ")
                .append(worstSession)
                .append(" (")
                .append(String.format("%.2f", worstRate))
                .append("%)\n");

        // 🔥 STRATEGY BREAKDOWN
        Map<String, List<Trade>> strategyMap = trades.stream()
                .filter(t -> t.getStrategy() != null && !t.getStrategy().isEmpty())
                .collect(Collectors.groupingBy(Trade::getStrategy));

        StringBuilder strategyReport = new StringBuilder("\n📊 Strategy Performance\n\n");

        String bestStrategy = null;
        String worstStrategy = null;

        double bestPnL = Double.MIN_VALUE;
        double worstPnL = Double.MAX_VALUE;

        for (String strategy : strategyMap.keySet()) {

            List<Trade> strategyTrades = strategyMap.get(strategy);

            int strategyTotal = strategyTrades.size();

            long strategyWins = strategyTrades.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getWin()))
                    .count();

            double winRateStrategy = (strategyWins * 100.0) / strategyTotal;

            double totalRRStrategy = strategyTrades.stream()
                    .mapToDouble(Trade::getRiskReward)
                    .sum();

            double pnlStrategy = strategyTrades.stream()
                    .mapToDouble(Trade::getPnl)
                    .sum();

            double avgRRStrategy = totalRRStrategy / strategyTotal;

            strategyReport.append(strategy)
                    .append(" → Trades: ").append(strategyTotal)
                    .append(" | Win Rate: ").append(String.format("%.2f", winRateStrategy)).append("%")
                    .append(" | Avg RR: ").append(String.format("%.2f", avgRRStrategy)).append("R")
                    .append(" | PnL: ").append(String.format("%.2f", pnlStrategy)).append("R\n");

            // 🔥 BEST / WORST BASED ON PnL
            if (pnlStrategy >= bestPnL) {
                bestPnL = pnlStrategy;
                bestStrategy = strategy;
            }

            if (pnlStrategy <= worstPnL) {
                worstPnL = pnlStrategy;
                worstStrategy = strategy;
            }
        }

// 🔥 SUMMARY
        strategyReport.append("\n🏆 Best Strategy: ")
                .append(bestStrategy)
                .append("\n");

        strategyReport.append("⚠️ Worst Strategy: ")
                .append(worstStrategy)
                .append("\n");

        // 🧠 EMOTIONAL ANALYSIS
        Map<String, List<Trade>> emotionMap = new HashMap<>();

        for (Trade trade : trades) {

            if (Boolean.TRUE.equals(trade.getConfident())) {
                emotionMap.computeIfAbsent("Confident", k -> new ArrayList<>()).add(trade);
            }

            if (Boolean.TRUE.equals(trade.getAnxious())) {
                emotionMap.computeIfAbsent("Anxious", k -> new ArrayList<>()).add(trade);
            }

            if (Boolean.TRUE.equals(trade.getFearful())) {
                emotionMap.computeIfAbsent("Fearful", k -> new ArrayList<>()).add(trade);
            }
        }

        StringBuilder emotionReport = new StringBuilder("\n🧠 Emotional Analysis\n\n");

        String bestEmotion = null;
        String worstEmotion = null;

        double bestEmotionPnL = Double.MIN_VALUE;
        double worstEmotionPnL = Double.MAX_VALUE;

        for (String emotion : emotionMap.keySet()) {

            List<Trade> emotionTrades = emotionMap.get(emotion);

            int totalEmotionTrades = emotionTrades.size();

            long winsEmotion = emotionTrades.stream()
                    .filter(t -> Boolean.TRUE.equals(t.getWin()))
                    .count();

            double winRateEmotion = (winsEmotion * 100.0) / totalEmotionTrades;

            double pnlEmotion = emotionTrades.stream()
                    .mapToDouble(Trade::getPnl)
                    .sum();

            emotionReport.append(emotion)
                    .append(" → Trades: ").append(totalEmotionTrades)
                    .append(" | Win Rate: ").append(String.format("%.2f", winRateEmotion)).append("%")
                    .append(" | PnL: ").append(String.format("%.2f", pnlEmotion)).append("R\n");

            // 🔥 BEST / WORST BASED ON PnL
            if (pnlEmotion >= bestEmotionPnL) {
                bestEmotionPnL = pnlEmotion;
                bestEmotion = emotion;
            }

            if (pnlEmotion <= worstEmotionPnL) {
                worstEmotionPnL = pnlEmotion;
                worstEmotion = emotion;
            }
        }

// 🔥 SUMMARY
        emotionReport.append("\n🏆 Best Emotion: ")
                .append(bestEmotion)
                .append("\n");

        emotionReport.append("⚠️ Worst Emotion: ")
                .append(worstEmotion)
                .append("\n");



        return "📊 Trading Report\n\n"
                + "Total Trades: " + total + "\n"
                + "Wins: " + wins + "\n"
                + "Losses: " + losses + "\n"
                + "Win Rate: " + String.format("%.2f", winRate) + "%\n\n"

                + "🔥 Max Win Streak: " + maxWinStreak + "\n"
                + "🥶 Max Loss Streak: " + maxLossStreak + "\n\n"

                + "Avg RR: " + String.format("%.2f", avgRR) + "R\n"
                + "Total RR: " + String.format("%.2f", totalRR) + "R\n"
                + "Net PnL: " + String.format("%.2f", totalPnL) + "R\n"
                + sessionReport.toString()
                + strategyReport.toString()
                + emotionReport.toString();


    }

    public String getPerformanceDashboard() {

        List<Trade> trades = repo.findAllByOrderByLocalDateTimeAsc();

        if (trades.isEmpty()) {
            return "📭 No trades available for dashboard.";
        }

        int total = trades.size();

        long wins = trades.stream()
                .filter(t -> Boolean.TRUE.equals(t.getWin()))
                .count();

        long losses = total - wins;

        double pnl = trades.stream()
                .mapToDouble(Trade::getPnl)
                .sum();

        double totalRR = trades.stream()
                .mapToDouble(Trade::getRiskReward)
                .sum();

        double avgRR = totalRR / total;

        double winRate = (wins * 100.0) / total;

        // 🔥 QUALITY BREAKDOWN
        int aPlus = 0, a = 0, b = 0, c = 0;

        for (Trade trade : trades) {

            String quality = calculateTradeQuality(trade);

            if (quality.startsWith("A+")) aPlus++;
            else if (quality.startsWith("A")) a++;
            else if (quality.startsWith("B")) b++;
            else c++;
        }

        // 🧠 EMOTIONAL EDGE
        Map<String, Double> emotionPnL = new HashMap<>();

        for (Trade trade : trades) {

            if (Boolean.TRUE.equals(trade.getConfident())) {
                emotionPnL.merge("Confident", trade.getPnl(), Double::sum);
            }

            if (Boolean.TRUE.equals(trade.getAnxious())) {
                emotionPnL.merge("Anxious", trade.getPnl(), Double::sum);
            }

            if (Boolean.TRUE.equals(trade.getFearful())) {
                emotionPnL.merge("Fearful", trade.getPnl(), Double::sum);
            }
        }

        String bestEmotion = null;
        String worstEmotion = null;

        double best = Double.MIN_VALUE;
        double worst = Double.MAX_VALUE;

        for (String e : emotionPnL.keySet()) {

            double val = emotionPnL.get(e);

            if (val >= best) {
                best = val;
                bestEmotion = e;
            }

            if (val <= worst) {
                worst = val;
                worstEmotion = e;
            }
        }

        // ⚠️ RISK DETECTION
        String riskWarning = "";

        if (total >= 5 && winRate < 50) {
            riskWarning = "⚠️ You're forcing trades. Reduce frequency.\n";
        }

        if (losses >= wins && losses >= 3) {
            riskWarning += "⚠️ Losses dominating. Review strategy.\n";
        }

        // 🎯 SMART FEEDBACK
        String feedback;

        if (winRate >= 60 && pnl > 0) {
            feedback = "🔥 You're trading like a sniper. Stay disciplined.";
        } else if (winRate < 50 && pnl < 0) {
            feedback = "🚨 You're in drawdown. Reduce risk and reset.";
        } else {
            feedback = "⚖️ You're inconsistent. Focus on A+ setups only.";
        }

        return "📊 NEXT LEVEL DASHBOARD\n\n"

                + "📈 Performance\n"
                + "Trades: " + total + "\n"
                + "Win Rate: " + String.format("%.2f", winRate) + "%\n"
                + "Net PnL: " + String.format("%.2f", pnl) + "R\n"
                + "Avg RR: " + String.format("%.2f", avgRR) + "R\n\n"

                + "🎯 Trade Quality\n"
                + "A+: " + aPlus + " | A: " + a + " | B: " + b + " | C: " + c + "\n\n"

                + "🧠 Emotional Edge\n"
                + "Best: " + bestEmotion + "\n"
                + "Worst: " + worstEmotion + "\n\n"

                + "⚠️ Risk Analysis\n"
                + (riskWarning.isEmpty() ? "✅ No major risk detected\n\n" : riskWarning + "\n")

                + "🎯 Coach Insight\n"
                + feedback + "\n";
    }

    public String getCoachFeedback() {


        List<Trade> trades = repo.findAll();
        int totalTrades = trades.size();

        double winRate = calculateWinRate(trades);
        double avgRR = calculateAverageRR(trades);
        double netPnL = calculateTotalPnL(trades);

        StringBuilder coach = new StringBuilder();

        coach.append("🧠 AI Trading Coach\n\n");

        // 🎯 PERFORMANCE ANALYSIS
        if (winRate < 50) {
            coach.append("⚠️ Your win rate is below 50%. You are likely forcing trades.\n");
        } else {
            coach.append("✅ Good win rate. Focus on consistency.\n");
        }

        if (avgRR < 1.5) {
            coach.append("⚠️ Your RR is too low. You're cutting winners early.\n");
        } else {
            coach.append("✅ RR is solid. Let winners run.\n");
        }

        // 💰 PNL REALITY CHECK
        if (netPnL < 0) {
            coach.append("❌ You're losing overall. Reduce risk and trade less.\n");
        } else {
            coach.append("💰 You're profitable. Protect your edge.\n");
        }

        // 🧠 BEHAVIOR CHECK
        if (totalTrades > 10) {
            coach.append("⚠️ Possible overtrading detected. Quality > Quantity.\n");
        }

        // 🔥 SESSION INSIGHT
        String bestSession = getBestSession(trades);
        String worstSession = getWorstSession(trades);

        coach.append("\n📊 Trade more in ").append(bestSession);
        coach.append(" and avoid ").append(worstSession).append("\n");

        // 🎯 STRATEGY INSIGHT
        String bestStrategy = getBestStrategy(trades);
        String worstStrategy = getWorstStrategy(trades);

        coach.append("🏆 Focus on ").append(bestStrategy);
        coach.append(" and fix or drop ").append(worstStrategy).append("\n");

        coach.append("\n🔥 Discipline > Strategy. Fix behavior, results will follow.");

        return coach.toString();
    }


    // 🔥 TRADE QUALITY SYSTEM
    private String calculateTradeQuality(Trade trade) {

        int score = 0;

        if ("Breakout".equalsIgnoreCase(trade.getStrategy())) score++;

        if (trade.getSession() != null && trade.getSession().name().equalsIgnoreCase("ASIA")) score++;

        if (Boolean.TRUE.equals(trade.getConfident())) score++;

        if (score == 3) return "A+ 🟢 (Sniper Trade)";
        if (score == 2) return "A 🟢 (High Quality)";
        if (score == 1) return "B 🟡 (Average)";
        return "C 🔴 (Low Quality / Risky)";
    }


    // 🚨 WARNING SYSTEM
    private List<String> generateWarnings(Trade trade) {

        List<String> warnings = new ArrayList<>();

        if (!Boolean.TRUE.equals(trade.getConfident())) {
            warnings.add("⚠️ You are not confident — historically unprofitable");
        }

        if (trade.getSession() != null &&
                trade.getSession().name().equalsIgnoreCase("NEW_YORK")) {
            warnings.add("⚠️ New York session is your worst session");
        }

        if ("Reversal".equalsIgnoreCase(trade.getStrategy())) {
            warnings.add("⚠️ Reversal strategy is underperforming");
        }

        if (Boolean.TRUE.equals(trade.getAnxious())) {
            warnings.add("🚨 Trading while anxious = LOSS pattern detected");
        }

        if (Boolean.TRUE.equals(trade.getFearful())) {
            warnings.add("🚨 Trading while fearful = LOSS pattern detected");
        }

        return warnings;
    }

    // ✅ WIN RATE
    private double calculateWinRate(List<Trade> trades) {

        if (trades.isEmpty()) return 0;

        long wins = trades.stream()
                .filter(t -> Boolean.TRUE.equals(t.getWin()))
                .count();

        return (wins * 100.0) / trades.size();
    }


    // ✅ AVG RR
    private double calculateAverageRR(List<Trade> trades) {

        if (trades.isEmpty()) return 0;

        double totalRR = trades.stream()
                .mapToDouble(Trade::getRiskReward)
                .sum();

        return totalRR / trades.size();
    }


    // ✅ TOTAL PNL (RR)
    private double calculateTotalPnL(List<Trade> trades) {

        return trades.stream()
                .mapToDouble(Trade::getPnl)
                .sum();
    }


    // ✅ BEST SESSION
    private String getBestSession(List<Trade> trades) {

        Map<Session, List<Trade>> map = trades.stream()
                .filter(t -> t.getSession() != null)
                .collect(Collectors.groupingBy(Trade::getSession));

        String best = null;
        double bestRate = Double.MIN_VALUE;

        for (Session s : map.keySet()) {

            List<Trade> tList = map.get(s);

            double winRate = calculateWinRate(tList);

            if (winRate >= bestRate) {
                bestRate = winRate;
                best = s.name();
            }
        }

        return best;
    }


    // ✅ WORST SESSION
    private String getWorstSession(List<Trade> trades) {

        Map<Session, List<Trade>> map = trades.stream()
                .filter(t -> t.getSession() != null)
                .collect(Collectors.groupingBy(Trade::getSession));

        String worst = null;
        double worstRate = Double.MAX_VALUE;

        for (Session s : map.keySet()) {

            List<Trade> tList = map.get(s);

            double winRate = calculateWinRate(tList);

            if (winRate <= worstRate) {
                worstRate = winRate;
                worst = s.name();
            }
        }

        return worst;
    }


    // ✅ BEST STRATEGY
    private String getBestStrategy(List<Trade> trades) {

        Map<String, List<Trade>> map = trades.stream()
                .filter(t -> t.getStrategy() != null)
                .collect(Collectors.groupingBy(Trade::getStrategy));

        String best = null;
        double bestPnL = Double.MIN_VALUE;

        for (String s : map.keySet()) {

            double pnl = map.get(s).stream()
                    .mapToDouble(Trade::getPnl)
                    .sum();

            if (pnl >= bestPnL) {
                bestPnL = pnl;
                best = s;
            }
        }

        return best;
    }


    // ✅ WORST STRATEGY
    private String getWorstStrategy(List<Trade> trades) {

        Map<String, List<Trade>> map = trades.stream()
                .filter(t -> t.getStrategy() != null)
                .collect(Collectors.groupingBy(Trade::getStrategy));

        String worst = null;
        double worstPnL = Double.MAX_VALUE;

        for (String s : map.keySet()) {

            double pnl = map.get(s).stream()
                    .mapToDouble(Trade::getPnl)
                    .sum();

            if (pnl <= worstPnL) {
                worstPnL = pnl;
                worst = s;
            }
        }

        return worst;
    }
}







