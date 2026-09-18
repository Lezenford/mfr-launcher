package com.lezenford.mfr.launcher.task

import com.lezenford.mfr.common.exception.ServerMaintenanceException
import com.lezenford.mfr.launcher.exception.ApplicationException
import com.lezenford.mfr.launcher.extension.versionLine
import com.lezenford.mfr.launcher.service.State
import com.lezenford.mfr.launcher.service.provider.KtorProvider
import com.lezenford.mfr.launcher.service.provider.LineVersion

/**
 * Линия установленной игры: выбранная игроком либо выведенная из номера версии локальной схемы.
 */
internal fun installedLine(): String =
    State.selectedBuild.value
        ?: State.schema.value?.version?.versionLine()
        ?: throw ApplicationException("Линия установленной игры не определена")

/**
 * Активная версия линии для задач, которым без неё делать нечего. Отсутствие версии в линии —
 * временное состояние, о котором сообщается как о работах на сервере; неизвестная линия —
 * постоянное, и это ошибка.
 */
internal suspend fun KtorProvider.requireActiveGameVersion(line: String): String =
    when (val result = findActiveGameVersion(line)) {
        is LineVersion.Found -> result.id
        LineVersion.NothingYet -> throw ServerMaintenanceException("В линии $line пока нет активной версии")
        LineVersion.NoSuchLine -> throw ApplicationException("Линия $line недоступна на сервере")
    }
