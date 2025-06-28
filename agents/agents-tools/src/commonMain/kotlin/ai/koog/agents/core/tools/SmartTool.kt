/**
 * SmartTool - A context-aware tool implementation for AI agents
 * 
 * SmartTool extends the standard Tool class by adding execution context tracking and
 * result history management. It enables tools to make decisions based on previous
 * invocations, share data between tools, and provide different information internally
 * versus what is exposed to the LLM.
 * 
 * Key features:
 * - Execution history tracking via StateFlow
 * - Separation of internal and external data
 * - Thread-safe concurrent execution support
 * - Tool-specific context querying
 * 
 * SmartTool addresses several challenges in AI agent development:
 * - Dynamic adaptation based on conversation context
 * - Token optimization by controlling data exposed to LLMs
 * - Complex multi-step task management
 * - Enhanced control over sensitive data exposure
 */
package ai.koog.agents.core.tools

import ai.koog.agents.core.tools.SmartTool.Companion.toolCalls
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * An abstract class that extends Tool with context-awareness capabilities.
 * 
 * SmartTool maintains a history of all tool executions and their results,
 * allowing tools to adapt their behavior based on previous calls and results.
 * 
 * @param TArgs The type of arguments accepted by this tool
 * @param TResult The type of result returned by this tool
 */
public abstract class SmartTool<TArgs : ToolArgs, TResult : ToolResult>() : Tool<TArgs, TResult>() {

    /**
     * The main execution function that must be implemented by concrete SmartTool classes.
     * This function has access to the tool instance (via 'this') and can query execution history.
     */
    public abstract val executeWithContext: suspend SmartTool<TArgs, TResult>.(TArgs) -> ToolResultWrapper<TArgs, TResult>

    /**
     * Executes the tool with the given arguments, tracks the execution,
     * and returns the external data from the result wrapper.
     * 
     * @param args The arguments for the tool execution
     * @return The external result data intended for the LLM
     */
    final override suspend fun execute(args: TArgs): TResult {
        val wrapper = executeWithContext(args)
        addToolCallResult(wrapper)
        return wrapper.externalData
    }

    /**
     * Adds a tool call result to the shared history.
     * 
     * @param toolCall The result wrapper containing tool, args, and result data
     */
    @Suppress("UNCHECKED_CAST")
    private fun addToolCallResult(toolCall: ToolResultWrapper<TArgs, TResult>) {
        _toolCalls.update { it + toolCall as ToolResultWrapper<ToolArgs, ToolResult> }
    }

    /**
     * Companion object containing shared state for all SmartTool instances.
     */
    public companion object {
        /**
         * Internal mutable state flow for tracking tool calls
         */
        private val _toolCalls = MutableStateFlow(listOf<ToolResultWrapper<ToolArgs, ToolResult>>())

        /**
         * Public read-only state flow containing the history of all tool calls
         */
        public val toolCalls: StateFlow<List<ToolResultWrapper<ToolArgs, ToolResult>>> = _toolCalls.asStateFlow()
    }
}

/**
 * Returns the number of times a specific SmartTool type has been executed.
 * 
 * @param T The specific SmartTool type to count executions for
 * @return The number of executions for the specified tool type
 */
public inline fun <reified T : SmartTool<*, *>> count(): Int =
    toolCalls.value.count { it.toolCall is T }

/**
 * Returns all execution results for a specific SmartTool type.
 * 
 * @param T The specific SmartTool type to get results for
 * @return A list of result wrappers for the specified tool type, or null if none exist
 */
public inline fun <reified T : SmartTool<*, *>> results(): List<ToolResultWrapper<ToolArgs, ToolResult>>? =
    toolCalls.value.filter { it.toolCall is T }.takeIf { it.isNotEmpty() }

/**
 * Returns the internal result data from the last execution of a specific SmartTool type.
 * 
 * @param T The specific SmartTool type to get the last internal result for
 * @return The internal result data from the last execution, or null if none exists
 */
public inline fun <reified T : SmartTool<*, *>> lastInternalResult(): ToolResult? =
    toolCalls.value.lastOrNull { it.toolCall is T }?.internalData
