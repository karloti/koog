package ai.koog.agents.examples.tripplanning.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet


class UserTools(private val showUserMessage: suspend (String) -> String) : ToolSet {
    @Tool
    @LLMDescription("Покажи на потребителя съобщение от агента и изчакай отговор. Използвай този инструмент, когато трябва да зададеш въпрос на потребителя.")
    suspend fun showMessage(
        @LLMDescription("Съобщението, което да се покаже на потребителя.")
        message: String
    ): String {
        return showUserMessage(message)
    }
}
