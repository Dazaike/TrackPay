package com.trackpay.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.trackpay.app.data.local.dao.BreakIntervalDao
import com.trackpay.app.data.local.dao.GoalAllocationDao
import com.trackpay.app.data.local.dao.GoalDao
import com.trackpay.app.data.local.dao.JobDao
import com.trackpay.app.data.local.dao.WorkSessionDao
import com.trackpay.app.data.local.entity.BreakIntervalEntity
import com.trackpay.app.data.local.entity.GoalAllocationEntity
import com.trackpay.app.data.local.entity.GoalEntity
import com.trackpay.app.data.local.entity.JobEntity
import com.trackpay.app.data.local.entity.WorkSessionEntity

@Database(
    entities = [
        JobEntity::class,
        WorkSessionEntity::class,
        BreakIntervalEntity::class,
        GoalEntity::class,
        GoalAllocationEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class TrackPayDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao
    abstract fun workSessionDao(): WorkSessionDao
    abstract fun breakIntervalDao(): BreakIntervalDao
    abstract fun goalDao(): GoalDao
    abstract fun goalAllocationDao(): GoalAllocationDao
}
