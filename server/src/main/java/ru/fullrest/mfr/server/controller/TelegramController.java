package ru.fullrest.mfr.server.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.*;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.fullrest.mfr.server.telegram.TelegramBot;

@Log4j2
@RestController
@RequestMapping("telegram")
@RequiredArgsConstructor
public class TelegramController {

    private final TelegramBot telegramBot;

    @RequestMapping(value = "/${telegram.bot.token}", method = RequestMethod.POST)
    @ResponseBody
    public BotApiMethod<?> onUpdateReceived(@RequestBody Update update) {
        try {
            return telegramBot.onWebhookUpdateReceived(update);
        } catch (Exception e) {
            log.error(e);
            return null;
        }
    }
}
