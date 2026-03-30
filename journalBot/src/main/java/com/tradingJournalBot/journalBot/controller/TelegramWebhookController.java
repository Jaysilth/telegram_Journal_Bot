package com.tradingJournalBot.journalBot.controller;

import com.tradingJournalBot.journalBot.model.ResultType;
import com.tradingJournalBot.journalBot.service.ParserService;
import com.tradingJournalBot.journalBot.service.TelegramService;
import com.tradingJournalBot.journalBot.service.TradeService;
import lombok.Data;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Data
@RestController
public class TelegramWebhookController {

    private final ParserService parserService;
    private final TradeService tradeService;
    private final TelegramService telegramService;


    @PostMapping("/webhook")
    public void handleUpdate(@RequestBody Map<String, Object> update) {

        System.out.println(" webhook hit");

        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) return;

        String text = (String) message.get("text");
        if (text == null) return;

        Map<String, Object> chat = (Map<String, Object>) message.get("chat");
        Long chatId = ((Number) chat.get("id")).longValue();

        try {

            // ✅ START COMMAND
            if (text.equalsIgnoreCase("/start")) {
                telegramService.sendMessage(chatId, "👋 Bot is live! Send your trade.");
                return;
            }

            // ✅ STEP 4: REPORT COMMAND
           else if (text.equalsIgnoreCase("/report")) {
                String report = tradeService.generateReport(chatId);
                telegramService.sendMessage(chatId, report);
                return;
            }


            else if (text.equalsIgnoreCase("/dashboard")) {
                String dashboard = tradeService.getPerformanceDashboard(chatId);
                telegramService.sendMessage(chatId, dashboard);
                return;
            }

            else if (text.equalsIgnoreCase("/coach")) {
                String feedback = tradeService.getCoachFeedback(chatId);
                telegramService.sendMessage(chatId, feedback);
                return;
            }

            else if (text.equalsIgnoreCase("/missed")) {

                long missed = tradeService.countMissedTrades(chatId);

                telegramService.sendMessage(chatId,
                        "👀 Missed Trades: " + missed + "\n" +
                                (missed > 0
                                        ? "⚠️ You are hesitating on valid setups."
                                        : "✅ No hesitation detected. Good execution.")
                );

                return;
            }

            else if (text.equalsIgnoreCase("/psychology")) {

                String behavior = tradeService.getBehaviorAnalysis(chatId);
                long missed = tradeService.countMissedTrades(chatId);

                String response = behavior + "\n\n👀 Missed Trades: " + missed;

                if (missed >= 3) {
                    response += "\n⚠️ Pattern: You're hesitating — likely fear-based.";
                }

                telegramService.sendMessage(chatId, response);

                return;
            }

            else if (text.equalsIgnoreCase("/behavior")) {
                String behavior = tradeService.getBehaviorAnalysis(chatId);
                telegramService.sendMessage(chatId, behavior);
                return;
            }

            System.out.println("message received " + text);

            var dto = parserService.parse(text);

            var savedTrade = tradeService.saveTrade(dto, chatId);

            String response;

            if (savedTrade.getResultType() == ResultType.MISSED_ENTRY) {

                response = "👀 Missed Trade Logged\n\n"
                        + "📊 " + savedTrade.getSymbol() + " " + savedTrade.getDirection() + "\n"
                        + "Session: " + savedTrade.getSession() + "\n"
                        + "Strategy: " + savedTrade.getStrategy() + "\n\n"
                        + "⚠️ You saw the setup but didn’t execute.";

            } else {

                response = "✅ Trade Logged\n\n"
                        + "📊 " + savedTrade.getSymbol() + " " + savedTrade.getDirection() + "\n"
                        + "RR: " + String.format("%.2f", savedTrade.getRiskReward()) + "R\n"
                        + "Session: " + savedTrade.getSession() + "\n"
                        + "Strategy: " + savedTrade.getStrategy();
            }

            telegramService.sendMessage(chatId, response);

            String warning = tradeService.getExecutionWarning(chatId);

            if (warning != null) {
                response += "\n\n" + warning;
            }

        } catch (NumberFormatException e) {

            telegramService.sendMessage(chatId, "❌ Invalid number format (check Entry, SL, TP)");

        } catch (IllegalArgumentException e) {

            telegramService.sendMessage(chatId, "❌ " + e.getMessage());

        } catch (RuntimeException e) {

            telegramService.sendMessage(chatId, "❌ " + e.getMessage());

        } catch (Exception e) {

            telegramService.sendMessage(chatId, "❌ Something went wrong. Check your format and try again.");
            e.printStackTrace();
        }

    }
}



