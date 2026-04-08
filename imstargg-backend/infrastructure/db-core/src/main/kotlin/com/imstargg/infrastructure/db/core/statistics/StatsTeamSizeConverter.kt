package com.imstargg.infrastructure.db.core.statistics

import com.imstargg.core.enums.StatsTeamSize
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter(autoApply = false)
class StatsTeamSizeConverter : AttributeConverter<StatsTeamSize, Int> {
    override fun convertToDatabaseColumn(attribute: StatsTeamSize): Int = attribute.value

    override fun convertToEntityAttribute(dbData: Int): StatsTeamSize = StatsTeamSize.fromValue(dbData)
}
