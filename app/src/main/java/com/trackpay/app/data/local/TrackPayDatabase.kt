package com.trackpay.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trackpay.app.data.local.dao.BreakIntervalDao
import com.trackpay.app.data.local.dao.JobDao
import com.trackpay.app.data.local.dao.WorkSessionDao
import com.trackpay.app.data.local.entity.BreakIntervalEntity
import com.trackpay.app.data.local.entity.JobEntity
import com.trackpay.app.data.local.entity.WorkSessionEntity

@Database(
    entities = [
        JobEntity::class,
        WorkSessionEntity::class,
        BreakIntervalEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class TrackPayDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun workSessionDao(): WorkSessionDao
    abstract fun breakIntervalDao(): BreakIntervalDao
}
