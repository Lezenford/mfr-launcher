package com.lezenford.mfr.configurator.configuration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lezenford.mfr.configurator.content.AdditionalContent
import com.lezenford.mfr.configurator.content.FileTree
import com.lezenford.mfr.configurator.content.MainContent
import com.lezenford.mfr.configurator.content.SwitchableContent
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.nio.file.Path
import java.util.Collections
import java.util.TreeSet
import java.util.concurrent.atomic.AtomicReference

@Configuration
class ContentConfiguration {

    @Bean
    fun mainContent(): MainContent = MainContent()

    @Bean
    fun additionalContents(): MutableSet<AdditionalContent> = Collections.synchronizedSet(TreeSet())

    @Bean
    fun switchableContents(): MutableSet<SwitchableContent> = Collections.synchronizedSet(TreeSet())

    @Bean
    fun fileTree(): AtomicReference<FileTree> = AtomicReference()

    @Bean
    fun rootFolder(): AtomicReference<Path> = AtomicReference()

    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper()
}