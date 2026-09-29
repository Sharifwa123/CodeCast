package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val framework: String,
    val repoSource: String, // "ZIP", "GITHUB", "GITLAB", "BITBUCKET", "DEMO"
    val repoUrl: String = "",
    val screensCount: Int = 0,
    val routesCount: Int = 0,
    val featuresJson: String = "",
    val techStackJson: String = "",
    val activeVersion: String = "v1.0.0",
    val hasRuntimeVerification: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tutorials")
data class TutorialPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val title: String,
    val tutorialType: String, // "GETTING_STARTED", "SIGN_UP", "LOGIN", "FEATURE_WALKTHROUGH", "HOW_TO", "PRODUCT_TOUR", "ADMIN_GUIDE", "DEVELOPER_GUIDE", "TROUBLESHOOTING", "CUSTOM"
    val selectedFeature: String = "",
    val selectedWorkflow: String = "",
    val audience: String = "New users",
    val experienceLevel: String = "Beginner",
    val duration: String = "Standard — 1–3 minutes",
    val presentationType: String = "VOICE_ONLY", // "FACE_AND_VOICE", "VOICE_ONLY", "ILLUSTRATED"
    val presenterOption: String = "Presenter beside application",
    val voiceName: String = "Professional Male (Marcus)",
    val voiceGender: String = "Male",
    val voiceAccent: String = "Standard American",
    val speakingStyle: String = "Professional",
    val speakingSpeed: Float = 1.0f,
    val narrationLang: String = "English",
    val subtitleLang: String = "Twi",
    val detectedAppLang: String = "English (US)",
    val customTerminology: String = "SHARIF AI, SHARIF TECHNOLOGIES, Paystack",
    val hasSubtitles: Boolean = true,
    val subtitleStyle: String = "Professional",
    val subtitlePosition: String = "Bottom",
    val visualStyle: String = "Professional",
    val cursorStyle: String = "Highlighted",
    val calloutStyle: String = "Detailed",
    val zoomStyle: String = "Automatic",
    val transitionStyle: String = "Smooth",
    val illustrationStyle: String = "Educational",
    val backgroundMusic: String = "Subtle",
    val musicVolume: Float = 0.25f,
    val brandLogo: String = "CodeCast Default",
    val watermarkOption: String = "Logo",
    val status: String = "DRAFT", // "DRAFT", "GENERATING", "COMPLETED"
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tutorial_steps")
data class TutorialStepEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tutorialId: Int,
    val stepOrder: Int,
    val title: String,
    val screenName: String,
    val actionType: String,
    val instruction: String = "",
    val verificationStatus: String, // "RUNTIME_VERIFIED", "CODE_VERIFIED", "INFERRED", "UNABLE_TO_VERIFY"
    val evidenceSource: String = "",
    val evidenceElement: String = "",
    val isChecked: Boolean = true,
    val codeSnippet: String = "",
    val codeFilePath: String = ""
)

@Entity(tableName = "generated_scenes")
data class GeneratedSceneEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tutorialId: Int,
    val sceneOrder: Int,
    val screenDrawableName: String, // "demo_screen_auth", "demo_screen_dash", etc.
    val title: String,
    val narrationScript: String,
    val subtitleText: String,
    val durationSeconds: Int = 8,
    val zoomTarget: String = "Center",
    val calloutText: String = "",
    val transitionType: String = "Smooth Fade",
    val codeFilePath: String = "",
    val codeSnippet: String = "",
    val highlightedLines: String = "1,2",
    val terminalOutput: String = ""
)

@Entity(tableName = "project_versions")
data class ProjectVersionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val projectId: Int,
    val versionTag: String,
    val changelog: String,
    val affectedTutorialsCount: Int = 0,
    val isLatest: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
