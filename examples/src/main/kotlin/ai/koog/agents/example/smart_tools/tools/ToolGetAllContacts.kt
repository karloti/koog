package ai.koog.agents.example.smart_tools.tools

import ai.koog.agents.core.tools.SmartTool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolResultWrapper
import ai.koog.agents.core.tools.count
import ai.koog.agents.example.smart_tools.ToolData
import ai.koog.agents.example.smart_tools.contactMap
import kotlinx.serialization.serializer

/**
 * A tool implementation for retrieving the list of all user contacts in the system.
 *
 * ToolGetAllContacts extends the `SmartTool` class, granting it the capability to track
 * execution history and adapt its behavior based on previous invocations. On the first
 * execution, it retrieves and provides the complete list of contacts. Any subsequent calls
 * respond with a message indicating that's already been done.
 *
 * The tool operates as follows:
 * - Initially fetches a `ContactMap`, representing all contacts in a `Map<Int, Contact>`.
 * - Returns a textual message ("Now I have the list") as external data to confirm retrieval on the first call.
 * - For subsequent calls, provides a message signaling further calls are unnecessary.
 *
 * Key characteristics:
 * - `argsSerializer` handles serialization for the `ToolData.NoData` type, as no arguments are required.
 * - The tool is described by a `ToolDescriptor` that specifies its name and purpose.
 * - Uses the `executeWithContext` function, adapting execution logic depending on call history.
 *
 * Intended response logic:
 * - On the first call, provides internal data as a `ContactMap` and external data as a `Text` message.
 * - On additional calls, provides no internal data and a `Text` message indicating no further action.
 */
class ToolGetAllContacts() : SmartTool<ToolData.NoData, ToolData>() {
    @Suppress("UNCHECKED_CAST")
    override val argsSerializer = serializer<ToolData.NoData>()

    override val descriptor = ToolDescriptor(
        name = this::class.simpleName!!,
        description = "Retrieves the list of all users",
    )
    override val executeWithContext: suspend SmartTool<ToolData.NoData, ToolData>.(ToolArgs) -> ToolResultWrapper<ToolData.NoData, ToolData> =
        { args: ToolArgs ->
            when (count<ToolGetAllContacts>()) {
                0 -> ToolResultWrapper(
                    toolCall = this,
                    args = args as ToolData.NoData,
                    internalData = ToolData.ContactMap(contactMap),
                    externalData = ToolData.Text("Now I have the list"),
                )

                else -> ToolResultWrapper(
                    toolCall = this,
                    args = args as ToolData.NoData,
                    internalData = ToolData.NoData,
                    externalData = ToolData.Text("Stop calling ${this::class.simpleName}!. I already have the list."),
                )
            }
        }
}