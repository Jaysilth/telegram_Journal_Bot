package com.tradingJournalBot.journalBot.dto;

import com.tradingJournalBot.journalBot.model.Direction;
import com.tradingJournalBot.journalBot.model.Session;

public class ParsedTradeDTO {
    public String symbol;
    public Direction direction;
    public double entry;
    public double sl;
    public double tp;

    public Boolean win;
    public Session session;
    public String strategy;

    public boolean confident;
    public boolean anxious;
    public boolean fearful;

    public String notes;
}
