package ai.koog.agents.examples.tripplanning.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.format
import kotlinx.datetime.plus

@Tool
@LLMDescription("Добави период към дата. Използвай този инструмент, когато трябва да изчислиш отмествания, като утре, след два дни, вчера.")
fun addDate(
    @LLMDescription("Датата, към която да се добави, в ISO формат, напр. 2022-01-01")
    date: String,
    @LLMDescription("Броят дни за добавяне, по подразбиране 0")
    days: Int = 0,
    @LLMDescription("Броят месеци за добавяне, по подразбиране 0")
    months: Int = 0,
): String = date.parseLocalDate()
    .plus(days, DateTimeUnit.DAY)
    .plus(months, DateTimeUnit.MONTH)
    .format(LocalDate.Formats.ISO)
