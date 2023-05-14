package com.lezenford.mfr.launcher.javafx.component

import javafx.fxml.FXMLLoader
import com.lezenford.mfr.javafx.component.ProgressBar

open class SimpleLauncherProgressBar(fxmlLoader: FXMLLoader) : ProgressBar(fxmlLoader) {
    override val maxLength: Int = 245
}