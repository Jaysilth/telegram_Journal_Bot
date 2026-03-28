package com.tradingJournalBot.journalBot.controller;

import com.tradingJournalBot.journalBot.dto.ParsedTradeDTO;
import com.tradingJournalBot.journalBot.model.Trade;
import com.tradingJournalBot.journalBot.service.ParserService;
import com.tradingJournalBot.journalBot.service.TradeService;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/trade")
public class TelegramController {

    private final ParserService parser;
    private final TradeService service;

    public TelegramController(ParserService parser, TradeService service) {
        this.parser = parser;
        this.service = service;
    }

    @PostMapping
    public String createTrade(@RequestBody String message) {

        try {
            ParsedTradeDTO dto = parser.parse(message);
            Long testChatId = 1L;
            Trade trade = service.saveTrade(dto, testChatId);

            return "Trade saved successfully. ID: " + trade.getId();
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }

    }
    @GetMapping("/dashboard")
    public String dashboard() {

        Long testChatId = 1L;
        return service.getPerformanceDashboard(testChatId);
    }

}


