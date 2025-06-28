package ai.koog.agents.example.smart_tool

import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable

/**
 * Represents a sealed interface for encapsulating data related to tools. This interface allows for a variety
 * of data types to be used interchangeably as inputs or outputs of tool operations by implementing the
 * `ToolArgs` and `ToolResult` interfaces.
 *
 * This interface accommodates multiple data representations through its known subclasses.
 */
@Serializable
sealed interface ToolData : ToolArgs, ToolResult {
    @Serializable
    data object NoData : ToolData {
        override fun toStringDefault(): String = NoData::class.java.simpleName
    }

    @JvmInline
    @Serializable
    value class Text(val text: String) : ToolData {
        override fun toStringDefault(): String = text
    }

    @JvmInline
    @Serializable
    value class Expression(val data: String) : ToolData {
        override fun toStringDefault(): String = data
    }

    @JvmInline
    @Serializable
    value class DataDouble(val data: Double) : ToolData {
        override fun toStringDefault(): String = data.toString()
    }

    sealed interface Error : ToolData {
        @Serializable
        data object NotCastable : Error {
            override fun toStringDefault(): String = NoData::class.java.simpleName
        }
    }
}