package com.lezenford.mfr.javafx.component

import javafx.event.EventHandler
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.stage.Stage

class FxScene(
    stage: Stage,
    root: Parent,
    fill: Paint = Color.TRANSPARENT,
    css: String? = null
) : Scene(root, fill) {
    private var xOffset = 0.0
    private var yOffset = 0.0

    init {
        stage.scene = this
        css?.also { stylesheets.add(css) }
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