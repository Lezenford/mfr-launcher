package ru.fullrest.mfr.server.telegram.command;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.fullrest.mfr.server.model.entity.TelegramUser;
import ru.fullrest.mfr.server.model.entity.UserRole;
import ru.fullrest.mfr.server.model.repository.ApplicationDownloadHistoryRepository;
import ru.fullrest.mfr.server.model.repository.GameDownloadHistoryRepository;
import ru.fullrest.mfr.server.model.repository.TelegramUserRepository;
import ru.fullrest.mfr.server.model.repository.UpdateDownloadHistoryRepository;
import ru.fullrest.mfr.server.telegram.TelegramBot;

@Log4j2
@Component
public class StatisticCommand extends SecureBotCommand {

    private final GameDownloadHistoryRepository gameDownloadHistoryRepository;
    private final ApplicationDownloadHistoryRepository applicationDownloadHistoryRepository;
    private final UpdateDownloadHistoryRepository updateDownloadHistoryRepository;

    public StatisticCommand(TelegramUserRepository telegramUserRepository, GameDownloadHistoryRepository gameDownloadHistoryRepository,
            ApplicationDownloadHistoryRepository applicationDownloadHistoryRepository,
            UpdateDownloadHistoryRepository updateDownloadHistoryRepository) {
        super("statistic", "Download statistic", telegramUserRepository, UserRole.ADMIN);
        this.gameDownloadHistoryRepository = gameDownloadHistoryRepository;
        this.applicationDownloadHistoryRepository = applicationDownloadHistoryRepository;
        this.updateDownloadHistoryRepository = updateDownloadHistoryRepository;
    }

    @Override
    protected void execute(TelegramBot absSender, User user, TelegramUser telegramUser, Chat chat, String[] arguments) throws TelegramApiException {
        Integer gameDownloadCount = gameDownloadHistoryRepository.countAll();
        Integer applicationDownloadCount = applicationDownloadHistoryRepository.countAll();
        Integer updateDownloadCount = updateDownloadHistoryRepository.countAll();
        SendMessage sendMessage = new SendMessage(
                chat.getId(), String.format("Статистика скачивания:\nЛаунчер:%s\nИгра:%s\nОбновления:%s",
                                            applicationDownloadCount, gameDownloadCount, updateDownloadCount
                                           ));
        absSender.execute(sendMessage);
    }
}
