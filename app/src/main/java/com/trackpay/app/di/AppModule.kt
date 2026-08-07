package com.trackpay.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.trackpay.app.data.local.TrackPayDatabase
import com.trackpay.app.data.local.dao.BreakIntervalDao
import com.trackpay.app.data.local.dao.JobDao
import com.trackpay.app.data.local.dao.WorkSessionDao
import com.trackpay.app.domain.time.Clock
import com.trackpay.app.domain.time.SystemClock
import com.trackpay.app.domain.usecase.NoOpSessionMutationHooks
import com.trackpay.app.domain.usecase.SessionMutationHooks

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "trackpay_prefs")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideTrackPayDatabase(
        @ApplicationContext context: Context,
    ): TrackPayDatabase =
        Room.databaseBuilder(
            context,
            TrackPayDatabase::class.java,
            "trackpay.db",
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideJobDao(db: TrackPayDatabase): JobDao = db.jobDao()

    @Provides
    fun provideWorkSessionDao(db: TrackPayDatabase): WorkSessionDao = db.workSessionDao()

    @Provides
    fun provideBreakIntervalDao(db: TrackPayDatabase): BreakIntervalDao = db.breakIntervalDao()

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context,
    ): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideClock(): Clock = SystemClock()

    @Provides
    @Singleton
    fun provideSessionMutationHooks(impl: NoOpSessionMutationHooks): SessionMutationHooks = impl
}
