package com.lezenford.mfr.launcher.exception

class NotEnoughSpaceException(override val message: String) : RuntimeException(message) {
}