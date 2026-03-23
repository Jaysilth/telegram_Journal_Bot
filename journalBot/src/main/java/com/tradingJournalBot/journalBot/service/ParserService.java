package com.tradingJournalBot.journalBot.service;

import com.tradingJournalBot.journalBot.dto.ParsedTradeDTO;
import com.tradingJournalBot.journalBot.model.Direction;
import com.tradingJournalBot.journalBot.model.Session;
import org.springframework.stereotype.Service;

@Service
public class ParserService {

    public ParsedTradeDTO parse(String message) {

        if (message == null || message.isBlank()) {
            throw new RuntimeException("Message is empty");
        }

        ParsedTradeDTO dto = new ParsedTradeDTO();

        String[] lines = message.split("\\r?\\n");

        for (String rawLine : lines) {

            String line = rawLine.trim();

            if (line.isEmpty()) continue;

            String lower = line.toLowerCase();

            // SYMBOL + DIRECTION
            if (lower.contains("buy") || lower.contains("sell")) {

                if (lower.contains("btc")) dto.symbol = "BTCUSDT";
                else if (lower.contains("eth")) dto.symbol = "ETHUSDT";
                else throw new RuntimeException("Unsupported symbol");

                if (lower.contains("buy")) dto.direction = Direction.BUY;
                else dto.direction = Direction.SELL;
            }

            // ENTRY
            else if (lower.startsWith("entry")) {
                dto.entry = extractNumberSafe(line, "Entry");
            }

            // SL
            else if (lower.startsWith("sl")) {
                dto.sl = extractNumberSafe(line, "SL");
            }

            // TP
            else if (lower.startsWith("tp")) {
                dto.tp = extractNumberSafe(line, "TP");
            }

            // SESSION
            else if (lower.startsWith("session")) {

                if (lower.contains("london")) dto.session = Session.LONDON;
                else if (lower.contains("new york")) dto.session = Session.NEW_YORK;
                else if (lower.contains("asia")) dto.session = Session.ASIA;
                else throw new RuntimeException("Invalid session");
            }

            // STRATEGY
            else if (lower.startsWith("strategy")) {
                dto.strategy = extractText(line, "Strategy");
            }

            // EMOTION
            else if (lower.contains("emotion")) {
                dto.confident = lower.contains("confident");
                dto.anxious = lower.contains("anxious");
                dto.fearful = lower.contains("fearful");
            }

            // OUTCOME
             if (lower.contains("final outcome")) {

                 if (lower.contains("tp")) dto.win = true;
                 else if (lower.contains("sl")) dto.win = false;
             }
        }

        dto.notes = message;

        validate(dto);

        return dto;
    }

    // 🔥 SAFE NUMBER EXTRACTION
    private double extractNumberSafe(String line, String field) {
        try {
            String[] parts = line.split(":");
            if (parts.length < 2) {
                throw new RuntimeException(field + " format is invalid. Use: " + field + ": value");
            }
            return Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            throw new RuntimeException(field + " must be a valid number");
        }
    }

    // 🔥 SAFE TEXT EXTRACTION
    private String extractText(String line, String field) {
        String[] parts = line.split(":");
        if (parts.length < 2) {
            throw new RuntimeException(field + " format is invalid");
        }
        return parts[1].trim();
    }

    // 🔥 VALIDATION
    private void validate(ParsedTradeDTO dto) {

        if (dto.symbol == null)
            throw new RuntimeException("Symbol missing");

        if (dto.direction == null)
            throw new RuntimeException("Direction missing");

        if (dto.entry == 0)
            throw new RuntimeException("Entry missing");

        if (dto.sl == 0)
            throw new RuntimeException("Stop Loss missing");

        if (dto.tp == 0)
            throw new RuntimeException("Take Profit missing");
        if (dto.session == null)
            throw new RuntimeException("Session missing");

        if (dto.strategy == null || dto.strategy.isBlank())
            throw new RuntimeException("Strategy missing");

        // 🔥 VERY IMPORTANT
        if (dto.win == null) {
            throw new RuntimeException("Outcome missing");
        }
    }
}



