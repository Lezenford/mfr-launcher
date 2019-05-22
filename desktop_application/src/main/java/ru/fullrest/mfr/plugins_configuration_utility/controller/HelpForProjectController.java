package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;

@Log4j2
public class HelpForProjectController implements AbstractController {

    @Autowired
    private StageManager stageManager;

    @FXML
    private TextArea text;

    @Setter
    private boolean firstStart = false;

    @Override
    public void beforeOpen() {
        if (firstStart) {
            text.setText("Дорогой друг!\n" +
                    "Спасибо за то, что скачал и установил Morrowind Fullrest Repack!\n" +
                    "На создание и оптимизацию этого великолепного сборника модов ушел не один год кропотливой работы" +
                    " целой команды людей," +
                    " искренне любящих старый добрый Morrowind. " +
                    "Надеюсь, ты оценишь наши старания, ведь старались мы именно для тебя. И раз ты читаешь этот " +
                    "текст - мы все сделали правильно.\n" +
                    "Удачи тебе на просторах Вварденфелла и, если тебе действительно понравится, не забудь сказать " +
                    "авторам \"спасибо\".\n" +
                    "С наилучшими пожеланиями, команда M[FR]\n\n" +
                    "Ну а теперь, \"Мы уже почти приплыли в Морровинд...\"");
            firstStart = false;
        } else {
            text.setText("Дорогой друг!\n" +
                    "Данный проект никогда не задумывался как коммерческий продукт и развивался, " +
                    "развивается и, не смотря ни на что, будет развиваться благодаря стараниям целой команды " +
                    "энтузиастов.\n" +
                    "Но если тебе хочется поддержать проект и его создателей, просто выразить свою благодарность за " +
                    "потраченное время или дать понять, что наша работа важна для тебя - " +
                    "можно сделать пожертвование, которое поможет в разных аспектах развития, включая многие " +
                    "амбициозные цели на будущее.\n" +
                    "Спасибо тебе, дорогой друг!\n\n" +
                    "Карта Сбербанка: 4817-7600-1514-8392.\n" +
                    "Связь с автором: http://www.fullrest.ru/forum/user/14788-al/ или telegram - @tes3_aL");
        }
    }

    public void close() {
        stageManager.getHelpForProjectStage().close();
    }

}