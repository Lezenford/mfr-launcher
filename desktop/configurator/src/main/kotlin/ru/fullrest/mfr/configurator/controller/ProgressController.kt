package ru.fullrest.mfr.configurator.controller

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import ru.fullrest.mfr.javafx.component.FxController

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
class ProgressController(
    mainController: MainController
) : FxController(source = "fxml/progress.fxml", css = null, owner = mainController) {

    fun execute(init: () -> Unit) {
        show()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                init()
            } finally {
                close()
            }
        }
    }
}