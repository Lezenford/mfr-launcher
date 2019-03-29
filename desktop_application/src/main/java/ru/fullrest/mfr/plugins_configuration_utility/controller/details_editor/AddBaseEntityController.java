package ru.fullrest.mfr.plugins_configuration_utility.controller.details_editor;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import ru.fullrest.mfr.plugins_configuration_utility.controller.AbstractController;
import ru.fullrest.mfr.plugins_configuration_utility.manager.StageManager;

public class AddBaseEntityController implements AbstractController {

    @Autowired
    private StageManager stageManager;

    @Getter
    private String text;

    @FXML
    private TextField textField;

    @FXML
    private Button addButton;

    @FXML
    private Button cancelButton;

    @Getter
    @FXML
    private Label label;

    public void init() {
        cancelButton.setOnAction(event -> {
            text = null;
            stageManager.getAddBaseEntityStage().close();
        });

        addButton.setOnAction(event -> {
            text = textField.getText();
            stageManager.getAddBaseEntityStage().close();
        });

        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                addButton.setDisable(false);
            } else {
                addButton.setDisable(true);
            }
        });
    }

    @Override
    public void beforeOpen() {
        textField.setText("");
        addButton.setDisable(true);
        label.setText("");
    }
}