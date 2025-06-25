package ai.koog.agents.core.tools

/**
 * Represents a simplified tool base class that processes specific arguments and produces a textual result.
 *
 * @param TArgs The type of arguments the tool accepts, which must be a subtype of `ToolArgs`.
 */
@Suppress("UNCHECKED_CAST")
public abstract class SmartTool<TArgs : ToolArgs, TResult : ToolResult> : Tool<TArgs, TResult>() {
    override suspend fun execute(args: TArgs): TResult {
        return executeWithContext(args) { args ->
            ToolResultWrapper(
                smartToolCall = this,
                args = args,
                internalData = null,
                externalData = ToolResult.Text(doExecute(args)),
            )
        } as TResult
    }

//    final override suspend fun execute(args: TArgs): ToolResult.Text = ToolResult.Text(doExecute(args))

    /**
     * Executes the tool's main functionality using the provided arguments and produces a textual result.
     *
     * @param args The arguments of type [TArgs] required to perform the execution.
     * @return A string representing the result of the execution.
     */
    public abstract suspend fun doExecute(args: TArgs): String
}
