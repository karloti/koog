package ai.koog.agents.example.smart_tools

import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable

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

    @Serializable
    data class UserNames(
        val name: String? = null,
        val surname: String? = null,
    ) : ToolData {
        override fun toStringDefault(): String = "name: $name surname: $surname"
    }

    @Serializable
    data class Contact(
        val id: Int,
        val name: String,
        val surname: String,
        val phoneNumber: String
    ) : ToolData {
        override fun toStringDefault(): String = "$id: $name $surname ($phoneNumber)"
    }

    @JvmInline
    @Serializable
    value class ContactMap(val contactMap: Map<Int, Contact>) : ToolData {
        override fun toStringDefault(): String = contactMap.toString()
    }

    @JvmInline
    @Serializable
    value class UserID(val userID: Int) : ToolData {
        override fun toStringDefault(): String = userID.toString()
    }
}
/*@Serializable
sealed interface ToolData<T : ToolResult.JSONSerializable<T>> : ToolArgs, ToolResult, ToolResult.JSONSerializable<T> {
    @Serializable
    data object NoData : ToolData<NoData> {
        override fun getSerializer() = serializer()
    }

    @JvmInline
    @Serializable
    value class Text(val text: String) : ToolData<Text> {
        override fun getSerializer() = serializer()
        override fun toStringDefault(): String = text
    }

    @Serializable
    data class UserNames(
        val name: String? = null,
        val surname: String? = null,
    ) : ToolData<UserNames> {
        override fun getSerializer() = serializer()
        override fun toStringDefault(): String = "name: $name surname: $surname"
    }

    @Serializable
    data class Contact(
        val id: Int,
        val name: String,
        val surname: String,
        val phoneNumber: String
    ) : ToolData<Contact> {
        override fun getSerializer() = serializer()
        override fun toStringDefault(): String = "$id: $name $surname ($phoneNumber)"
    }

    @JvmInline
    @Serializable
    value class ContactMap(val contactMap: Map<Int, Contact>) : ToolData<ContactMap> {
        override fun getSerializer() = serializer()
    }

    @JvmInline
    @Serializable
    value class UserID(val userID: Int) : ToolData<UserID> {
        override fun getSerializer() = serializer()
        override fun toStringDefault(): String = userID.toString()
    }
}*/