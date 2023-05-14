package com.lezenford.mfr.common.extensions

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.companionObject

class Logger<in R : Any> : ReadOnlyProperty<R, Logger> {
    private var value: Logger? = null

    override fun getValue(thisRef: R, property: KProperty<*>): Logger {
        return value ?: LogManager.getLogger(thisRef.javaClass.enclosingClass
            ?.takeIf { it.kotlin.companionObject?.java == thisRef.javaClass }
            ?: thisRef.javaClass).also { value = it }
    }
}