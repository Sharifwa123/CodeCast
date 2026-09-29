package com.example.data.repository

import com.example.data.db.ProjectDao
import com.example.data.db.TutorialDao
import com.example.data.model.*
import com.example.analysis.*
import kotlinx.coroutines.flow.Flow

class CodeCastRepository(
    private val projectDao: ProjectDao,
    private val tutorialDao: TutorialDao
) {
    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun getProject(id: Int): ProjectEntity? = projectDao.getProjectById(id)

    suspend fun insertProject(project: ProjectEntity): Long = projectDao.insertProject(project)

    suspend fun deleteProject(id: Int) = projectDao.deleteProjectById(id)

    fun getTutorials(projectId: Int): Flow<List<TutorialPlanEntity>> =
        tutorialDao.getTutorialsForProject(projectId)

    suspend fun getTutorial(id: Int): TutorialPlanEntity? = tutorialDao.getTutorialById(id)

    fun observeTutorial(id: Int): Flow<TutorialPlanEntity?> = tutorialDao.observeTutorialById(id)

    suspend fun saveTutorial(tutorial: TutorialPlanEntity): Long = tutorialDao.insertTutorial(tutorial)

    suspend fun updateTutorial(tutorial: TutorialPlanEntity) = tutorialDao.updateTutorial(tutorial)

    fun getSteps(tutorialId: Int): Flow<List<TutorialStepEntity>> =
        tutorialDao.getStepsForTutorial(tutorialId)

    suspend fun getStepsList(tutorialId: Int): List<TutorialStepEntity> =
        tutorialDao.getStepsListForTutorial(tutorialId)

    suspend fun saveSteps(steps: List<TutorialStepEntity>) = tutorialDao.insertSteps(steps)

    suspend fun updateStep(step: TutorialStepEntity) = tutorialDao.updateStep(step)

    suspend fun deleteStep(stepId: Int) = tutorialDao.deleteStepById(stepId)

    fun getScenes(tutorialId: Int): Flow<List<GeneratedSceneEntity>> =
        tutorialDao.getScenesForTutorial(tutorialId)

    suspend fun getScenesList(tutorialId: Int): List<GeneratedSceneEntity> =
        tutorialDao.getScenesListForTutorial(tutorialId)

    suspend fun saveScenes(scenes: List<GeneratedSceneEntity>) = tutorialDao.insertScenes(scenes)

    suspend fun updateScene(scene: GeneratedSceneEntity) = tutorialDao.updateScene(scene)

    fun getVersions(projectId: Int): Flow<List<ProjectVersionEntity>> =
        projectDao.getVersionsForProject(projectId)

    suspend fun addVersion(version: ProjectVersionEntity) = projectDao.insertVersion(version)

    suspend fun updateProject(project: ProjectEntity) = projectDao.updateProject(project)

    /** Plans scenes from the selected steps, localizes them, persists them. */
    suspend fun generateScenes(
        tutorialId: Int,
        steps: List<TutorialStepEntity>,
        tutorial: TutorialPlanEntity,
        translator: Translator?,
        speechSeconds: (String) -> Double? = { null }
    ): Pair<List<GeneratedSceneEntity>, LocalizationResult> {
        val planned = ScenePlanner.plan(tutorialId, steps, tutorial, speechSeconds)
        val (scenes, loc) = ScenePlanner.localize(planned, tutorial, translator)
        tutorialDao.deleteScenesForTutorial(tutorialId)
        tutorialDao.insertScenes(scenes)
        return getScenesList(tutorialId) to loc
    }

    fun runQualityCheck(
        steps: List<TutorialStepEntity>,
        scenes: List<GeneratedSceneEntity>,
        tutorial: TutorialPlanEntity,
        files: List<CodebaseFile>,
        localization: LocalizationResult = LocalizationResult(true, true),
        audioAvailable: Boolean = true
    ): QualityCheckReport = QualityChecker.run(steps, scenes, tutorial, files, localization, audioAvailable)
}
