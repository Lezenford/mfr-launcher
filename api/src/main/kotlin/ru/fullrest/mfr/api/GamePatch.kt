package ru.fullrest.mfr.api

data class GamePatch(
    val version: String,

    val addFiles: List<String>,

    val moveFiles: List<Pair<String, String>>,

    val removeFiles: List<String>
)