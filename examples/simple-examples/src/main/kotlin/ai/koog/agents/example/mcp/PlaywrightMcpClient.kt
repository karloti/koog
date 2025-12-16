package ai.koog.agents.example.mcp

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.prompt.executor.clients.google.GoogleModels
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleGoogleAIExecutor
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Example of using the MCP (Model Context Protocol) integration with Playwright.
 *
 * This example demonstrates how to:
 * 1. Start a Playwright MCP server on port 8931
 * 2. Connect to the MCP server using the McpToolRegistryProvider's SSE client
 * 3. Create a ToolRegistry with tools from the MCP server
 * 4. Use the tools in an AI agent to automate browser interactions
 *
 * The example specifically shows how to open a browser and navigate to jetbrains.com
 * using the Playwright tools provided by the MCP server.
 */
fun main() {
    // Get the API key from environment variables
    val openAIApiToken = System.getenv("OPENAI_API_KEY") ?: error("OPENAI_API_KEY environment variable not set")
    val googleApiKey = System.getenv("GOOGLE_AI_API_KEY") ?: error("GOOGLE_API_KEY environment variable not set")



//    val process = ProcessBuilder(
//        "docker", "run", "-i",
//        "-port", "8931:8931",
//        "mcp/playwright"
//    ).start()

    // Start the Docker container with the Google Maps MCP server
//    val process = ProcessBuilder(
//        "npx",
//        "@playwright/mcp@latest",
//        "--port",
//        "8931"
//    ).start()

    // Wait for the server to start
//    Thread.sleep(5000)

    try {
        runBlocking {
            try {
                // Create the ToolRegistry with tools from the MCP server
                println("Connecting to Playwright MCP server...")
                val toolRegistry = McpToolRegistryProvider.fromTransport(
                    transport = McpToolRegistryProvider.defaultSseTransport("http://localhost:8931/sse")
                )
                println("Successfully connected to Playwright MCP server")

                // Create the runner
                val agent = AIAgent(
                    promptExecutor = simpleGoogleAIExecutor(googleApiKey),
//                    promptExecutor = simpleOpenAIExecutor(openAIApiToken),
                    llmModel = GoogleModels.Gemini2_5Flash,
//                    llmModel = OpenAIModels.Chat.GPT5Mini,
                    toolRegistry = toolRegistry,
                ){
                    handleEvents {
                        onToolCallStarting { ctx ->
                            val s = ctx.toolArgs?.toString() ?: return@onToolCallStarting
                            Json.parseToJsonElement(s).jsonObject.mapValues { it.value.toString() }.forEach {
                                println("Tool ${ctx.tool.name} args: ${it.key}: ${it.value}")
                            }
                            println("-".repeat(100))
                        }
                        onToolCallCompleted { ctx ->
                            val result = ctx.tool.encodeResultToStringUnsafe(ctx.result)
                            Json.parseToJsonElement(result).jsonObject.mapValues { it.value.toString() }.forEach {
                                println("Tool ${ctx.tool.name} result: ${it.key}: ${it.value}")
                            }
                            println("-".repeat(100))
                        }
                    }

                }
                val request = "Отиди на https://www.toner.bg/ и потвърди бисквитите ако се изисква. Потърси в този сайт някой забавна случка или майтап!"
                println("Sending request: $request")
                agent.run(
                    request //+ "You can only call tools. Use the Playwright tools to complete this task."
                )
                println("Result: ${agent.result()}")
            } catch (e: Exception) {
                println("Error connecting to Playwright MCP server: ${e.message}")
                e.printStackTrace()
            }
        }
    } finally {
        // Shutdown the curl process
        println("Closing connection to Playwright MCP server")
//        process.destroy()
    }
}
