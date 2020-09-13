package ru.fullrest.mfr.api

data class GameUpdate(
    val version: String,

    val addFiles: List<String>,

    val moveFiles: List<MoveFile>,

    val removeFiles: List<String>
) {
    companion object {
        const val FILE_NAME = "update.json"
    }
}

data class MoveFile(
    val from: String,

    val to: String
)