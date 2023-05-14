package com.lezenford.mfr.configurator

import com.lezenford.mfr.configurator.controller.MainController
import com.lezenford.mfr.configurator.service.ContentService
import com.lezenford.mfr.configurator.service.FileTreeService
import com.lezenford.mfr.javafx.extensions.runFx
import javafx.stage.DirectoryChooser
import javafx.stage.Stage
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.io.File
import kotlin.system.exitProcess

@SpringBootApplication
class Configurator

fun main(args: Array<String>) {
    runApplication<Configurator>()
}

@Component
@Profile("UI")
class Runner(
    private val mainController: MainController,
    private val fileTreeService: FileTreeService,
    private val contentService: ContentService
) : CommandLineRunner {

    override fun run(vararg args: String?) {
//        fileTreeServiceleTreeService.initRoot("/Users/av-plekhanov/MFR/morrowind-fullrest-repack".toPath())
        runFx {
            DirectoryChooser().apply {
                initialDirectory = File("").absoluteFile
                title = "Выберите папку игры"
            }.showDialog(Stage())?.also { file ->
                fileTreeService.initRoot(file.toPath())
            } ?: exitProcess(0)
        }
        contentService.load()
        mainController.show()
    }
}