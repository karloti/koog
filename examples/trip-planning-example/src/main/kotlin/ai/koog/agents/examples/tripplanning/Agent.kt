package ai.koog.agents.examples.tripplanning

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.agent.entity.createStorageKey
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.HistoryCompressionStrategy
import ai.koog.agents.core.dsl.extension.nodeLLMRequestStructured
import ai.koog.agents.core.dsl.extension.replaceHistoryWithTLDR
import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.core.tools.reflect.asTool
import ai.koog.agents.core.tools.reflect.asTools
import ai.koog.agents.core.tools.reflect.tool
import ai.koog.agents.core.tools.reflect.tools
import ai.koog.agents.examples.tripplanning.api.OpenMeteoClient
import ai.koog.agents.examples.tripplanning.tools.UserTools
import ai.koog.agents.examples.tripplanning.tools.WeatherTools
import ai.koog.agents.examples.tripplanning.tools.addDate
import ai.koog.agents.ext.agent.subgraphWithTask
import ai.koog.agents.features.eventHandler.feature.handleEvents
import ai.koog.agents.features.opentelemetry.attribute.CustomAttribute
import ai.koog.agents.features.opentelemetry.feature.OpenTelemetry
import ai.koog.agents.features.opentelemetry.integration.langfuse.addLangfuseExporter
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.bedrock.BedrockInferencePrefixes
import ai.koog.prompt.executor.clients.bedrock.BedrockModels
import ai.koog.prompt.executor.clients.bedrock.withInferenceProfile
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.markdown.markdown
import ai.koog.prompt.xml.xml
import java.util.UUID

// UNUSED!
fun createSimplePlannerAgent(
    promptExecutor: PromptExecutor,
    openMeteoClient: OpenMeteoClient,
    googleMapsMcpRegistry: ToolRegistry,
    onToolCallEvent: (String) -> Unit,
    showMessage: suspend (String) -> String,
): AIAgent<String, String> {
    val weatherTools = WeatherTools(openMeteoClient)

    val userTools = UserTools(
        showMessage,
        /** other user-facing tools if needed */
    )

    val toolRegistry = ToolRegistry {
        tool(::addDate)
        tools(weatherTools)
        tools(userTools)
    } + googleMapsMcpRegistry

    return AIAgent(
        promptExecutor = promptExecutor,
        llmModel = OpenAIModels.Chat.GPT4o,
        temperature = 0.3,
        toolRegistry = toolRegistry,
        systemPrompt = """
            Вие сте агент за планиране на пътувания, който помага на потребителя да планира своето пътуване.
            Използвайте информацията предоставена от потребителя, за да предложите възможно най-добрия план за пътуване.
            Отговаряй само на български език!
        """.trimIndent(),
        maxIterations = 200
    ) {
        handleEvents {
            onToolCallStarting { ctx ->
                onToolCallEvent(
                    "Tool ${ctx.tool.name}, args ${
                        ctx.toolArgs.toString().replace('\n', ' ').take(100)
                    }..."
                )
            }
        }
    }
}

fun createPlannerAgent(
    promptExecutor: PromptExecutor,
    openMeteoClient: OpenMeteoClient,
    googleMapsMcpRegistry: ToolRegistry,
    onToolCallEvent: (String) -> Unit,
    showMessage: suspend (String) -> String,
): AIAgent<UserInput, TripPlan> {
    val sessionId = UUID.randomUUID().toString()
    val weatherTools = WeatherTools(openMeteoClient)
    val userTools = UserTools(
        showMessage,
        /** other user-facing tools if needed */
    )

    val toolRegistry = ToolRegistry {
        tool(::addDate)
        tools(weatherTools)
        tools(userTools)
    } + googleMapsMcpRegistry

    toolRegistry.tools.forEachIndexed { index, tool ->
        println("tool[$index]: " + tool.name)
    }

    val plannerStrategy = plannerStrategy(
        googleMapsTools = googleMapsMcpRegistry.tools,
        addDateTool = ::addDate.asTool(),
        weatherTools = weatherTools,
        userTools = userTools,
    )


    val bedrockModel: LLModel = BedrockModels.AnthropicClaude4_5Haiku.withInferenceProfile(BedrockInferencePrefixes.EU.prefix)
    val agentConfig = AIAgentConfig(
        prompt = prompt(
            "planner-agent-prompt",
        ) {
            system(
                """
                Вие сте агент за планиране на пътувания, който помага на потребителя да планира своето пътуване.
                Използвайте информацията предоставена от потребителя, за да предложите възможно най-добрия план за пътуване.
                Отговаряй само на български език!
                """.trimIndent()
            )
        },
//        model = OpenAIModels.Chat.GPT5Mini,
//        model = GoogleModels.Gemini2_5Flash,
//        model = GoogleModels.Gemini2_5Pro,
//        model = BedrockModels.AnthropicClaude4_5Haiku.withInferenceProfile(BedrockInferencePrefixes.EU.prefix),
//        model = BedrockModels.AnthropicClaude4_5Sonnet.withInferenceProfile(BedrockInferencePrefixes.EU.prefix),
        model = bedrockModel,
        maxAgentIterations = 200
    )

    // Create the runner
    return AIAgent<UserInput, TripPlan>(
        promptExecutor = promptExecutor,
        strategy = plannerStrategy,
        agentConfig = agentConfig,
        toolRegistry = toolRegistry,
    ) {
        handleEvents {
            onToolCallStarting { ctx ->
                onToolCallEvent(
                    "Tool ${ctx.tool.name}, args ${
                        ctx.toolArgs.toString().replace('\n', ' ').take(100)
                    }..."
                )
            }
        }

        this.install(OpenTelemetry) {
            addLangfuseExporter(
                traceAttributes = listOf(
                    CustomAttribute("langfuse.session.id", sessionId),
                    CustomAttribute("langfuse.trace.tags", listOf("chat", "kotlin", "production"))
                )
            )
//            addWeaveExporter(
//                weaveOtelBaseUrl = "https://trace.wandb.ai/otel/v1/traces"
//            )

            setVerbose(true)
        }
    }
}


private fun plannerStrategy(
    googleMapsTools: List<Tool<*, *>>,
    addDateTool: Tool<*, *>,
    weatherTools: WeatherTools,
    userTools: UserTools
) = strategy<UserInput, TripPlan>("planner-strategy") {
    val userPlanKey = createStorageKey<TripPlan>("user_plan")
    val prevSuggestedPlanKey = createStorageKey<TripPlan>("prev_suggested_plan")

    // Nodes

    // Set additional system instructions
    val setup by node<UserInput, String> { userInput ->
        llm.writeSession {
            appendPrompt {
                system {
                    +"Днешната дата е ${userInput.currentDate}."
                    +"Времевата зона на потребителя е ${userInput.timezone}."
                }
            }
        }

        userInput.message
    }

    val clarifyUserPlan by subgraphWithTask<String, TripPlan>(
        tools = userTools.asTools() + addDateTool
    ) { initialMessage ->
        xml {
            tag("instructions") {
                +"""
                Изяснявайте потребителския план докато не бъдат уточнени местата, датите и допълнителната информация като предпочитания.    
                """.trimIndent()
            }

            tag("initial_user_message") {
                +initialMessage
            }
        }
    }

    val suggestPlan by subgraphWithTask<SuggestPlanRequest, TripPlan>(
        tools = googleMapsTools + weatherTools.asTools()
    ) { input ->
        xml {
            tag("instructions") {
                markdown {
                    h2("Изисквания")
                    bulleted {
                        item("Предложете план за ВСИЧКИ дни и ВСИЧКИ места в потребителския план, запазвайки реда.")
                        item("Следвайте потребителския план и предоставете подробно предложение за план стъпка по стъпка с множество опции за всяка дата.")
                        item("Вземете предвид метеорологичните условия, когато предлагате места за всяка дата и час, за да прецените доколко дейността е подходяща за времето.")
                        item("Проверете подробна информация за всяко място, като работно време и отзиви, преди да го добавите към окончателното предложение за план.")
                    }

                    h2("Указания за използване на инструментите")
                    +"""
                    ВИНАГИ използвайте инструмента "maps_search_places" за търсене на места, ИЗБЯГВАЙТЕ да правите собствени предложения.
                    При търсене на места, пазете заявката кратка и конкретна:
                    Пример ДА: "музей", "исторически музей", "италиански ресторант", "кафене", "галерия"
                    Пример НЕ: "интересни културни обекти", "ресторанти с местна кухня", "ресторант в центъра на града"
                    """.trimIndent()
                    br()

                    """
                    Използвай други "maps_*" инструменти за да получиш повече детайли за мястото: отзиви, работно време, разстояния и др.
                    """.trimIndent()
                    br()

                    """
                    Използвай ${
                        weatherTools.asTools().joinToString(", ") { it.name }
                    } инструмент за всяка дата, изисквайки почасова детайлност когато трябва да направиш подробен график.
                    """.trimIndent()
                }
            }

            when (input) {
                is SuggestPlanRequest.InitialRequest -> {
                    tag("user_plan") {
                        +input.userPlan.toMarkdownString()
                    }
                }

                is SuggestPlanRequest.CorrectionRequest -> {
                    tag("additional_instructions") {
                        +"User asked for corrections to the previously suggested plan. Provide updated plan according to these corrections."
                    }

                    tag("user_plan") {
                        +input.userPlan.toMarkdownString()
                    }

                    tag("previously_suggested_plan") {
                        +input.prevSuggestedPlan.toMarkdownString()
                    }

                    tag("user_feedback") {
                        +input.userFeedback
                    }
                }
            }
        }
    }

    val saveUserPlan by node<TripPlan, Unit> { plan ->
        storage.set(userPlanKey, plan)

        llm.writeSession {
            replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.WholeHistory)
        }
    }

    val savePrevSuggestedPlan by node<TripPlan, TripPlan> { plan ->
        storage.set(prevSuggestedPlanKey, plan)

        llm.writeSession {
            replaceHistoryWithTLDR(strategy = HistoryCompressionStrategy.WholeHistory)
        }

        plan
    }

    val createInitialPlanRequest by node<Unit, SuggestPlanRequest> {
        SuggestPlanRequest.InitialRequest(
            userPlan = storage.getValue(userPlanKey),
        )
    }

    val createPlanCorrectionRequest by node<String, SuggestPlanRequest> { userFeedback ->
        SuggestPlanRequest.CorrectionRequest(
            userFeedback = userFeedback,
            userPlan = storage.getValue(userPlanKey),
            prevSuggestedPlan = storage.getValue(prevSuggestedPlanKey)
        )
    }

    // Show plan suggestion to the user and get a response
    val showPlanSuggestion by node<String, String> { message ->
        userTools.showMessage(message)
    }

    val processUserFeedback by nodeLLMRequestStructured<PlanSuggestionFeedback>()

    // Edges

    nodeStart then setup then clarifyUserPlan then saveUserPlan then createInitialPlanRequest then suggestPlan then savePrevSuggestedPlan

    edge(
        savePrevSuggestedPlan forwardTo showPlanSuggestion
            transformed { it.toMarkdownString() }
    )

    edge(showPlanSuggestion forwardTo processUserFeedback)

    edge(
        processUserFeedback forwardTo createPlanCorrectionRequest
            transformed { it.getOrThrow().structure }
            onCondition { !it.isAccepted }
            transformed { it.message }
    )
    edge(
        processUserFeedback forwardTo nodeFinish
            transformed { it.getOrThrow().structure }
            onCondition { it.isAccepted }
            transformed { storage.getValue(prevSuggestedPlanKey) }
    )

    edge(createPlanCorrectionRequest forwardTo suggestPlan)
}
