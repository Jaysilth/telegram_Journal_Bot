package com.tradingJournalBot.journalBot.controller;

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

            else if (text.equalsIgnoreCase("/behavior")) {
                String behavior = tradeService.getBehaviorAnalysis(chatId);
                telegramService.sendMessage(chatId, behavior);
                return;
            }

            System.out.println("message received " + text);

            var dto = parserService.parse(text);

            var savedTrade = tradeService.saveTrade(dto, chatId);

            String response = "✅ Trade Logged\n\n"
                    + "📊 " + savedTrade.getSymbol() + " " + savedTrade.getDirection() + "\n"
                    + "RR: " + String.format("%.2f", savedTrade.getRiskReward()) + "R\n"
                    + "Session: " + savedTrade.getSession() + "\n"
                    + "Strategy: " + savedTrade.getStrategy();

            telegramService.sendMessage(chatId, response);

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



