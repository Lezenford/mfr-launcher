package ru.fullrest.mfr.server.telegram.component;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Data
@Log4j2
@AllArgsConstructor
@NoArgsConstructor
public class CallbackData {
    private final static ObjectMapper MAPPER = new ObjectMapper();

    @JsonAlias(value = "m")
    private String module;

    @JsonAlias(value = "d")
    private String data;

    public static String convertToJson(String module, String data) throws JsonProcessingException, TelegramApiException {
        String result = MAPPER.writeValueAsString(new CallbackData(module, data));
        if (result.getBytes().length < 1 || result.getBytes().length > 64) {
            throw new TelegramApiException("Callback data must be 1-64 bytes. Current size: " + result.getBytes().length);
        }
        return result;
    }

    public static CallbackData convertFromJson(String json) throws JsonProcessingException {
        return MAPPER.readValue(json, CallbackData.class);
    }
}
