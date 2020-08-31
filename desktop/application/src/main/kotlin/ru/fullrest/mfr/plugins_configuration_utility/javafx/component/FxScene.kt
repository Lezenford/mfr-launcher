package ru.fullrest.mfr.plugins_configuration_utility.javafx.component

import javafx.event.EventHandler
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.paint.Paint
import javafx.stage.Stage

class FxScene(
    root: Parent,
    fill: Paint,
    stage: Stage
) : Scene(root, fill) {
    private var xOffset = 0.0
    private var yOffset = 0.0

    init {
        onMousePressed = EventHandler {
            xOffset = stage.x - it.screenX
            yOffset = stage.y - it.screenY
        }
        onMouseDragged = EventHandler {
            stage.x = xOffset + it.screenX
            stage.y = yOffset + it.screenY
        }
    }
}