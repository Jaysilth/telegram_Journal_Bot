package com.tradingJournalBot.journalBot.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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

    private String symbol;

    @Enumerated(EnumType.STRING)
    private Direction direction;

    private double entryPrice;
    private double stopLoss;
    private double takeProfit;

    private double riskReward;
    private double pnl;

    @Column(name = "is_win")
    private boolean win;

    @Enumerated(EnumType.STRING)
    private Session session;

    private String strategy;

    private boolean confident;
    private boolean anxious;
    private boolean fearful;

    @Column(length = 1000)
    private String notes;

    @Column(name = "local_date_time")
    private LocalDateTime localDateTime;

    public Boolean getWin() {
        return win;
    }
}
