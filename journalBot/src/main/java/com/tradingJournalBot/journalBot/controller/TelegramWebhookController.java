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

        try {
            // 🔍 Navigate Telegram JSON
            Map<String, Object> message = (Map<String, Object>) update.get("message");

            if (message == null) return;

            String text = (String) message.get("text");

            if (text == null) return;

            if (text.equalsIgnoreCase("/start")){
                System.out.println("bot don start");
            }

            System.out.println("message received" + text);

            var dto = parserService.parse(text);

            var savedTrade = tradeService.saveTrade(dto);

// 🔥 GET CHAT ID
            Map<String, Object> chat = (Map<String, Object>) message.get("chat");
           // Integer chatId = (Integer) chat.get("id");
            Long chatId = ((Number) chat.get("id")).longValue();

// 🔥 BUILD RESPONSE
            String response = "✅ Trade Logged\n\n"
                    + "📊 " + savedTrade.getSymbol() + " " + savedTrade.getDirection() + "\n"
                    + "RR: " + String.format("%.2f", savedTrade.getRiskReward()) + "R\n"
                    + "Session: " + savedTrade.getSession() + "\n"
                    + "Strategy: " + savedTrade.getStrategy();

// 🔥 SEND MESSAGE
            telegramService.sendMessage(chatId, response);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}





