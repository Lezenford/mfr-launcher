package com.lezenford.mfr.launcher.exception

/**
 * Поток данных замолчал дольше допустимого. Соединение при этом может оставаться открытым,
 * поэтому сетевой стек сам такой обрыв не замечает.
 */
class DownloadStalledException(message: String) : RuntimeException(message)
