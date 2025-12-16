package ai.koog.agents.examples.tripplanning

import ai.koog.agents.examples.tripplanning.api.OpenMeteoClient
import ai.koog.agents.mcp.McpToolRegistryProvider
import ai.koog.agents.mcp.defaultStdioTransport
import ai.koog.prompt.executor.clients.anthropic.AnthropicLLMClient
import ai.koog.prompt.executor.clients.bedrock.BedrockClientSettings
import ai.koog.prompt.executor.clients.bedrock.BedrockLLMClient
import ai.koog.prompt.executor.clients.bedrock.BedrockRegions
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.llms.SingleLLMPromptExecutor
import ai.koog.prompt.executor.llms.all.simpleBedrockExecutor
import ai.koog.prompt.llm.LLMProvider
import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.bedrockruntime.BedrockRuntimeClient
import io.modelcontextprotocol.kotlin.sdk.client.StdioClientTransport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun main() = runBlocking(Dispatchers.Default) {
    val openAiKey = System.getenv("OPENAI_API_KEY")
//    val mistralAiKey = System.getenv("MISTRALAI_API_KEY")
    val anthropicKey = System.getenv("ANTHROPIC_API_KEY")
    val googleAiKey = System.getenv("GOOGLE_AI_API_KEY")
    val googleMapsKey = System.getenv("GOOGLE_MAPS_API_KEY")

    val googleMapsMcp = createGoogleMapsMcp(googleMapsKey)

    try {
        // Create agent
        val agent = createPlannerAgent(
            promptExecutor = MultiLLMPromptExecutor(
                LLMProvider.OpenAI to OpenAILLMClient(openAiKey),
                LLMProvider.Anthropic to AnthropicLLMClient(anthropicKey),
                LLMProvider.Google to GoogleLLMClient(googleAiKey),
                LLMProvider.Bedrock to BedrockLLMClient(
                    identityProvider = StaticCredentialsProvider {
                        this.accessKeyId = System.getenv("AWS_ACCESS_KEY_ID")
                        this.secretAccessKey = System.getenv("AWS_SECRET_ACCESS_KEY")
                    },
                    settings = BedrockClientSettings(
                        region = BedrockRegions.EU_CENTRAL_1.regionCode,
                        maxRetries = 3
                    )
                ),
            ),
            openMeteoClient = OpenMeteoClient(),
            googleMapsMcpRegistry = McpToolRegistryProvider.fromTransport(googleMapsMcp),
            onToolCallEvent = {
                println("функция: $it")
            },
            showMessage = {
                println("Агент: $it")
                print("Отговор > ")
                readln()
            }
        )

        // Get initial request
        println("Здравейте, аз съм агент за планиране на пътувания. Кажете ми къде и кога искате да отидете и аз ще ви помогна да подготвите плана.")
        print("Отговор > ")
        val message = readln()

        val timezone = TimeZone.currentSystemDefault()
        val userInput = UserInput(
            message = message,
            currentDate = Clock.System.now().toLocalDateTime(timezone).date,
            timezone = timezone,
        )

        // Print final result
        val result: TripPlan = agent.run(userInput)
        println(result.toMarkdownString())
    } finally {
        // Don't forget to close MCP transport after use
//        googleMapsMcp.close()
    }
}

private suspend fun createGoogleMapsMcp(googleMapsKey: String): StdioClientTransport {
    // Start MCP server
    val process = ProcessBuilder(
        "docker", "run", "-i",
        "-e", "GOOGLE_MAPS_API_KEY=$googleMapsKey",
        "mcp/google-maps"
    ).start()

    // Stupid straightforward way to wait for the MCP server to boot
    delay(1000)

    // Create transport to MCP
    return McpToolRegistryProvider.defaultStdioTransport(process)
}
