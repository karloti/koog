package ai.koog.agents.examples.tripplanning

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.prompt.markdown.markdown
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable

data class UserInput(
    val message: String,
    val currentDate: LocalDate,
    val timezone: TimeZone,
)


sealed interface SuggestPlanRequest {
    data class InitialRequest(
        val userPlan: TripPlan,
    ) : SuggestPlanRequest

    data class CorrectionRequest(
        val userPlan: TripPlan,
        val userFeedback: String,
        val prevSuggestedPlan: TripPlan,
    ) : SuggestPlanRequest
}

@Serializable
@LLMDescription("Обратна връзка от потребителя за предложението за план.")
data class PlanSuggestionFeedback(
    @property:LLMDescription("Дали предложението за план е прието.")
    val isAccepted: Boolean,
    @property:LLMDescription("Оригиналното съобщение от потребителя.")
    val message: String,
)


@Serializable
@LLMDescription(
    "Инструмент за финализиране, който съставя окончателното предложение за план според заявката на потребителя. \n" +
        "Извикайте, за да предоставите крайния резултат от предложението за план."
)
data class TripPlan(
    @property:LLMDescription("Стъпките в туристическия план на потребителя.")
    val steps: List<Step>,
) {
    @Serializable
    @LLMDescription("Стъпките в туристическия план на потребителя.")
    data class Step(
        @property:LLMDescription("Местоположение на дестинацията (напр. име на град)")
        val location: String,
        @property:LLMDescription("Код на държавата по ISO 3166-1 alpha-2 за местоположението (напр. US, GB, FR).")
        val countryCodeISO2: String? = null,
        @property:LLMDescription("Начална дата на пристигане на това място във формат ISO, напр. 2022-01-01.")
        val fromDate: LocalDate,
        @property:LLMDescription("Крайна дата на напускане на това място във формат ISO, напр. 2022-01-01.")
        val toDate: LocalDate,
        @property:LLMDescription("Допълнителна информация за тази стъпка от плана")
        val description: String
    )

    fun toMarkdownString(): String = markdown {
        h1("План:")
        br()

        steps.forEach { step ->
            h2("${step.location}, ${step.fromDate} - ${step.toDate}")
            +step.description
            br()
        }
    }
}
