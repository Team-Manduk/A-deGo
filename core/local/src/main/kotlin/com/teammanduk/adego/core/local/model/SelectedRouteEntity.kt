package com.teammanduk.adego.core.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 선택된 경로 정보를 로컬에 저장하는 Entity
 *
 * roomId를 기준으로 조회/삭제 (userId는 현재 로그인한 사용자로 자동 처리)
 */
@Entity(
    tableName = "selected_routes",
    indices = [Index(value = ["roomId"], unique = true)]
)
data class SelectedRouteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "roomId")
    val roomId: String,

    // Route 기본 정보
    val totalTime: Int,
    val totalDistance: Int,
    val totalFare: Int,
    val transferCount: Int,
    val pathType: Int,

    val startLatitude: Double?,
    val startLongitude: Double?,
    val endLatitude: Double?,
    val endLongitude: Double?,

    // SubPath 전체를 JSON으로 저장
    @ColumnInfo(name = "sub_paths_json")
    val subPathsJson: String, // List<SubPath>를 JSON으로

    val selectedAt: Long,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
