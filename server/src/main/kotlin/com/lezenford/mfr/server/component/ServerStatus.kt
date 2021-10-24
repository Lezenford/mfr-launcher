package com.lezenford.mfr.server.component

import ru.fullrest.mfr.common.exception.ServerMaintenanceException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.withLock

object ServerStatus {
    private val maintenanceMod = AtomicBoolean(false)
    private val operationLock = ReentrantReadWriteLock()

    fun <T> operation(action: () -> T): T {
        if (maintenanceMod.get().not() && operationLock.readLock().tryLock()) {
            try {
                return action()
            } finally {
                operationLock.readLock().unlock()
            }
        } else {
            throw ServerMaintenanceException()
        }
    }

    fun maintenance(action: () -> Unit) {
        maintenanceMod.set(true)
        try {
            operationLock.writeLock().withLock(action)
        } finally {
            maintenanceMod.set(false)
        }
    }
}