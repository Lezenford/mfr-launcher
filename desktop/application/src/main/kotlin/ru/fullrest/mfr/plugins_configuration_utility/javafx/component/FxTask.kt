package ru.fullrest.mfr.plugins_configuration_utility.javafx.component

import ru.fullrest.mfr.plugins_configuration_utility.javafx.controller.AbstractProgressController
import ru.fullrest.mfr.plugins_configuration_utility.logging.Loggable

abstract class FxTask<T>: Loggable {

    lateinit var progressController: AbstractProgressController

    abstract suspend fun process(): T

//    fun start() = GlobalScope.launch {
//        if (progressController.showing) {
//            progressController.setCloseButtonVisible(false)
//            process()
//        } //TODO добавить логику отображения или ошибки
//    }
//
//    fun startAndWait() {
//        progressController.setCloseButtonVisible(false)
//        GlobalScope.launch { process() }
//        progressController.showAndWait()
//    }

//    fun updateMessage(message: String) {
//        progressController.setDescription(message)
//    }
//
//    fun updateProgress(workDone: Long, max: Long) {
//        progressController.updateProgress(workDone, max)
//    }
//
//    fun updateProgress(workDone: Double, max: Double) {
//        progressController.updateProgress(workDone.toLong(), max.toLong())
//    }
}