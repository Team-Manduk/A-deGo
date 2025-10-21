package com.teammanduk.adego.core.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.teammanduk.adego.core.local.dao.SelectedRouteDao
import com.teammanduk.adego.core.local.model.SelectedRouteEntity

/**
 * AdeGo Room Database
 */
@Database(
    entities = [SelectedRouteEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AdeGoDatabase : RoomDatabase() {
    abstract fun selectedRouteDao(): SelectedRouteDao

    companion object {
        const val DATABASE_NAME = "adego_database"
    }
}
