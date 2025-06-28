package ai.koog.agents.example.smart_tool.tools

import ai.koog.agents.core.tools.SmartTool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolResultWrapper
import ai.koog.agents.example.smart_tool.ToolData
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

/**
 * A tool implementation that tracks and counts the invocations of the "cast to double" operation.
 *
 * This class extends `SmartTool` with specialized behavior to process `ToolData.NoData` as input
 * arguments and produce a `ToolData` result. The tool inspects the history of tool executions
 * through its inherited state flow to track the last invocation of the `ToolCastToDouble` tool
 * and its external result.
 *
 * Key Features:
 * - Context-awareness: Leverages the shared state flow provided by `SmartTool`
 *   to identify and process the last result of `ToolCastToDouble`.
 * - Simulates asynchronous behavior with a random delay during execution.
 * - Generates a result containing a message with information about the last
 *   external result of the `ToolCastToDouble` tool.
 *
 * Tool Descriptor:
 * - Name: "counting"
 * - Description: "counting the invocations of cast to double"
 *
 * Behavior:
 * - Retrieves the last execution of the `ToolCastToDouble` tool from the shared history.
 * - Returns a result message encapsulated as `ToolData.Text` containing details of the
 *   last external result of the `ToolCastToDouble` tool.
 */
class CastToDoubleTracker() : SmartTool<ToolData.NoData, ToolData>() {
    override val argsSerializer: KSerializer<ToolData.NoData> = serializer<ToolData.NoData>()
    override val descriptor = ToolDescriptor(
        name = "counting",
        description = "counting the invocations of cast to double",
    )
    override val executeWithContext: suspend SmartTool<ToolData.NoData, ToolData>.(ToolData.NoData) -> ToolResultWrapper<ToolData.NoData, ToolData> =
        executeWithContext@{ args ->
            delay((1..10L).random())
            val lastCastToDoubleExternalResult = toolCalls.value
                .lastOrNull { it.toolCall is ToolCastToDouble }
                ?.externalData
            ToolResultWrapper(
                toolCall = this,
                args = args,
                internalData = ToolData.NoData,
                externalData = ToolData.Text("last external result of CastToDouble is $lastCastToDoubleExternalResult"),
            )
        }
}