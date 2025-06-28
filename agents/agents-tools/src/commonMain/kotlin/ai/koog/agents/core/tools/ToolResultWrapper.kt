package ai.koog.agents.core.tools

/**
 * Wrapper class for encapsulating the result of a tool execution in the SmartTool framework.
 * 
 * This class is a key component of the SmartTool architecture, enabling the separation of
 * internal data (for tool-to-tool communication) from external data (exposed to the LLM).
 * It maintains a complete record of each tool execution including the tool itself,
 * the arguments provided, and both internal and external result data.
 *
 * Key benefits:
 * - Enables data separation for token optimization
 * - Facilitates tool-to-tool communication via internal data
 * - Provides complete execution context for history tracking
 * - Supports sensitive data management by controlling what's exposed to the LLM
 *
 * @param TArgs The type of arguments accepted by the tool
 * @param TResult The type of result returned by the tool
 * @property toolCall The tool being executed, represented as an instance of `Tool` with its associated argument and result types
 * @property args The arguments provided to the tool during execution
 * @property internalData The internal result data, which can contain complete or sensitive information not exposed to the LLM
 * @property externalData The external result data that will be returned to the LLM, typically optimized for token efficiency
 */
public data class ToolResultWrapper<TArgs : ToolArgs, TResult : ToolResult>(
    val toolCall: Tool<TArgs, TResult>,
    val args: TArgs,
    val internalData: TResult,
    val externalData: TResult,
) : ToolResult {
    /**
     * Returns the string representation of the external data.
     * This is what will be shown to the LLM when the tool result is displayed.
     * 
     * @return String representation of the external data
     */
    override fun toStringDefault(): String = externalData.toStringDefault()
}
