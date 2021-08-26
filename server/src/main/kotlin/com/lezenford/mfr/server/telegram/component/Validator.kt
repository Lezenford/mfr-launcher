package com.lezenford.mfr.server.telegram.component

import org.springframework.stereotype.Component
import javax.annotation.PostConstruct

@Component
class Validator(
    private val commands: Collection<BotCommand>
) {

    @PostConstruct
    fun validate() {
        commands.forEach { command ->
            command.command.lowercase().removePrefix(BotCommand.COMMAND_INIT_CHARACTER).takeIf { it.length in 1..32 }
                ?: throw IllegalArgumentException("${command.command} command identifier must be 1-32 symbols")
            command.description.takeIf { it.length in 3..256 || command.publish.not() }
                ?: throw IllegalArgumentException("${command.command} command description must be 3-256 symbols for publish")
        }
    }
}