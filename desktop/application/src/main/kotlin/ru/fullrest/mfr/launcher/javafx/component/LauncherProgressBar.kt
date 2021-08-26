package ru.fullrest.mfr.launcher.javafx.component

import javafx.fxml.FXMLLoader
import ru.fullrest.mfr.javafx.component.ProgressBar

class LauncherProgressBar(fxmlLoader: FXMLLoader) : ProgressBar(fxmlLoader) {
    override val maxLength: Int = 245
}