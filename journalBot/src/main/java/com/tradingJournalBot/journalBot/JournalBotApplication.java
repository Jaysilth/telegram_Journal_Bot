package com.tradingJournalBot.journalBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JournalBotApplication {

	public static void main(String[] args) {
		SpringApplication.run(JournalBotApplication.class, args);
	}

}
