package ru.fullrest.mfr.plugins_configuration_utility.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class AlertNewsController implements AbstractController {
    @FXML
    private Label textLabel;

    @FXML
    private HBox buttonHBox;

    public void createAlert(String text, Button... buttons) {
        textLabel.setText(text);
        buttonHBox.getChildren().clear();
        buttonHBox.getChildren().addAll(buttons);
    }
}
