package ru.fullrest.mfr.plugins_configuration_utility.javafx.controller

import com.sun.javafx.webkit.Accessor
import javafx.application.Platform
import javafx.event.EventHandler
import javafx.fxml.FXML
import javafx.scene.input.KeyCode
import javafx.scene.input.KeyEvent
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.web.WebView
import org.springframework.beans.factory.annotation.Autowired
import ru.fullrest.mfr.plugins_configuration_utility.PluginsConfigurationUtilityApplication
import ru.fullrest.mfr.plugins_configuration_utility.config.ApplicationFiles
import ru.fullrest.mfr.plugins_configuration_utility.javafx.component.FxController
import java.net.MalformedURLException
import java.net.URL

class ReadmeController : FxController() {

    @Autowired
    private lateinit var launcherController: LauncherController

    @Autowired
    private lateinit var application: PluginsConfigurationUtilityApplication

    @Autowired
    private lateinit var files: ApplicationFiles

    @FXML
    private lateinit var mainPane: HBox

    private lateinit var webView: WebView

    private lateinit var url: URL

    override fun init() {
        Platform.runLater {
            webView = WebView()
            mainPane.children.add(webView)
            HBox.setHgrow(webView, Priority.ALWAYS)
        }
        stage.onShowing = EventHandler {
            try {
                url = files.readme.toURI().toURL()
                webView.engine.load(url.toString())
                Accessor.getPageFor(webView.engine).setBackgroundColor(10)
            } catch (e: MalformedURLException) {
                log().error("Can't create readme URI")
            }
        }

        stage.addEventHandler(KeyEvent.KEY_PRESSED) {
            if (it.code == KeyCode.ESCAPE) {
                hide()
                launcherController.show()
            }
        }

        stage.onHidden = EventHandler { launcherController.show() }
    }

    fun openInBrowser() {
        application.hostServices.showDocument(url.toString())
        hide()
    }

    fun back() {
        webView.engine.history.entries
    }
}