package com.teammanduk.adego.core.local.converter

import androidx.room.TypeConverter
import com.teammanduk.adego.core.model.SubPath
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Route 관련 TypeConverter
 *
 * Kotlinx Serialization을 사용하여 List<SubPath>를 JSON으로 직렬화/역직렬화
 * (TMAP API에서 사용하는 것과 동일한 직렬화 방식)
 */
class RouteTypeConverters {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    @TypeConverter
    fun fromSubPathList(value: List<SubPath>): String {
        return json.encodeToString(value)
    }

    @TypeConverter
    fun toSubPathList(value: String): List<SubPath> {
        return json.decodeFromString(value)
    }
}
