package ai.koog.agents.example.smart_tool

import ai.koog.agents.core.tools.DirectToolCallsEnabler
import ai.koog.agents.core.tools.SmartTool
import ai.koog.agents.core.tools.annotations.InternalAgentToolsApi
import ai.koog.agents.example.smart_tool.tools.CastToDoubleTracker
import ai.koog.agents.example.smart_tool.tools.ToolCastToDouble
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(InternalAgentToolsApi::class)
private object MockToolsEnabler : DirectToolCallsEnabler

const val CONCURRENCY = 1_000

class SmartToolTest {

    /**
     * Tests the concurrent execution of tools and validates their behavior under concurrent conditions.
     *
     * This test validates the following:
     * - The behavior of tools when executed concurrently, including `ToolCastToDouble` and `CastToDoubleTracker`.
     * - Proper handling of invalid input data by `ToolCastToDouble`.
     * - Synchronization and correct tracking of the tool call results.
     *
     * The test performs the following steps:
     * 1. Initializes `ToolCastToDouble` and `CastToDoubleTracker` instances.
     * 2. Executes the `ToolCastToDouble` tool with invalid input to verify error handling.
     * 3. Concurrently executes a predefined number of `ToolCastToDouble` and `CastToDoubleTracker` operations.
     * 4. Verifies and prints the results of the last 10 tool executions for debugging purposes.
     * 5. Asserts that the total number of tool result records matches the expected concurrency value.
     *
     * Used tools employ randomized delays to simulate real-world asynchronous execution environments.
     */
    @OptIn(InternalAgentToolsApi::class)
    @Test
    fun `test Concurrent Tool Execution`() = runTest {
        val toolCastToDouble = ToolCastToDouble()
        val castToDoubleTracker = CastToDoubleTracker()

        toolCastToDouble.executeUnsafe(
            args = ToolData.Expression("Hello"),
            enabler = MockToolsEnabler
        )

        assert(SmartTool.toolCalls.value.last().internalData is ToolData.Error.NotCastable)

        List(CONCURRENCY) { index ->
            async {
                when (index % 2) {
                    0 -> toolCastToDouble.executeUnsafe(
                        args = ToolData.Expression("$index.0"),
                        enabler = MockToolsEnabler
                    )

                    1 -> castToDoubleTracker.executeUnsafe(
                        args = ToolData.NoData,
                        enabler = MockToolsEnabler
                    )
                }
            }
        }.awaitAll()

        SmartTool.toolCalls.value.takeLast(10).forEachIndexed { index, toolResultWrapper ->
            val index = CONCURRENCY + index - 9
            val toolCall = toolResultWrapper.toolCall
            println("toolResultWrappers[$index] toolCall name = ${toolCall.name}")
            println("toolResultWrappers[$index] args = ${toolResultWrapper.args}")
            println("toolResultWrappers[$index] internalData = ${toolResultWrapper.internalData.toStringDefault()}")
            println("toolResultWrappers[$index] externalData = ${toolResultWrapper.externalData.toStringDefault()}")
            println("====================================================")
        }
        assertEquals(SmartTool.toolCalls.value.size - 1, CONCURRENCY)
    }
}