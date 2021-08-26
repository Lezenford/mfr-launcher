package ru.fullrest.mfr.launcher.exception

class NotEnoughSpaceException(override val message: String) : RuntimeException(message) {
}