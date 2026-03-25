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

                String[] parts = line.split("\\s+");

                if (parts.length < 2) {
                    throw new RuntimeException("Invalid format. Use: SYMBOL BUY/SELL");
                }

                // 🔥 SYMBOL (first word)
                dto.symbol = parts[0].toUpperCase();

                // 🔥 DIRECTION (second word)
                String dir = parts[1].toLowerCase();

                if (dir.equals("buy")) {
                    dto.direction = Direction.BUY;
                } else if (dir.equals("sell")) {
                    dto.direction = Direction.SELL;
                } else {
                    throw new RuntimeException("Direction must be BUY or SELL");
                }
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

                String value = line.split(":")[1].trim().toLowerCase().replaceAll("\\s|-", "");

                if (value.equals("london")) {
                    dto.session = Session.LONDON;

                } else if (value.equals("newyork") || value.equals("ny")) {
                    dto.session = Session.NEW_YORK;

                } else if (value.equals("asia")) {
                    dto.session = Session.ASIA;

                } else {
                    throw new RuntimeException("Invalid session (use London, New York, or Asia)");
                }
            }

            // STRATEGY
            else if (lower.startsWith("strategy")) {
                dto.strategy = normalizeStrategy(extractText(line, "Strategy"));
                dto.notes = message.trim();
            }

            // EMOTION
            else if (lower.contains("emotion")) {
                dto.confident = lower.contains("confident");
                dto.anxious = lower.contains("anxious");
                dto.fearful = lower.contains("fearful");
            }

            // OUTCOME
            else if (lower.startsWith("outcome")) {

                String[] parts = line.split(":");

                if (parts.length < 2) {
                    throw new RuntimeException("Outcome format is invalid. Use: Outcome: TP or SL");
                }

                String value = parts[1].trim().toLowerCase();

                if (value.equals("tp")) {
                    dto.win = true;
                } else if (value.equals("sl")) {
                    dto.win = false;
                } else {
                    throw new RuntimeException("Outcome must be TP or SL");
                }
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

    private String normalizeStrategy(String strategy) {

        if (strategy == null) return null;

        strategy = strategy.trim().toLowerCase();

        // capitalize first letter
        return strategy.substring(0, 1).toUpperCase() + strategy.substring(1);
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



