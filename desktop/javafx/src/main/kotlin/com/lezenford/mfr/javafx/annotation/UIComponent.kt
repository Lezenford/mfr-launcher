package com.lezenford.mfr.javafx.annotation

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("UI")
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class UIComponent
