package com.teammanduk.adego.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.teammanduk.adego.core.local.model.SelectedRouteEntity

/**
 * 선택된 경로 DAO
 */
@Dao
interface SelectedRouteDao {
    /**
     * roomId로 경로 조회
     */
    @Query("SELECT * FROM selected_routes WHERE roomId = :roomId LIMIT 1")
    suspend fun getRoute(roomId: String): SelectedRouteEntity?

    /**
     * 경로 저장 (roomId가 같으면 덮어쓰기)
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRoute(route: SelectedRouteEntity): Long

    /**
     * roomId로 경로 삭제
     */
    @Query("DELETE FROM selected_routes WHERE roomId = :roomId")
    suspend fun deleteRoute(roomId: String): Int

    /**
     * 모든 경로 삭제
     */
    @Query("DELETE FROM selected_routes")
    suspend fun deleteAllRoutes(): Int
}
