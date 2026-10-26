package com.lukr99.workout.data

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.lukr99.workout.data.run.RoomRunRepository
import java.util.concurrent.Executor
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.SQLiteMode

/** The repository contract against Room, the reference the web edition's store is held to. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [34], application = Application::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class RoomWorkoutRepositoryContractTest : WorkoutRepositoryContract() {

    private val databases = mutableListOf<WorkoutDb>()

    override fun newStore(): Store {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val direct = Executor { it.run() }
        val db = Room.inMemoryDatabaseBuilder(context, WorkoutDb::class.java)
            .allowMainThreadQueries().setQueryExecutor(direct).setTransactionExecutor(direct).build()
        databases += db
        return Store(RoomWorkoutRepository(db.workoutDao(), RoomTransactionRunner(db)), RoomRunRepository(db.runDao()))
    }

    @After
    fun closeDatabases() = databases.forEach(WorkoutDb::close)
}
