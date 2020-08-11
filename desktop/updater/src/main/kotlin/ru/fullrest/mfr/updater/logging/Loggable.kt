package ru.fullrest.mfr.updater.logging

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

interface Loggable {
    fun <T : Loggable> T.log(): Logger =
        LogManager.getLogger(javaClass)
}