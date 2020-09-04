package ru.fullrest.mfr.server.telegram.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log4j2
public class CallbackModuleRegistry {
    private final Map<String, SecureCallbackModule> modules = new ConcurrentHashMap<>();

    public void register(SecureCallbackModule module) {
        modules.put(module.getName(), module);
    }

    public void deregister(String moduleName) {
        modules.remove(moduleName);
    }

    public BotApiMethod<?> execute(Update update) throws TelegramApiException {
        if (update.hasCallbackQuery()) {
            String data = update.getCallbackQuery().getData();
            if (data != null) {
                try {
                    CallbackData callbackData = CallbackData.convertFromJson(data);
                    SecureCallbackModule module = modules.get(callbackData.getModule());
                    if (module != null) {
                        return module.execute(update.getCallbackQuery(), callbackData);
                    } else {
                        log.error(String.format("Module %s doesn't exist", callbackData.getModule()));
                    }
                } catch (JsonProcessingException e) {
                    log.error("Can't parse callback data", e);
                }
            }
            return new AnswerCallbackQuery().setCallbackQueryId(update.getCallbackQuery().getId());
        } else {
            throw new TelegramApiException("Callback module can execute only updates with callback query");
        }
    }
}
