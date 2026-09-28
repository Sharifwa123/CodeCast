package com.example.data.db

import androidx.room.*
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectVersionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Int): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    // Versions
    @Query("SELECT * FROM project_versions WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getVersionsForProject(projectId: Int): Flow<List<ProjectVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: ProjectVersionEntity): Long
}
