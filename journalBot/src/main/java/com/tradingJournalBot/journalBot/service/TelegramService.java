package com.tradingJournalBot.journalBot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class TelegramService {

    private final String BOT_TOKEN = "8579181160:AAEKUay5Uaf-uzxsnWOc-I4WrxWXnmxa7BA";
    private final String CHAT_ID = "8295076839";
    private final RestTemplate restTemplate;

    public void sendMessage(/*Integer*/ Long chatId, String text) {

        String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage";

        Map<String, Object> body = new HashMap<>();
        body.put("chat_id", chatId);
        body.put("text", text);

        restTemplate.postForObject(url, body, String.class);
    }
}




