package com.tradingJournalBot.journalBot.service;

import com.tradingJournalBot.journalBot.dto.ParsedTradeDTO;
import com.tradingJournalBot.journalBot.model.Direction;
import com.tradingJournalBot.journalBot.model.Session;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ParserService {


    public ParsedTradeDTO parse(String message) {

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Message is empty");
        }

        if (!message.toLowerCase().contains("entry")) {
            throw new IllegalArgumentException("❌ Invalid format");
        }

        ParsedTradeDTO dto = new ParsedTradeDTO();
        List<String> errors = new ArrayList<>();

        String[] lines = message.split("\\r?\\n");

        String pendingSymbol = null;

        for (String rawLine : lines) {

            String line = rawLine.trim();

            if (line.isEmpty()) continue;

            String lower = line.toLowerCase();

            // ✅ SYMBOL + DIRECTION (supports split lines)
            if (lower.contains("buy") || lower.contains("sell")) {

                String[] parts = line.split("\\s+");

                if (parts.length >= 2) {

                    dto.symbol = parts[0].toUpperCase();

                    String dir = parts[1].toLowerCase();

                    if (dir.equals("buy")) {
                        dto.direction = Direction.BUY;
                    } else if (dir.equals("sell")) {
                        dto.direction = Direction.SELL;
                    } else {
                        errors.add("Direction must be BUY or SELL");
                    }

                } else if (pendingSymbol != null) {

                    dto.symbol = pendingSymbol.toUpperCase();

                    if (lower.equals("buy")) {
                        dto.direction = Direction.BUY;
                    } else if (lower.equals("sell")) {
                        dto.direction = Direction.SELL;
                    } else {
                        errors.add("Direction must be BUY or SELL");
                    }

                } else {
                    errors.add("Invalid format. Use: SYMBOL BUY/SELL");
                }
            }

            // ✅ SYMBOL ONLY LINE
            else if (!line.contains(":") && dto.symbol == null) {
                pendingSymbol = line;
            }

            // ENTRY
            else if (lower.startsWith("entry")) {
                dto.entry = extractNumberSafe(line, "Entry", errors);
            }

            // SL
            else if (lower.startsWith("sl")) {
                dto.sl = extractNumberSafe(line, "SL", errors);
            }

            // TP
            else if (lower.startsWith("tp")) {
                dto.tp = extractNumberSafe(line, "TP", errors);
            }

            // SESSION
            else if (lower.startsWith("session")) {

                String[] parts = line.split(":");
                if (parts.length < 2) {
                    errors.add("Session format invalid. Use: Session: London");
                    continue;
                }

                String value = parts[1].trim().toLowerCase().replaceAll("\\s|-", "");

                if (value.equals("london")) {
                    dto.session = Session.LONDON;

                } else if (value.equals("newyork") || value.equals("ny")) {
                    dto.session = Session.NEW_YORK;

                } else if (value.equals("asia")) {
                    dto.session = Session.ASIA;

                } else {
                    errors.add("Invalid session (use London, New York, or Asia)");
                }
            }

            // STRATEGY
            else if (lower.startsWith("strategy")) {
                dto.strategy = normalizeStrategy(extractText(line, "Strategy", errors));
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
                    errors.add("Outcome format is invalid. Use: Outcome: TP or SL");
                    continue;
                }

                String value = parts[1].trim().toLowerCase();

                if (value.equals("tp")) {
                    dto.win = true;
                } else if (value.equals("sl")) {
                    dto.win = false;
                } else {
                    errors.add("Outcome must be TP or SL");
                }
            }
        }

        // ✅ NEGATIVE VALUE CHECKS (NULL SAFE)
        if (dto.entry != null && dto.entry <= 0) {
            errors.add("Entry must be greater than 0");
        }

        if (dto.sl != null && dto.sl <= 0) {
            errors.add("SL must be greater than 0");
        }

        if (dto.tp != null && dto.tp <= 0) {
            errors.add("TP must be greater than 0");
        }

        dto.notes = message;

        // ✅ VALIDATION
        validate(dto, errors);

        // ✅ FINAL ERROR THROW
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("❌ " + String.join("\n❌ ", errors));
        }

        return dto;
    }

    // ✅ SAFE NUMBER EXTRACTION
    private Double extractNumberSafe(String line, String field, List<String> errors) {
        try {
            String[] parts = line.split(":");
            if (parts.length < 2) {
                errors.add(field + " format is invalid. Use: " + field + ": value");
                return null;
            }
            return Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            errors.add(field + " must be a valid number");
            return null;
        }
    }

    // ✅ SAFE TEXT EXTRACTION
    private String extractText(String line, String field, List<String> errors) {
        String[] parts = line.split(":");
        if (parts.length < 2) {
            errors.add(field + " format is invalid");
            return null;
        }
        return parts[1].trim();
    }

    private String normalizeStrategy(String strategy) {

        if (strategy == null) return null;

        strategy = strategy.trim().toLowerCase();

        return strategy.substring(0, 1).toUpperCase() + strategy.substring(1);
    }

    // ✅ VALIDATION
    private void validate(ParsedTradeDTO dto, List<String> errors) {

        if (dto.symbol == null)
            errors.add("Symbol missing");

        if (dto.direction == null)
            errors.add("Direction missing (BUY or SELL)");

        if (dto.entry == null)
            errors.add("Entry missing");

        if (dto.sl == null)
            errors.add("Stop Loss missing");

        if (dto.tp == null)
            errors.add("Take Profit missing");

        if (dto.session == null)
            errors.add("Session missing");

        if (dto.strategy == null || dto.strategy.isBlank())
            errors.add("Strategy missing");

        if (dto.win == null)
            errors.add("Outcome missing (TP or SL)");
    }
}

