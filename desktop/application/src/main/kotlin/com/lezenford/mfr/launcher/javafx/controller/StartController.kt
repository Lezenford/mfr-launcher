package com.lezenford.mfr.launcher.javafx.controller

import org.springframework.context.annotation.Profile
import com.lezenford.mfr.javafx.component.FxController

@Profile("GUI")
class StartController : FxController("fxml/start.fxml")