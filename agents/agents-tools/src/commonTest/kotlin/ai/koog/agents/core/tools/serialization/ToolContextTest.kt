package ai.koog.agents.core.tools.serialization

import ai.koog.agents.core.tools.*
import ai.koog.agents.core.tools.annotations.InternalAgentToolsApi
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(InternalAgentToolsApi::class)
class ToolContextTest {
    // Unstructured tool

    private object UnstructuredTool : SmartTool<ToolArgs.Empty, ToolResult>() {
        override val argsSerializer = ToolArgs.Empty.serializer()

        override val descriptor = ToolDescriptor(
            name = "unstructured_tool",
            description = "Unstructured tool"
        )

        override suspend fun doExecute(args: ToolArgs.Empty): String = "Smart result"
    }

    @Test
    fun testSmartUnstructuredToolSerialization() = runTest {
        val args = JsonObject(emptyMap())
        val result = UnstructuredTool.execute(UnstructuredTool.decodeArgs(args), Enabler)
        val resultCount = UnstructuredTool.count<UnstructuredTool>()
        assertEquals(1, resultCount)
        val lastExternalResult: ToolResult? = UnstructuredTool.lastExternalResult<UnstructuredTool>()
        assertEquals(lastExternalResult!!.toStringDefault(), result.toStringDefault())
    }

    @Test
    fun testSmartUnstructuredToolCount() = runTest {
        val args = JsonObject(emptyMap())
        val count = 100_000
        val list: List<ToolResult> = List(count) {
            async {
                UnstructuredTool.execute(UnstructuredTool.decodeArgs(args), Enabler)
            }
        }.awaitAll()
        val resultCount = UnstructuredTool.count<UnstructuredTool>()
        assertEquals(count + 1, resultCount)
    }

}
