package com.tradingJournalBot.journalBot.service;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class TelegramService {

    @Value("${telegram.bot.token}")
    private String BOT_TOKEN ;
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




