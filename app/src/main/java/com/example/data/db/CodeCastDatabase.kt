package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.GeneratedSceneEntity
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectVersionEntity
import com.example.data.model.TutorialPlanEntity
import com.example.data.model.TutorialStepEntity

@Database(
    entities = [
        ProjectEntity::class,
        TutorialPlanEntity::class,
        TutorialStepEntity::class,
        GeneratedSceneEntity::class,
        ProjectVersionEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class CodeCastDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun tutorialDao(): TutorialDao

    companion object {
        @Volatile
        private var INSTANCE: CodeCastDatabase? = null

        fun getDatabase(context: Context): CodeCastDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CodeCastDatabase::class.java,
                    "codecast_database.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
