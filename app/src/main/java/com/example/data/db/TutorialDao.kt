package com.example.data.db

import androidx.room.*
import com.example.data.model.GeneratedSceneEntity
import com.example.data.model.TutorialPlanEntity
import com.example.data.model.TutorialStepEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TutorialDao {
    @Query("SELECT * FROM tutorials WHERE projectId = :projectId ORDER BY createdAt DESC")
    fun getTutorialsForProject(projectId: Int): Flow<List<TutorialPlanEntity>>

    @Query("SELECT * FROM tutorials WHERE id = :id")
    suspend fun getTutorialById(id: Int): TutorialPlanEntity?

    @Query("SELECT * FROM tutorials WHERE id = :id")
    fun observeTutorialById(id: Int): Flow<TutorialPlanEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTutorial(tutorial: TutorialPlanEntity): Long

    @Update
    suspend fun updateTutorial(tutorial: TutorialPlanEntity)

    @Query("DELETE FROM tutorials WHERE id = :id")
    suspend fun deleteTutorialById(id: Int)

    // Steps
    @Query("SELECT * FROM tutorial_steps WHERE tutorialId = :tutorialId ORDER BY stepOrder ASC")
    fun getStepsForTutorial(tutorialId: Int): Flow<List<TutorialStepEntity>>

    @Query("SELECT * FROM tutorial_steps WHERE tutorialId = :tutorialId ORDER BY stepOrder ASC")
    suspend fun getStepsListForTutorial(tutorialId: Int): List<TutorialStepEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: TutorialStepEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSteps(steps: List<TutorialStepEntity>)

    @Update
    suspend fun updateStep(step: TutorialStepEntity)

    @Query("DELETE FROM tutorial_steps WHERE id = :stepId")
    suspend fun deleteStepById(stepId: Int)

    @Query("DELETE FROM tutorial_steps WHERE tutorialId = :tutorialId")
    suspend fun deleteStepsForTutorial(tutorialId: Int)

    // Scenes
    @Query("SELECT * FROM generated_scenes WHERE tutorialId = :tutorialId ORDER BY sceneOrder ASC")
    fun getScenesForTutorial(tutorialId: Int): Flow<List<GeneratedSceneEntity>>

    @Query("SELECT * FROM generated_scenes WHERE tutorialId = :tutorialId ORDER BY sceneOrder ASC")
    suspend fun getScenesListForTutorial(tutorialId: Int): List<GeneratedSceneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScene(scene: GeneratedSceneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScenes(scenes: List<GeneratedSceneEntity>)

    @Update
    suspend fun updateScene(scene: GeneratedSceneEntity)

    @Query("DELETE FROM generated_scenes WHERE id = :sceneId")
    suspend fun deleteSceneById(sceneId: Int)

    @Query("DELETE FROM generated_scenes WHERE tutorialId = :tutorialId")
    suspend fun deleteScenesForTutorial(tutorialId: Int)
}
