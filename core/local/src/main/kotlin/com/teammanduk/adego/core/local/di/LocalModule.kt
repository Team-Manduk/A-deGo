package com.teammanduk.adego.core.local.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.teammanduk.adego.core.data_api.datasource.SelectedRouteDataSource
import com.teammanduk.adego.core.data_api.datasource.SessionDataSource
import com.teammanduk.adego.core.local.dao.SelectedRouteDao
import com.teammanduk.adego.core.local.database.AdeGoDatabase
import com.teammanduk.adego.core.local.datasource.SelectedRouteDataSourceImpl
import com.teammanduk.adego.core.local.datasource.SessionDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_session")

@Module
@InstallIn(SingletonComponent::class)
object LocalModule {

    @Provides
    @Singleton
    fun provideDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> {
        return context.dataStore
    }

    @Provides
    @Singleton
    fun provideAdeGoDatabase(
        @ApplicationContext context: Context
    ): AdeGoDatabase {
        return Room.databaseBuilder(
            context,
            AdeGoDatabase::class.java,
            AdeGoDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    @Singleton
    fun provideSelectedRouteDao(
        database: AdeGoDatabase
    ): SelectedRouteDao {
        return database.selectedRouteDao()
    }
}

@Module
@InstallIn(SingletonComponent::class)
interface LocalBindModule {

    @Binds
    @Singleton
    fun bindSessionDataSource(
        impl: SessionDataSourceImpl
    ): SessionDataSource

    @Binds
    @Singleton
    fun bindSelectedRouteDataSource(
        impl: SelectedRouteDataSourceImpl
    ): SelectedRouteDataSource
}
