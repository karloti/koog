package ai.koog.agents.example.smart_tools.tools

import ai.koog.agents.core.tools.SmartTool
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResultWrapper
import ai.koog.agents.core.tools.results
import ai.koog.agents.example.smart_tools.ToolData
import kotlinx.serialization.serializer

/**
 * A tool for retrieving a user's ID from a list of contacts based on their name or surname.
 *
 * This tool is context-aware and requires prior access to the full list of contacts,
 * which can be provided by the ToolGetAllContacts tool. If the relevant contact data
 * is unavailable, the tool will prompt the user to retrieve the contact list first.
 *
 * The tool is designed to handle scenarios where:
 * - No users match the provided name or surname.
 * - More than one user matches the given criteria, requiring additional details to narrow the search.
 * - Exactly one user matches the criteria, in which case the user ID is returned.
 *
 * This tool makes use of the base functionality provided by the SmartTool class,
 * including execution with context-aware capabilities and access to the tool's execution history.
 *
 * The tool descriptor includes:
 * - Name: The class name of this tool.
 * - Description: "Get user id from contacts by name or surname".
 * - Optional Parameters:
 *   - "name": A string representing the user's first name.
 *   - "surname": A string representing the user's last name.
 *
 * Execution returns a ToolResultWrapper containing one of the following:
 * - An error message if no matching users are found or if there are multiple matches.
 * - The user ID of the single matching user.
 */
class ToolGetUserIDFromList() : SmartTool<ToolData.UserNames, ToolData>() {
    override val descriptor = ToolDescriptor(
        name = this::class.simpleName!!,
        description = "Get user id from contacts by name or surname",
        optionalParameters = listOf(
            ToolParameterDescriptor(
                name = "name",
                description = "user name",
                type = ToolParameterType.String
            ),
            ToolParameterDescriptor(
                name = "surname",
                description = "user surname",
                type = ToolParameterType.String
            )
        ),
    )

    override val argsSerializer = serializer<ToolData.UserNames>()
    override val executeWithContext: suspend SmartTool<ToolData.UserNames, ToolData>.(ToolData.UserNames) -> ToolResultWrapper<ToolData.UserNames, ToolData>
        get() = execute@{ args: ToolData.UserNames ->
            println("args = $args")
            val (name, surname) = args
            val results = results<ToolGetAllContacts>()
            val map: Map<Int, ToolData.Contact> = results
                ?.firstNotNullOfOrNull { it.internalData as? ToolData.ContactMap }
                ?.contactMap
                ?: return@execute ToolResultWrapper(
                    toolCall = this,
                    args = args,
                    internalData = ToolData.NoData,
                    externalData = ToolData.Text("Please call the tool ${ToolGetAllContacts::class.simpleName} first"),
                )

            val names = setOf(name, surname).filterNotNull()
            val resultOfUserIDs: List<Int>? = map
                .filter { setOf(it.value.name, it.value.surname).intersect(names).isNotEmpty() }
                .takeIf { it.isNotEmpty() }
                ?.map { it.key }

            when {
                resultOfUserIDs == null -> ToolResultWrapper(
                    toolCall = this,
                    args = args,
                    internalData = ToolData.NoData,
                    externalData = ToolData.Text("I can't find any users with name:$name or surname:$surname."),
                )

                resultOfUserIDs.size > 1 -> ToolResultWrapper(
                    toolCall = this,
                    args = args,
                    internalData = ToolData.NoData,
                    externalData = ToolData.Text("I have more that one user. Provide more information to narrow down the search. For example: name:John or surname:Smith."),
                )

                else -> ToolResultWrapper(
                    toolCall = this,
                    args = args,
                    internalData = ToolData.NoData,
                    externalData = ToolData.UserID(resultOfUserIDs.single()),
                )
            }
        }
}