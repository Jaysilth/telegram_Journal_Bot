package com.tradingJournalBot.journalBot.service;

import com.tradingJournalBot.journalBot.dto.ParsedTradeDTO;
import com.tradingJournalBot.journalBot.model.ResultType;
import com.tradingJournalBot.journalBot.model.Session;
import com.tradingJournalBot.journalBot.model.Trade;
import com.tradingJournalBot.journalBot.repository.TradeRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Data
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository repo;

    private void validateTradeInput(ParsedTradeDTO dto) {

        if (dto.getSymbol() == null || dto.getSymbol().isBlank()) {
            throw new IllegalArgumentException("Symbol is required");
        }

        if (dto.getDirection() == null) {
            throw new IllegalArgumentException("Direction (buy/sell) is required");
        }

        if (dto.getEntry() == null || dto.getSl() == null || dto.getTp() == null) {
            throw new IllegalArgumentException("Entry, SL and TP must be provided");
        }

        double entry = dto.getEntry();
        double sl = dto.getSl();
        double tp = dto.getTp();

        if (entry <= 0 || sl <= 0 || tp <= 0) {
            throw new IllegalArgumentException("Prices must be greater than 0");
        }

        // 🔥 LOGICAL VALIDATION
        if ("buy".equalsIgnoreCase(String.valueOf(dto.getDirection()))) {

            if (sl >= entry) {
                throw new IllegalArgumentException("For BUY: SL must be below entry");
            }

            if (tp <= entry) {
                throw new IllegalArgumentException("For BUY: TP must be above entry");
            }

        } else if ("sell".equalsIgnoreCase(String.valueOf(dto.getDirection()))) {

            if (sl <= entry) {
                throw new IllegalArgumentException("For SELL: SL must be above entry");
            }

            if (tp >= entry) {
                throw new IllegalArgumentException("For SELL: TP must be below entry");
            }

        } else {
            throw new IllegalArgumentException("Direction must be BUY or SELL");
        }
    }

    // 🔥 SAFE WIN CHECK (GLOBAL FIX)
    private boolean isWin(Trade trade) {
        return trade.getResultType() == ResultType.TP;
    }

    // 🔥 SAFE LOSS CHECK
    private boolean isLoss(Trade trade) {
        return trade.getResultType() == ResultType.SL;
    }



    public Trade saveTrade(ParsedTradeDTO dto, Long chatId) {


        validateTradeInput(dto);
        double rr = 0;
        double pnl = 0;

        if (dto.resultType != ResultType.MISSED_ENTRY){
            rr= calculateRR(dto);
            pnl = calculatePnL(dto);
        }


        Trade trade = Trade.builder()
                .symbol(dto.symbol)
                .chatId(chatId)
                .direction(dto.direction)
                // 🔥 FIX (avoid null crash in DB logic)
                .entryPrice(dto.entry != null ? dto.entry : null)
                .stopLoss(dto.sl != null ? dto.sl : null)
                .takeProfit(dto.tp != null ? dto.tp : null)
                .resultType(dto.resultType)
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

        if (risk == 0){
            throw new IllegalArgumentException("risk cannot be zero");
        }

        return reward / risk;
    }

    private double calculatePnL(ParsedTradeDTO dto) {

        if (dto.resultType == ResultType.MISSED_ENTRY) {
            return 0;
        }

        double rr = calculateRR(dto);

       return dto.resultType == ResultType.TP ? rr : -1;
    }


    public long countMissedTrades(Long chatId) {
        return repo.countByChatIdAndResultType(chatId, ResultType.MISSED_ENTRY);
    }

    public String getExecutionWarning(Long chatId) {

        List<Trade> trades = repo.findByChatIdOrderByLocalDateTimeAsc(chatId);

        if (trades.size() < 2) return null;

        Trade last = trades.get(trades.size() - 1);
        Trade prev = trades.get(trades.size() - 2);

        // ⚠️ Missed after loss → hesitation
        if (prev.getResultType() == ResultType.SL &&
                last.getResultType() == ResultType.MISSED_ENTRY) {

            return "⚠️ You hesitated after a loss. This leads to revenge trading.";
        }

        // ⚠️ Missed streak
        long missedStreak = trades.stream()
                .skip(Math.max(0, trades.size() - 3))
                .filter(t -> t.getResultType() == ResultType.MISSED_ENTRY)
                .count();

        if (missedStreak >= 2) {
            return "⚠️ You're consistently hesitating. Confidence issue detected.";
        }

        return null;
    }

    public String detectOvertrading(Long chatId) {

        List<Trade> trades = repo.findByChatId(chatId);

        long now = System.currentTimeMillis();

        long recentTrades = trades.stream()
                .filter(t -> t.getCreatedAt() != null)
                .filter(t -> now - t.getCreatedAt().getTime() < (10 * 60 * 1000)) // last 10 mins
                .count();

        if (recentTrades >= 5) {
            return "⚠️ You're trading too frequently in a short time. Possible revenge trading.";
        }

        return null;
    }


    // ✅ STEP 5: REPORT GENERATOR
    public String generateReport(Long chatId) {

        List<Trade> trades = repo.findByChatIdOrderByLocalDateTimeAsc(chatId);

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

            if (isWin(trade)) {

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

        double winRate = total == 0 ? 0 : (wins * 100.0) / total;

        double avgRR = total == 0 ? 0 : totalRR/total;

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

            double sessionWinRate =sessionTotal == 0 ? 0 : (sessionWins * 100.0) / sessionTotal;


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

            double avgRRStrategy = strategyTotal == 0 ? 0 : totalRRStrategy / strategyTotal;

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

            double winRateEmotion = totalEmotionTrades == 0 ? 0 :(winsEmotion * 100.0) / totalEmotionTrades;

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

    public String getPerformanceDashboard(Long chatId) {

        List<Trade> trades = repo.findByChatIdOrderByLocalDateTimeAsc(chatId);

        if (trades.isEmpty()) {
            return "📭 No trades available for dashboard.";
        }

        int total = trades.size();

        long missed = trades.stream()
                .filter(t -> t.getResultType() == ResultType.MISSED_ENTRY)
                .count();

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
                + "Avg RR: " + String.format("%.2f", avgRR) + "R\n"
                + "Missed Trades: " + missed + "\n\n"

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

    public String getCoachFeedback(Long chatId) {


        List<Trade> trades = repo.findByChatIdOrderByLocalDateTimeAsc(chatId);
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

    public String getBehaviorAnalysis(Long chatId) {

        List<Trade> trades = repo.findByChatIdOrderByLocalDateTimeAsc(chatId);

        if (trades.isEmpty()) return "No trades yet.";

        StringBuilder result = new StringBuilder();
        result.append("🧠 Behavior Analysis\n\n");

        int revengeCount = 0;
        int overtradingFlag = 0;
        int maxLossStreak = 0;
        int currentLossStreak = 0;

        int fearfulLosses = 0;
        int anxiousLosses = 0;

        int missedTrades = 0;

        int revengeLossLoop = 0;
        int confidenceMistakes = 0;
        int fearMisses = 0;

        Map<String, Integer> strategyLossMap = new HashMap<>();
        Map<String, Integer> sessionLossMap = new HashMap<>();

        for (int i = 0; i < trades.size(); i++) {

            Trade current = trades.get(i);

            // 🔁 LOSS → REVENGE → LOSS LOOP
            if (i > 0) {
                Trade prev = trades.get(i - 1);

                if (isLoss(prev) && isLoss(current)) {
                    long minutes = Duration.between(
                            prev.getLocalDateTime(),
                            current.getLocalDateTime()
                    ).toMinutes();

                    if (minutes <= 15) {
                        revengeLossLoop++;
                    }
                }
            }

// 🎭 CONFIDENCE MISTAKE
            if (Boolean.TRUE.equals(current.getConfident()) && isLoss(current)) {
                confidenceMistakes++;
            }

// 😬 FEAR → MISSED
            if (current.getResultType() == ResultType.MISSED_ENTRY &&
                    Boolean.TRUE.equals(current.getFearful())) {
                fearMisses++;
            }

// 📉 STRATEGY TRACKING
            if (isLoss(current) && current.getStrategy() != null) {
                strategyLossMap.put(
                        current.getStrategy(),
                        strategyLossMap.getOrDefault(current.getStrategy(), 0) + 1
                );
            }

// 📉 SESSION TRACKING
            if (isLoss(current) && current.getSession() != null) {
                sessionLossMap.put(
                        current.getSession().name(),
                        sessionLossMap.getOrDefault(current.getSession().name(), 0) + 1
                );
            }

            // 🔥 MISSED COUNT
            if (current.getResultType() == ResultType.MISSED_ENTRY) {
                missedTrades++;
            }

            // 🔥 LOSS STREAK (SAFE)
            if (isLoss(current)) {
                currentLossStreak++;
                maxLossStreak = Math.max(maxLossStreak, currentLossStreak);
            } else if (isWin(current)) {
                currentLossStreak = 0;
            }

            // 🔥 EMOTIONAL LOSSES (SAFE)
            if (isLoss(current)) {
                if (Boolean.TRUE.equals(current.getFearful())) fearfulLosses++;
                if (Boolean.TRUE.equals(current.getAnxious())) anxiousLosses++;
            }

            // 🔥 REVENGE TRADING (SAFE)
            if (i > 0) {
                Trade prev = trades.get(i - 1);

                if (isLoss(prev)) {

                    long minutes = java.time.Duration.between(
                            prev.getLocalDateTime(),
                            current.getLocalDateTime()
                    ).toMinutes();

                    if (minutes > 0 && minutes <= 15) {
                        revengeCount++;
                    }
                }
            }
        }

        if (trades.size() >= 10) {
            overtradingFlag = 1;
        }

        // 📊 OUTPUT

        if (missedTrades > 0) {
            result.append("👀 Missed ").append(missedTrades)
                    .append(" valid trades — hesitation detected\n");
        }

        if (revengeCount > 0) {
            result.append("⚠️ Revenge trading detected (")
                    .append(revengeCount).append(" times)\n");
        }

        if (overtradingFlag == 1) {
            result.append("⚠️ You may be overtrading. Reduce frequency.\n");
        }

        if (maxLossStreak >= 2) {
            result.append("⚠️ Losing streak detected (Max: ")
                    .append(maxLossStreak).append(")\n");
        }

        if (fearfulLosses > 0) {
            result.append("😨 Fear impacted ")
                    .append(fearfulLosses).append(" losing trades\n");
        }

        if (anxiousLosses > 0) {
            result.append("😰 Anxiety impacted ")
                    .append(anxiousLosses).append(" losing trades\n");
        }

        if (revengeLossLoop > 0) {
            result.append("🔥 Emotional revenge loop detected (")
                    .append(revengeLossLoop)
                    .append(" times)\n");
        }

        if (confidenceMistakes > 0) {
            result.append("🎭 Overconfidence detected (")
                    .append(confidenceMistakes)
                    .append(" losses)\n");
        }

        if (fearMisses > 0) {
            result.append("😬 Fear caused ")
                    .append(fearMisses)
                    .append(" missed trades\n");
        }

        // 🔥 WORST STRATEGY
        strategyLossMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> result.append("📉 Strategy underperforming: ")
                        .append(entry.getKey())
                        .append("\n"));

// 🔥 WORST SESSION
        sessionLossMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .ifPresent(entry -> result.append("📉 Weak session: ")
                        .append(entry.getKey())
                        .append("\n"));


        if (result.toString().equals("🧠 Behavior Analysis\n\n")) {
            result.append("✅ No major behavioral issues detected. Stay disciplined.");
        }

        result.append("\n\n🔥 Control behavior → Control outcomes.");

        return result.toString();
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







