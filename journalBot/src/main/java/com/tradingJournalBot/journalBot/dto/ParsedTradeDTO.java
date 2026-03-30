package com.tradingJournalBot.journalBot.dto;

import com.tradingJournalBot.journalBot.model.Direction;
import com.tradingJournalBot.journalBot.model.ResultType;
import com.tradingJournalBot.journalBot.model.Session;

public class ParsedTradeDTO {
    public String symbol;
    public Direction direction;
    public Double entry;
    public Double sl;
    public Double tp;

    public Session session;
    public String strategy;

    public ResultType resultType;

    public boolean confident;
    public boolean anxious;
    public boolean fearful;

    public String notes;
}
