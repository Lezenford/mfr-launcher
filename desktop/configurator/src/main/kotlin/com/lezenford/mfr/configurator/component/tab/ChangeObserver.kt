package com.lezenford.mfr.configurator.component.tab

import javafx.beans.property.ObjectPropertyBase
import javafx.beans.property.SimpleObjectProperty
import java.util.concurrent.atomic.AtomicInteger

class ChangeObserver {
    private val counter: AtomicInteger = AtomicInteger(0)
    private val contentChangeObserver: ObjectPropertyBase<Int> = SimpleObjectProperty(counter.getAndIncrement())

    fun addListener(action: () -> Unit) {
        contentChangeObserver.addListener { _, _, _ -> action() }
    }

    fun update() {
        contentChangeObserver.value = counter.getAndIncrement()
    }
}