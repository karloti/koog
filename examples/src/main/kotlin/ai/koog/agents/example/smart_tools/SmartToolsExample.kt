package ai.koog.agents.example.smart_tools

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.*
import ai.koog.agents.example.ApiKeyService
import ai.koog.agents.example.smart_tools.ToolData.Contact
import ai.koog.agents.example.smart_tools.tools.ToolGetAllContacts
import ai.koog.agents.example.smart_tools.tools.ToolGetUserIDFromList
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking

const val instruction = """
Please retrieve the list of contacts.
Get the users id from list by name or surname.
"""

val contactMap: Map<Int, Contact> = contactList.associateBy { it.id }

/**
 * The main entry point of the application that initializes and configures the AI agent
 * with a tool registry and handles user interactions for task automation.
 *
 * It sets up an AI agent with a registry of tools, defines event handlers for processing
 * tool call results and agent finish events, and runs a predefined user message to demonstrate
 * the agent's functionality.
 *
 * @return Unit, as this is the main function that serves as the entry point of the application.
 */
fun main(): Unit = runBlocking {
    val apiKey = ApiKeyService.openAIApiKey // Your OpenAI API key

    val toolRegistry = ToolRegistry {
        tools(listOf(ToolGetAllContacts(), ToolGetUserIDFromList()))
    }

    val agent = AIAgent(
        executor = simpleOpenAIExecutor(apiKey),
        llmModel = OpenAIModels.CostOptimized.GPT4_1Nano,
        systemPrompt = instruction,
        temperature = 0.0,
        toolRegistry = toolRegistry
    ) {
        handleEvents {
            onToolCallResult { tool: Tool<*, *>, toolArgs: ToolArgs, tooResults: ToolResult? ->
                val args = toolArgs as? ToolData
                println("= T O O L C A L   R E S U L T S ===================")
                println("Tool name: ${tool.name}")
                println("Tool args: ${args?.toStringDefault()}")
                println("Tool results: ${tooResults?.toStringDefault()}")
                println("====================================================")
            }

            onAgentFinished { strategyName: String, result: Any? ->
                println("= F I N I S H E D ==============================")
                SmartTool.toolCalls.value.forEachIndexed { index, toolResultWrapper ->
                    val toolCall = toolResultWrapper.toolCall
                    println("toolResultWrappers[$index] toolCall name = ${toolCall.name}")
                    println("toolResultWrappers[$index] args = ${toolResultWrapper.args}")
                    println("toolResultWrappers[$index] internalData = ${toolResultWrapper.internalData.toStringDefault()}")
                    println("toolResultWrappers[$index] externalData = ${toolResultWrapper.externalData.toStringDefault()}")
                    println("====================================================")
                }
            }
        }
    }

    println("Banking Assistant started")
    val message = "Get Fischer id"

    val result = agent.run(message)
    println(result)
}