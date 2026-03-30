package com.tradingJournalBot.journalBot.service;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class TelegramService {

    @Value("${TELEGRAM_BOT_TOKEN:default_token}")
    private String botToken ;

    private final RestTemplate restTemplate;

    public void sendMessage( Long chatId, String text) {

        String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);

        try {
            restTemplate.postForObject(url, body, String.class);
        } catch (Exception e) {
            System.out.println("Failed to send telegram message: " + e.getMessage());;
        }
    }
}




