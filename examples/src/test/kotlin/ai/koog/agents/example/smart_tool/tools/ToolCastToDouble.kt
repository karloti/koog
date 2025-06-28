package ai.koog.agents.example.smart_tool.tools

import ai.koog.agents.core.tools.SmartTool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResultWrapper
import ai.koog.agents.example.smart_tool.ToolData
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer

/**
 * A tool implementation that attempts to cast an input expression to a double.
 * This tool is a subclass of `SmartTool` designed to process `ToolData` arguments
 * and return a result indicating whether the casting was successful or not.
 *
 * When executed, the tool takes an `Expression` containing a string representation
 * of the value to be cast. If the casting is successful, a `DataDouble` object is
 * returned. Otherwise, an error is returned indicating the inability to cast.
 *
 * Features:
 * - Supports context-aware execution through the `SmartTool` base class.
 * - Processes the input asynchronously with a random simulated delay for execution.
 * - Maintains a history of executions using the inherited state flow.
 */
class ToolCastToDouble() : SmartTool<ToolData, ToolData>() {
    override val argsSerializer: KSerializer<ToolData> = ToolData.serializer()
    override val descriptor = ToolDescriptor(
        name = "cast to double",
        description = "casts the passed expression to double",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = ToolData.Expression::class.simpleName!!,
                description = "An expression to case to double",
                type = ToolParameterType.String
            )
        ),
    )
    override val executeWithContext: suspend SmartTool<ToolData, ToolData>.(ToolData) -> ToolResultWrapper<ToolData, ToolData> =
        executeWithContext@{ args ->
            delay((1..10L).random())
            val d = (args as? ToolData.Expression)?.data?.toDoubleOrNull()
            when (d) {
                null -> ToolResultWrapper(
                    toolCall = this,
                    args = args,
                    internalData = ToolData.Error.NotCastable,
                    externalData = ToolData.Text("The expression '${args}' is not castable to double")
                )

                else -> ToolResultWrapper(
                    toolCall = this,
                    args = args,
                    internalData = ToolData.NoData,
                    externalData = ToolData.DataDouble(d),
                )
            }
        }
}