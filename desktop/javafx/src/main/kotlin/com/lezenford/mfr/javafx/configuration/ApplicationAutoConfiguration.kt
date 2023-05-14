package com.lezenford.mfr.javafx.configuration

import com.lezenford.mfr.javafx.Application
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(Application::class)
@Configuration
class ApplicationAutoConfiguration