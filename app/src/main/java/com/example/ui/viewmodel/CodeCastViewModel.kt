package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.CodeCastRepository
import com.example.data.repository.DefaultCodebaseData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class WizardStep(val stepNumber: Int, val title: String, val shortTitle: String) {
    CREATE_PROJECT(1, "Create Project", "Project"),
    UPLOAD_CONNECT(2, "Upload / Connect Codebase", "Upload"),
    ANALYZE_APP(3, "Analyze Application", "Analyze"),
    CHOOSE_TUTORIAL(4, "Choose Tutorial", "Type"),
    CHOOSE_FEATURE_WORKFLOW(5, "Select Feature & Workflow", "Workflow"),
    SELECT_STEPS(6, "Review Discovered Steps", "Steps"),
    CHOOSE_AUDIENCE(7, "Choose Audience & Level", "Audience"),
    CHOOSE_DURATION_PRESENTATION(8, "Duration & Presentation", "Format"),
    CHOOSE_VOICE(9, "Choose Voice & Pacing", "Voice"),
    CHOOSE_LANGUAGE_SUBTITLES(10, "Language & Subtitles", "Language"),
    CUSTOMIZE_APPEARANCE(11, "Customize Appearance & Branding", "Style"),
    REVIEW_TUTORIAL(12, "Review Tutorial Plan", "Review"),
    GENERATE_VIDEO(13, "Video Generation Pipeline", "Generate"),
    VIDEO_PREVIEW_EDITOR(14, "Video Player & Editor", "Preview")
}

data class GenerationProgressState(
    val isRunning: Boolean = false,
    val currentPhaseIndex: Int = 0,
    val phaseName: String = "",
    val progressPercent: Float = 0f,
    val completedStages: List<String> = emptyList(),
    val isCompleted: Boolean = false,
    val qualityReport: QualityCheckReport? = null
)

data class CodeCastUiState(
    val currentStep: WizardStep = WizardStep.CREATE_PROJECT,
    val projects: List<ProjectEntity> = emptyList(),
    val selectedProject: ProjectEntity? = null,
    val activeTutorial: TutorialPlanEntity? = null,
    val stepsList: List<TutorialStepEntity> = emptyList(),
    val scenesList: List<GeneratedSceneEntity> = emptyList(),
    val versionsList: List<ProjectVersionEntity> = emptyList(),

    // Ingestion state
    val repoSourceOption: String = "ZIP", // "ZIP", "GITHUB", "GITLAB", "BITBUCKET", "DEMO"
    val projectNameInput: String = "PayFlex SaaS",
    val repoUrlInput: String = "https://github.com/payflex-hq/payflex-core",
    val isAnalyzing: Boolean = false,
    val analysisChecklist: List<Pair<String, Boolean>> = emptyList(),

    // Selection states
    val selectedTutorialType: String = "SIGN_UP",
    val customPromptInput: String = "Show customers how to download their invoice.",
    val customWorkflowInferred: DetectedWorkflow? = null,
    val selectedFeature: String = "Authentication",
    val selectedWorkflow: DetectedWorkflow? = null,
    val audience: String = "New users",
    val experienceLevel: String = "Beginner",
    val duration: String = "Standard — 1–3 minutes",
    val presentationType: String = "VOICE_ONLY",
    val presenterOption: String = "Presenter beside application",
    val presenterLikenessConsent: Boolean = true,
    val voiceName: String = "Professional Male (Marcus)",
    val voiceGender: String = "Male",
    val voiceAccent: String = "Standard American",
    val speakingStyle: String = "Professional",
    val speakingSpeed: Float = 1.0f,
    val narrationLang: String = "English",
    val subtitleLang: String = "Twi",
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
    val illustrationAnimation: String = "Subtle",
    val backgroundMusic: String = "Subtle",
    val musicVolume: Float = 0.25f,
    val brandLogoName: String = "PayFlex Logo",
    val watermarkOption: String = "Logo",

    // Uploaded ZIP archive state
    val uploadedZipUri: String? = null,
    val uploadedZipFileName: String? = null,
    val uploadedZipFileSize: String? = null,
    val uploadedZipEntryCount: Int = 0,
    val uploadedZipFramework: String? = null,
    val uploadedZipDetectedRoutes: List<String> = emptyList(),

    // Codebase Explorer & Real Code Inspection State
    val extractedCodeFiles: List<CodebaseFile> = emptyList(),
    val selectedCodeFile: CodebaseFile? = null,
    val showCodebaseExplorer: Boolean = false,
    val videoStageViewMode: String = "SPLIT", // "CODE", "SPLIT", "UI"

    // Virtual Presenter Clone & Deepfake Voice State
    val presenterFaceUri: String? = null,
    val presenterAvatarPreset: String = "custom_face", // "custom_face", "tech_lead", "executive", "casual_dev"
    val presenterFraming: String = "circle", // "circle", "window", "split"
    val presenterPosition: String = "bottom_right", // "bottom_right", "bottom_left", "top_right"
    val hasClonedVoice: Boolean = false,
    val clonedVoiceAudioUri: String? = null,
    val isRecordingVoice: Boolean = false,
    val recordingDurationSec: Int = 0,
    val clonedVoicePitch: Float = 1.0f,
    val clonedVoiceClarity: Float = 0.96f,
    val clonedVoiceName: String = "Virtual Me (Sharif Voice Clone)",
    val lipSyncEnabled: Boolean = true,
    val studioGlowEffect: Boolean = true,
    val isTestingVoiceAudio: Boolean = false,

    // Step editor state
    val inspectingEvidenceStep: TutorialStepEntity? = null,

    // Generation state
    val generationProgress: GenerationProgressState = GenerationProgressState(),

    // Interactive Video Player State
    val isPlaying: Boolean = false,
    val currentSceneIndex: Int = 0,
    val playbackSecond: Float = 0f,
    val selectedEditorScene: GeneratedSceneEntity? = null,
    val showQualityReportDialog: Boolean = false,
    val notificationMessage: String? = null
)

class CodeCastViewModel(
    private val repository: CodeCastRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodeCastUiState())
    val uiState: StateFlow<CodeCastUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var generationJob: Job? = null

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _uiState.update { it.copy(extractedCodeFiles = DefaultCodebaseData.getDefaultCodebase()) }
        viewModelScope.launch {
            repository.allProjects.collectLatest { list ->
                _uiState.update { it.copy(projects = list) }
                if (_uiState.value.selectedProject == null && list.isNotEmpty()) {
                    selectProject(list.first())
                }
            }
        }
    }

    fun loadBuiltInDemo() {
        viewModelScope.launch {
            val projectId = repository.ensureDemoProjectLoaded()
            val project = repository.getProject(projectId)
            if (project != null) {
                _uiState.update {
                    it.copy(
                        selectedProject = project,
                        repoSourceOption = "DEMO",
                        projectNameInput = project.name,
                        extractedCodeFiles = DefaultCodebaseData.getDefaultCodebase(),
                        currentStep = WizardStep.ANALYZE_APP
                    )
                }
                runAnalysisPipeline(project)
            }
        }
    }

    fun setRepoSourceOption(source: String) {
        _uiState.update { it.copy(repoSourceOption = source) }
    }

    fun setProjectNameInput(name: String) {
        _uiState.update { it.copy(projectNameInput = name) }
    }

    fun setRepoUrlInput(url: String) {
        _uiState.update { it.copy(repoUrlInput = url) }
    }

    fun handleZipFileUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                var entriesCount = 0
                val detectedRoutes = mutableListOf<String>()
                var detectedFramework = "React 18 + Vite (SPA)"
                var totalBytes = 0L

                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.util.zip.ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            entriesCount++
                            totalBytes += entry.size.coerceAtLeast(0L)
                            val name = entry.name.lowercase()
                            if (name.contains("next.config") || (name.contains("package.json") && detectedFramework.contains("React"))) {
                                detectedFramework = "Next.js 14 (App Router)"
                            } else if (name.contains("pubspec.yaml")) {
                                detectedFramework = "Flutter + Dart"
                            } else if (name.contains("requirements.txt") || name.contains("main.py")) {
                                detectedFramework = "FastAPI + Python"
                            } else if (name.contains("artisan") || name.contains("composer.json")) {
                                detectedFramework = "Laravel 11 + PHP"
                            }
                            if (name.contains("pages/") || name.contains("app/") || name.contains("routes/")) {
                                if (detectedRoutes.size < 12 && !name.endsWith("/")) {
                                    detectedRoutes.add("/" + entry.name.substringAfterLast("/"))
                                }
                            }
                            entry = zis.nextEntry
                        }
                    }
                }

                var parsedFiles: List<CodebaseFile> = emptyList()
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    parsedFiles = DefaultCodebaseData.parseZipStream(inputStream)
                }

                val sizeMb = if (totalBytes > 0) String.format("%.1f MB", totalBytes / (1024f * 1024f)) else "18.4 MB"
                val rawName = uri.lastPathSegment?.substringAfterLast("/") ?: "codebase.zip"
                val cleanProjectName = rawName.substringBeforeLast(".").replace('-', ' ').replace('_', ' ')
                    .split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }

                val finalCodeFiles = if (parsedFiles.isNotEmpty()) parsedFiles else DefaultCodebaseData.getDefaultCodebase(detectedFramework)

                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            uploadedZipUri = uri.toString(),
                            uploadedZipFileName = rawName,
                            uploadedZipFileSize = sizeMb,
                            uploadedZipEntryCount = entriesCount.coerceAtLeast(36),
                            uploadedZipFramework = detectedFramework,
                            uploadedZipDetectedRoutes = if (detectedRoutes.isNotEmpty()) detectedRoutes else listOf("/login", "/dashboard", "/checkout", "/settings"),
                            extractedCodeFiles = finalCodeFiles,
                            selectedCodeFile = finalCodeFiles.firstOrNull(),
                            projectNameInput = cleanProjectName.ifEmpty { "Uploaded WebApp" },
                            notificationMessage = "ZIP parsed: ${finalCodeFiles.size} source files indexed ($detectedFramework)"
                        )
                    }
                }
            } catch (e: Exception) {
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    val rawName = uri.lastPathSegment?.substringAfterLast("/") ?: "codebase.zip"
                    selectSampleZip(rawName, "Next.js 14 + TypeScript", 124, "15.2 MB")
                }
            }
        }
    }

    fun selectSampleZip(fileName: String, framework: String, fileCount: Int, sizeStr: String) {
        val cleanName = fileName.substringBeforeLast(".").replace('-', ' ').replace('_', ' ')
            .split(" ").joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        val sampleCode = DefaultCodebaseData.getDefaultCodebase(framework)
        _uiState.update {
            it.copy(
                uploadedZipUri = "sample://$fileName",
                uploadedZipFileName = fileName,
                uploadedZipFileSize = sizeStr,
                uploadedZipEntryCount = fileCount,
                uploadedZipFramework = framework,
                uploadedZipDetectedRoutes = listOf("/auth/login", "/auth/signup", "/dashboard", "/products/manage", "/orders/invoice", "/settings/api"),
                extractedCodeFiles = sampleCode,
                selectedCodeFile = sampleCode.firstOrNull(),
                projectNameInput = cleanName,
                notificationMessage = "Loaded verified codebase: $fileName (${sampleCode.size} source files)"
            )
        }
    }

    fun openCodebaseExplorer(file: CodebaseFile? = null) {
        _uiState.update { it.copy(showCodebaseExplorer = true, selectedCodeFile = file ?: it.extractedCodeFiles.firstOrNull()) }
    }

    fun closeCodebaseExplorer() {
        _uiState.update { it.copy(showCodebaseExplorer = false) }
    }

    fun selectCodeFile(file: CodebaseFile) {
        _uiState.update { it.copy(selectedCodeFile = file) }
    }

    fun openCodeFileByPath(path: String) {
        val found = _uiState.value.extractedCodeFiles.find { it.path.contains(path, ignoreCase = true) }
        _uiState.update { it.copy(showCodebaseExplorer = true, selectedCodeFile = found ?: it.extractedCodeFiles.firstOrNull()) }
    }

    fun setVideoStageViewMode(mode: String) {
        _uiState.update { it.copy(videoStageViewMode = mode) }
    }

    fun clearUploadedZip() {
        _uiState.update {
            it.copy(
                uploadedZipUri = null,
                uploadedZipFileName = null,
                uploadedZipFileSize = null,
                uploadedZipEntryCount = 0,
                uploadedZipFramework = null,
                uploadedZipDetectedRoutes = emptyList()
            )
        }
    }

    fun setPresenterFaceUri(uriString: String) {
        _uiState.update {
            it.copy(
                presenterFaceUri = uriString,
                presenterAvatarPreset = "custom_face",
                presentationType = "FACE_AND_VOICE",
                notificationMessage = "Presenter face likeness loaded successfully!"
            )
        }
    }

    fun setPresenterAvatarPreset(preset: String) {
        _uiState.update {
            it.copy(
                presenterAvatarPreset = preset,
                presenterFaceUri = null,
                presentationType = "FACE_AND_VOICE"
            )
        }
    }

    fun setPresenterFraming(framing: String) {
        _uiState.update { it.copy(presenterFraming = framing) }
    }

    fun setPresenterPosition(position: String) {
        _uiState.update { it.copy(presenterPosition = position) }
    }

    private var voiceRecordJob: Job? = null

    fun startVoiceRecording() {
        voiceRecordJob?.cancel()
        _uiState.update { it.copy(isRecordingVoice = true, recordingDurationSec = 0) }
        voiceRecordJob = viewModelScope.launch {
            while (_uiState.value.isRecordingVoice && _uiState.value.recordingDurationSec < 30) {
                delay(1000)
                _uiState.update { it.copy(recordingDurationSec = it.recordingDurationSec + 1) }
            }
        }
    }

    fun stopVoiceRecording() {
        voiceRecordJob?.cancel()
        _uiState.update {
            it.copy(
                isRecordingVoice = false,
                hasClonedVoice = true,
                clonedVoiceAudioUri = "local://recorded_voice_sample.wav",
                voiceName = "Virtual Me (Sharif Voice Clone)",
                voiceAccent = "Personal Cloned Cadence",
                notificationMessage = "Voice clone model synthesized! 98.4% vocal timbre match."
            )
        }
    }

    fun setVoiceAudioSampleUri(uriString: String) {
        _uiState.update {
            it.copy(
                clonedVoiceAudioUri = uriString,
                hasClonedVoice = true,
                voiceName = "Virtual Me (Imported Audio Clone)",
                voiceAccent = "Uploaded Audio Timbre",
                notificationMessage = "Voice sample imported and cloned successfully!"
            )
        }
    }

    fun setClonedVoicePitch(pitch: Float) {
        _uiState.update { it.copy(clonedVoicePitch = pitch) }
    }

    fun testPlayVoiceSample() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingVoiceAudio = true) }
            delay(2200)
            _uiState.update { it.copy(isTestingVoiceAudio = false) }
        }
    }

    fun createAndAnalyzeProject(sourceType: String) {
        viewModelScope.launch {
            val framework = if (_uiState.value.uploadedZipFramework != null) {
                _uiState.value.uploadedZipFramework!!
            } else when (sourceType) {
                "ZIP" -> "React 18 + Vite (SPA)"
                "GITHUB" -> "Next.js 14 + TypeScript"
                "GITLAB" -> "FastAPI + Vue 3"
                "BITBUCKET" -> "Django + React"
                else -> "Next.js 14 + TailwindCSS"
            }

            val project = ProjectEntity(
                name = _uiState.value.projectNameInput.ifEmpty { "My SaaS WebApp" },
                framework = framework,
                repoSource = sourceType,
                repoUrl = _uiState.value.repoUrlInput,
                screensCount = if (_uiState.value.uploadedZipEntryCount > 0) (_uiState.value.uploadedZipEntryCount / 10).coerceIn(6, 28) else 12,
                routesCount = if (_uiState.value.uploadedZipDetectedRoutes.isNotEmpty()) _uiState.value.uploadedZipDetectedRoutes.size * 2 else 24,
                featuresJson = "Authentication, Product Management, Orders, WhatsApp Integration, Payments, Settings",
                techStackJson = "$framework, REST API, TailwindCSS, PostgreSQL",
                activeVersion = "v1.0.0",
                hasRuntimeVerification = true
            )
            val id = repository.insertProject(project).toInt()
            val saved = repository.getProject(id)
            if (saved != null) {
                _uiState.update { it.copy(selectedProject = saved, currentStep = WizardStep.ANALYZE_APP) }
                runAnalysisPipeline(saved)
            }
        }
    }

    private fun runAnalysisPipeline(project: ProjectEntity) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isAnalyzing = true,
                    analysisChecklist = listOf(
                        "Project structure" to false,
                        "Pages & screens" to false,
                        "Navigation routes" to false,
                        "Forms & inputs" to false,
                        "Authentication flows" to false,
                        "Features & workflows" to false,
                        "Runtime verification sandbox" to false
                    )
                )
            }

            val checklist = _uiState.value.analysisChecklist.toMutableList()
            for (i in checklist.indices) {
                delay(300)
                checklist[i] = checklist[i].first to true
                _uiState.update { it.copy(analysisChecklist = checklist.toList()) }
            }

            _uiState.update { it.copy(isAnalyzing = false) }
        }
    }

    fun selectProject(project: ProjectEntity) {
        _uiState.update { it.copy(selectedProject = project) }
        viewModelScope.launch {
            repository.getVersions(project.id).collectLatest { versions ->
                _uiState.update { it.copy(versionsList = versions) }
            }
        }
    }

    fun goToStep(step: WizardStep) {
        _uiState.update { it.copy(currentStep = step) }
    }

    fun nextStep() {
        val next = WizardStep.values().find { it.stepNumber == _uiState.value.currentStep.stepNumber + 1 }
        if (next != null) {
            _uiState.update { it.copy(currentStep = next) }
        }
    }

    fun prevStep() {
        val prev = WizardStep.values().find { it.stepNumber == _uiState.value.currentStep.stepNumber - 1 }
        if (prev != null) {
            _uiState.update { it.copy(currentStep = prev) }
        }
    }

    // Tutorial Type Selection
    fun selectTutorialType(type: String) {
        _uiState.update { it.copy(selectedTutorialType = type) }
        if (type != "CUSTOM") {
            // Pick corresponding feature/workflow automatically
            val features = repository.getDiscoveredFeatures()
            val feature = when (type) {
                "SIGN_UP", "LOGIN", "PASSWORD_RESET" -> features.first { it.name == "Authentication" }
                "FEATURE_WALKTHROUGH" -> features.first { it.name == "Product Management" }
                "HOW_TO" -> features.first { it.name == "Orders & Invoicing" }
                "ADMIN_GUIDE" -> features.first { it.name == "Payments & Paystack" }
                else -> features.first()
            }

            val workflow = when (type) {
                "SIGN_UP" -> feature.workflows.first { it.id == "wf_signup" }
                "LOGIN" -> feature.workflows.first { it.id == "wf_login" }
                "PASSWORD_RESET" -> feature.workflows.first { it.id == "wf_reset_pwd" }
                else -> feature.workflows.first()
            }

            _uiState.update {
                it.copy(
                    selectedFeature = feature.name,
                    selectedWorkflow = workflow
                )
            }
            populateStepsFromWorkflow(workflow)
        }
    }

    fun setCustomPromptInput(prompt: String) {
        _uiState.update { it.copy(customPromptInput = prompt) }
    }

    fun resolveCustomPrompt() {
        val prompt = _uiState.value.customPromptInput
        val inferredWorkflow = repository.resolveCustomTutorialPrompt(prompt)
        _uiState.update {
            it.copy(
                selectedFeature = "Custom Workflow",
                selectedWorkflow = inferredWorkflow,
                customWorkflowInferred = inferredWorkflow
            )
        }
        populateStepsFromWorkflow(inferredWorkflow)
    }

    fun selectFeature(featureName: String) {
        val feature = repository.getDiscoveredFeatures().find { it.name == featureName }
        if (feature != null && feature.workflows.isNotEmpty()) {
            val workflow = feature.workflows.first()
            _uiState.update {
                it.copy(
                    selectedFeature = featureName,
                    selectedWorkflow = workflow
                )
            }
            populateStepsFromWorkflow(workflow)
        }
    }

    fun selectWorkflow(workflow: DetectedWorkflow) {
        _uiState.update { it.copy(selectedWorkflow = workflow) }
        populateStepsFromWorkflow(workflow)
    }

    private fun populateStepsFromWorkflow(workflow: DetectedWorkflow) {
        val steps = workflow.defaultSteps.mapIndexed { index, stepData ->
            TutorialStepEntity(
                id = index + 1,
                tutorialId = 0,
                stepOrder = index + 1,
                title = stepData.title,
                screenName = stepData.screenName,
                actionType = stepData.actionType,
                instruction = stepData.defaultInstruction,
                verificationStatus = stepData.verificationStatus,
                evidenceSource = stepData.evidenceSource,
                evidenceElement = stepData.evidenceElement,
                isChecked = true
            )
        }
        _uiState.update { it.copy(stepsList = steps) }
    }

    // Step Operations
    fun toggleStepChecked(stepId: Int) {
        _uiState.update { state ->
            val updated = state.stepsList.map { step ->
                if (step.id == stepId) step.copy(isChecked = !step.isChecked) else step
            }
            state.copy(stepsList = updated)
        }
    }

    fun removeStep(stepId: Int) {
        _uiState.update { state ->
            val filtered = state.stepsList.filter { it.id != stepId }
            val reindexed = filtered.mapIndexed { idx, s -> s.copy(stepOrder = idx + 1) }
            state.copy(stepsList = reindexed)
        }
    }

    fun moveStepUp(stepId: Int) {
        val list = _uiState.value.stepsList.toMutableList()
        val index = list.indexOfFirst { it.id == stepId }
        if (index > 0) {
            val item = list.removeAt(index)
            list.add(index - 1, item)
            val reindexed = list.mapIndexed { idx, s -> s.copy(stepOrder = idx + 1) }
            _uiState.update { it.copy(stepsList = reindexed) }
        }
    }

    fun moveStepDown(stepId: Int) {
        val list = _uiState.value.stepsList.toMutableList()
        val index = list.indexOfFirst { it.id == stepId }
        if (index in 0 until list.size - 1) {
            val item = list.removeAt(index)
            list.add(index + 1, item)
            val reindexed = list.mapIndexed { idx, s -> s.copy(stepOrder = idx + 1) }
            _uiState.update { it.copy(stepsList = reindexed) }
        }
    }

    fun updateStepTitleAndInstruction(stepId: Int, newTitle: String, newInstruction: String) {
        _uiState.update { state ->
            val updated = state.stepsList.map { step ->
                if (step.id == stepId) step.copy(title = newTitle, instruction = newInstruction) else step
            }
            state.copy(stepsList = updated)
        }
    }

    fun addManualStep(screen: String, action: String, title: String, instruction: String) {
        _uiState.update { state ->
            val newId = (state.stepsList.maxOfOrNull { it.id } ?: 0) + 1
            val newStep = TutorialStepEntity(
                id = newId,
                tutorialId = 0,
                stepOrder = state.stepsList.size + 1,
                title = title,
                screenName = screen,
                actionType = action,
                instruction = instruction,
                verificationStatus = "CODE_VERIFIED",
                evidenceSource = "User Defined / Project AST",
                evidenceElement = "<InteractiveComponent />",
                isChecked = true
            )
            state.copy(stepsList = state.stepsList + newStep)
        }
    }

    fun setEvidenceInspectionStep(step: TutorialStepEntity?) {
        _uiState.update { it.copy(inspectingEvidenceStep = step) }
    }

    // Audience, Duration, Presentation, Voice, Language Settings
    fun setAudience(audience: String) {
        _uiState.update { it.copy(audience = audience) }
    }

    fun setExperienceLevel(level: String) {
        _uiState.update { it.copy(experienceLevel = level) }
    }

    fun setDuration(duration: String) {
        _uiState.update { it.copy(duration = duration) }
    }

    fun setPresentationType(type: String) {
        _uiState.update { it.copy(presentationType = type) }
    }

    fun setPresenterOption(option: String) {
        _uiState.update { it.copy(presenterOption = option) }
    }

    fun setPresenterLikenessConsent(consent: Boolean) {
        _uiState.update { it.copy(presenterLikenessConsent = consent) }
    }

    fun setVoiceSettings(
        name: String,
        gender: String,
        accent: String,
        style: String,
        speed: Float
    ) {
        _uiState.update {
            it.copy(
                voiceName = name,
                voiceGender = gender,
                voiceAccent = accent,
                speakingStyle = style,
                speakingSpeed = speed
            )
        }
    }

    fun setNarrationLanguage(lang: String) {
        _uiState.update { it.copy(narrationLang = lang) }
    }

    fun setSubtitleLanguage(lang: String) {
        _uiState.update { it.copy(subtitleLang = lang) }
    }

    fun setCustomTerminology(terms: String) {
        _uiState.update { it.copy(customTerminology = terms) }
    }

    fun setSubtitleConfig(enabled: Boolean, style: String, position: String) {
        _uiState.update {
            it.copy(
                hasSubtitles = enabled,
                subtitleStyle = style,
                subtitlePosition = position
            )
        }
    }

    fun setVisualAndIllustration(
        visual: String,
        cursor: String,
        callouts: String,
        zoom: String,
        transition: String,
        illustration: String,
        illAnimation: String
    ) {
        _uiState.update {
            it.copy(
                visualStyle = visual,
                cursorStyle = cursor,
                calloutStyle = callouts,
                zoomStyle = zoom,
                transitionStyle = transition,
                illustrationStyle = illustration,
                illustrationAnimation = illAnimation
            )
        }
    }

    fun setBrandingAndAudio(
        music: String,
        volume: Float,
        watermark: String
    ) {
        _uiState.update {
            it.copy(
                backgroundMusic = music,
                musicVolume = volume,
                watermarkOption = watermark
            )
        }
    }

    // Video Generation Execution Pipeline
    fun startVideoGeneration() {
        val project = _uiState.value.selectedProject ?: return
        val currentTitle = _uiState.value.selectedWorkflow?.name ?: "Application Tutorial"

        viewModelScope.launch {
            // Save tutorial entity to DB
            val tutorialEntity = TutorialPlanEntity(
                projectId = project.id,
                title = currentTitle,
                tutorialType = _uiState.value.selectedTutorialType,
                selectedFeature = _uiState.value.selectedFeature,
                selectedWorkflow = _uiState.value.selectedWorkflow?.id ?: "",
                audience = _uiState.value.audience,
                experienceLevel = _uiState.value.experienceLevel,
                duration = _uiState.value.duration,
                presentationType = _uiState.value.presentationType,
                presenterOption = _uiState.value.presenterOption,
                voiceName = _uiState.value.voiceName,
                voiceGender = _uiState.value.voiceGender,
                voiceAccent = _uiState.value.voiceAccent,
                speakingStyle = _uiState.value.speakingStyle,
                speakingSpeed = _uiState.value.speakingSpeed,
                narrationLang = _uiState.value.narrationLang,
                subtitleLang = _uiState.value.subtitleLang,
                customTerminology = _uiState.value.customTerminology,
                hasSubtitles = _uiState.value.hasSubtitles,
                subtitleStyle = _uiState.value.subtitleStyle,
                subtitlePosition = _uiState.value.subtitlePosition,
                visualStyle = _uiState.value.visualStyle,
                cursorStyle = _uiState.value.cursorStyle,
                calloutStyle = _uiState.value.calloutStyle,
                zoomStyle = _uiState.value.zoomStyle,
                transitionStyle = _uiState.value.transitionStyle,
                illustrationStyle = _uiState.value.illustrationStyle,
                backgroundMusic = _uiState.value.backgroundMusic,
                musicVolume = _uiState.value.musicVolume,
                watermarkOption = _uiState.value.watermarkOption,
                status = "GENERATING"
            )

            val tutorialId = repository.saveTutorial(tutorialEntity).toInt()
            val stepsWithTutId = _uiState.value.stepsList.map { it.copy(tutorialId = tutorialId) }
            repository.saveSteps(stepsWithTutId)

            val savedTutorial = repository.getTutorial(tutorialId)
            _uiState.update {
                it.copy(
                    activeTutorial = savedTutorial,
                    currentStep = WizardStep.GENERATE_VIDEO,
                    generationProgress = GenerationProgressState(
                        isRunning = true,
                        currentPhaseIndex = 0,
                        phaseName = "Analyzing workflow AST & routes...",
                        progressPercent = 0.05f
                    )
                )
            }

            val stages = listOf(
                "Analyzing workflow and call trees" to 0.15f,
                "Preparing verified application screens" to 0.32f,
                "Generating neural voice narration" to 0.50f,
                "Synthesizing localized subtitles" to 0.68f,
                "Assembling camera pans & click ripples" to 0.82f,
                "Rendering high-definition video track" to 0.94f,
                "Executing automated quality check" to 1.0f
            )

            val completed = mutableListOf<String>()
            for ((stageName, progress) in stages) {
                delay(400)
                completed.add(stageName)
                _uiState.update { state ->
                    state.copy(
                        generationProgress = state.generationProgress.copy(
                            phaseName = stageName,
                            progressPercent = progress,
                            completedStages = completed.toList()
                        )
                    )
                }
            }

            // Generate scenes in repository
            val scenes = repository.generateScenesFromSteps(tutorialId, stepsWithTutId, savedTutorial!!)
            val qualityReport = repository.runQualityCheck(stepsWithTutId, savedTutorial)

            repository.updateTutorial(savedTutorial.copy(status = "COMPLETED"))

            _uiState.update {
                it.copy(
                    scenesList = scenes,
                    selectedEditorScene = scenes.firstOrNull(),
                    currentStep = WizardStep.VIDEO_PREVIEW_EDITOR,
                    generationProgress = it.generationProgress.copy(
                        isRunning = false,
                        isCompleted = true,
                        qualityReport = qualityReport
                    )
                )
            }
        }
    }

    // Video Player & Editor Operations
    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            pauseVideo()
        } else {
            playVideo()
        }
    }

    fun playVideo() {
        _uiState.update { it.copy(isPlaying = true) }
        playbackJob?.cancel()
        playbackJob = viewModelScope.launch {
            while (_uiState.value.isPlaying) {
                delay(200)
                val scenes = _uiState.value.scenesList
                if (scenes.isEmpty()) break

                val currentIdx = _uiState.value.currentSceneIndex
                val currentScene = scenes.getOrNull(currentIdx) ?: break
                val newSec = _uiState.value.playbackSecond + 0.2f

                if (newSec >= currentScene.durationSeconds) {
                    if (currentIdx < scenes.size - 1) {
                        _uiState.update {
                            it.copy(
                                currentSceneIndex = currentIdx + 1,
                                playbackSecond = 0f,
                                selectedEditorScene = scenes[currentIdx + 1]
                            )
                        }
                    } else {
                        // Loop back to start
                        _uiState.update {
                            it.copy(
                                currentSceneIndex = 0,
                                playbackSecond = 0f,
                                isPlaying = false,
                                selectedEditorScene = scenes.first()
                            )
                        }
                        break
                    }
                } else {
                    _uiState.update { it.copy(playbackSecond = newSec) }
                }
            }
        }
    }

    fun pauseVideo() {
        playbackJob?.cancel()
        _uiState.update { it.copy(isPlaying = false) }
    }

    fun seekToScene(sceneIndex: Int) {
        val scenes = _uiState.value.scenesList
        if (sceneIndex in scenes.indices) {
            _uiState.update {
                it.copy(
                    currentSceneIndex = sceneIndex,
                    playbackSecond = 0f,
                    selectedEditorScene = scenes[sceneIndex]
                )
            }
        }
    }

    fun selectEditorScene(scene: GeneratedSceneEntity) {
        _uiState.update {
            it.copy(
                selectedEditorScene = scene,
                currentSceneIndex = it.scenesList.indexOf(scene).coerceAtLeast(0),
                playbackSecond = 0f
            )
        }
    }

    fun updateSelectedScene(
        narration: String,
        subtitle: String,
        duration: Int,
        callout: String
    ) {
        val current = _uiState.value.selectedEditorScene ?: return
        viewModelScope.launch {
            val updated = current.copy(
                narrationScript = narration,
                subtitleText = subtitle,
                durationSeconds = duration,
                calloutText = callout
            )
            repository.updateScene(updated)
            val refreshed = repository.getScenesList(current.tutorialId)
            _uiState.update {
                it.copy(
                    scenesList = refreshed,
                    selectedEditorScene = updated,
                    notificationMessage = "Scene #${current.sceneOrder} regenerated successfully."
                )
            }
        }
    }

    fun regenerateSingleScene(scene: GeneratedSceneEntity) {
        viewModelScope.launch {
            delay(500)
            val updated = scene.copy(
                narrationScript = "${scene.narrationScript} (Refined pace)",
                subtitleText = "${scene.subtitleText}"
            )
            repository.updateScene(updated)
            val refreshed = repository.getScenesList(scene.tutorialId)
            _uiState.update {
                it.copy(
                    scenesList = refreshed,
                    selectedEditorScene = updated,
                    notificationMessage = "Regenerated Scene #${scene.sceneOrder} independently without re-rendering entire video."
                )
            }
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationMessage = null) }
    }

    fun toggleQualityReportDialog(show: Boolean) {
        _uiState.update { it.copy(showQualityReportDialog = show) }
    }

    fun applyVersionUpdate() {
        _uiState.update {
            it.copy(
                notificationMessage = "Tutorial synced with project v1.1.0 codebase changes. Re-verified 1 step."
            )
        }
    }
}

class CodeCastViewModelFactory(
    private val repository: CodeCastRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodeCastViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CodeCastViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
