package ru.fullrest.mfr.server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.api.GameUpdate;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.Update;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.service.TelegramUserService;
import ru.fullrest.mfr.server.service.UpdateService;
import ru.fullrest.mfr.server.telegram.TelegramBot;
import ru.fullrest.mfr.server.telegram.component.CallbackAnswer;
import ru.fullrest.mfr.server.telegram.component.SecureBotCommand;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Log4j2
@Component
public class UploadPatchCommand extends SecureBotCommand {

    private final ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap;
    private final UpdateService updateService;

    @Value("${server.update-folder}")
    private String updates;

    public UploadPatchCommand(TelegramUserService telegramUserService, ConcurrentMap<Long, CallbackAnswer> callbackAnswerMap,
            UpdateService updateService) {
        super("uploadpatch", "upload game patch", telegramUserService, UserRole.ADMIN);
        this.callbackAnswerMap = callbackAnswerMap;
        this.updateService = updateService;
    }

    @Override
    protected void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        callbackAnswerMap.put(chat.getId(), message -> {
            File file = new File(updates + File.separator + message.getMessage().getText());
            System.out.println(file.getAbsolutePath());
            if (file.exists() && !file.isDirectory()) {
                String version = null;
                boolean hasSchema = false;
                try {
                    ZipFile zipFile = new ZipFile(file);
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {
                        ZipEntry zipEntry = entries.nextElement();
                        if (!zipEntry.isDirectory() && zipEntry.getName().toLowerCase().equals("patch/optional/version")) {
                            try (InputStream inputStream = zipFile.getInputStream(zipEntry)) {
                                version = new String(inputStream.readAllBytes());
                            }
                        }
                        if (!zipEntry.isDirectory() && zipEntry.getName().equals(GameUpdate.FILE_NAME)) {
                            hasSchema = true;
                        }
                    }
                } catch (IOException e) {
                    log.error(e);
                }
                if (version != null && hasSchema) {
                    if (updateService.findByVersion(version) == null) {
                        Update update = new Update();
                        update.setVersion(version);
                        update.setPath(message.getMessage().getText());
                        updateService.save(update);
                        absSender.execute(new SendMessage(chat.getId(), "Добавлено новое обновление: " + version));
                    } else {
                        absSender.execute(new SendMessage(chat.getId(), "Указанная версия уже существует. Пересоздайте патч и попробуйте еще раз /uploadpatch"));
                    }
                } else  {
                    absSender.execute(new SendMessage(chat.getId(), "В файле не хватает данных о версии игры и схемы развертывания. Пересоздайте патч и попробуйте еще раз /uploadpatch"));
                }
            } else {
                absSender.execute(new SendMessage(chat.getId(), "Файл не найден на сервере. Попробуйте еще раз /uploadpatch"));
            }
        });
        absSender.execute(new SendMessage(chat.getId(), "Укажите название файла"));
    }
}
