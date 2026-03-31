package com.tradingJournalBot.journalBot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Date;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long chatId;

    private String symbol;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    private Double entryPrice;
    private Double stopLoss;
    private Double takeProfit;

    private Double missedRR;

    private Double riskReward;
    private double pnl;


    @Enumerated(EnumType.STRING)
    private ResultType resultType;

    @Enumerated(EnumType.STRING)
    private Session session;

    private String strategy;

    private Boolean confident;
    private Boolean anxious;
    private Boolean fearful;

    @Column(length = 1000)
    private String notes;

    @Column(name = "local_date_time")
    private LocalDateTime localDateTime;

    public Boolean getWin() {

        if(resultType == null) return null;

        if(resultType == ResultType.MISSED_ENTRY) return null;

        return resultType == ResultType.TP;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "created_at")
    private Date createdAt;

}
