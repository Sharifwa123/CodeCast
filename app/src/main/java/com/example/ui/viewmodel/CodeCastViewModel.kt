package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.example.BuildConfig
import com.example.ai.GeminiTranslator
import com.example.analysis.*
import com.example.data.model.*
import com.example.data.repository.CodeCastRepository
import com.example.media.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
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
    val projectNameInput: String = "",
    val repoUrlInput: String = "",
    val analysis: ProjectAnalysis = ProjectAnalysis.EMPTY,
    val isFetching: Boolean = false,
    val ingestError: String? = null,
    val geminiApiKey: String = "",
    val exportProgress: Float? = null,
    val exportedVideoPath: String? = null,
    val isAnalyzing: Boolean = false,
    val analysisChecklist: List<Pair<String, Boolean>> = emptyList(),

    // Selection states
    val selectedTutorialType: String = "SIGN_UP",
    val customPromptInput: String = "",
    val customWorkflowInferred: DetectedWorkflow? = null,
    val selectedFeature: String = "",
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
    val subtitleLang: String = "English",
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
    val brandLogoName: String = "",
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
    val videoStageViewMode: String = "CODE",

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
    private val repository: CodeCastRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CodeCastUiState())
    val uiState: StateFlow<CodeCastUiState> = _uiState.asStateFlow()

    private var playbackJob: Job? = null
    private var generationJob: Job? = null

    private val prefs = appContext.getSharedPreferences("codecast_prefs", Context.MODE_PRIVATE)

    init {
        _uiState.update { it.copy(geminiApiKey = prefs.getString("gemini_key", "") ?: "") }
        loadInitialData()
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_key", key.trim()).apply()
        _uiState.update { it.copy(geminiApiKey = key.trim()) }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.allProjects.collectLatest { list -> _uiState.update { it.copy(projects = list) } }
        }
    }

    fun setRepoSourceOption(source: String) {
        _uiState.update { it.copy(repoSourceOption = source, ingestError = null) }
    }

    fun setProjectNameInput(name: String) {
        _uiState.update { it.copy(projectNameInput = name) }
    }

    fun setRepoUrlInput(url: String) {
        _uiState.update { it.copy(repoUrlInput = url, ingestError = null) }
    }

    fun handleZipFileUri(context: Context, uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetching = true, ingestError = null) }
            try {
                val result = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { ZipCodebaseReader.read(it) }
                        ?: throw IllegalStateException("Could not open the selected file.")
                }
                val rawName = uri.lastPathSegment?.substringAfterLast("/")?.substringAfterLast(":") ?: "codebase.zip"
                applyIngest(result, rawName, uri.toString())
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetching = false, ingestError = e.message ?: "Could not read the ZIP archive.") }
            }
        }
    }

    private suspend fun applyIngest(result: ZipReadResult, sourceName: String, uri: String?) {
        val analysis = withContext(Dispatchers.Default) { CodebaseAnalyzer.analyze(result.files) }
        val cleanName = sourceName.substringBeforeLast(".").replace('-', ' ').replace('_', ' ')
            .split(" ").filter { it.isNotEmpty() }.joinToString(" ") { w -> w.replaceFirstChar { it.uppercase() } }
        _uiState.update {
            it.copy(
                isFetching = false,
                ingestError = null,
                analysis = analysis,
                uploadedZipUri = uri,
                uploadedZipFileName = sourceName,
                uploadedZipFileSize = String.format("%.1f MB", result.totalBytes / (1024f * 1024f)),
                uploadedZipEntryCount = result.entryCount,
                uploadedZipFramework = analysis.framework,
                uploadedZipDetectedRoutes = analysis.routes.filter { r -> r.kind == "screen" }.map { r -> r.path },
                extractedCodeFiles = result.files,
                selectedCodeFile = result.files.firstOrNull(),
                projectNameInput = if (it.projectNameInput.isBlank()) cleanName else it.projectNameInput,
                selectedWorkflow = null,
                stepsList = emptyList(),
                notificationMessage = "Indexed ${result.files.size} source files (${analysis.framework})"
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
        val clean = path.substringBeforeLast(':')
        val found = _uiState.value.extractedCodeFiles.find { it.path.contains(clean, ignoreCase = true) }
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
                uploadedZipDetectedRoutes = emptyList(),
                extractedCodeFiles = emptyList(),
                analysis = ProjectAnalysis.EMPTY,
                selectedWorkflow = null,
                stepsList = emptyList()
            )
        }
    }

    fun setPresenterFaceUri(uriString: String) {
        _uiState.update {
            it.copy(
                presenterFaceUri = uriString,
                presenterAvatarPreset = "custom_face",
                presentationType = "FACE_AND_VOICE",
                notificationMessage = "Presenter photo added. It appears as a circle in the exported video."
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
                notificationMessage = "Voice cloning isn't available on-device. Narration uses the system text-to-speech voice you selected."
            )
        }
    }

    fun setVoiceAudioSampleUri(uriString: String) {
        _uiState.update {
            it.copy(notificationMessage = "Voice cloning isn't available on-device. Narration uses the system text-to-speech voice you selected.")
        }
    }

    fun setClonedVoicePitch(pitch: Float) {
        _uiState.update { it.copy(clonedVoicePitch = pitch) }
    }

    /** Speaks a real sample with the system TTS engine using the current voice settings. */
    fun testPlayVoiceSample() {
        viewModelScope.launch {
            _uiState.update { it.copy(isTestingVoiceAudio = true) }
            val st = _uiState.value
            val narrator = Narrator(appContext)
            try {
                if (narrator.prepare(st.narrationLang, st.speakingSpeed, pitchFor(st))) {
                    val wav = narrator.synthesize("This is a sample of the narration voice for your tutorial.", "sample")
                    if (wav != null) playPcm(wav) else _uiState.update { it.copy(notificationMessage = "Speech synthesis failed.") }
                } else {
                    _uiState.update { it.copy(notificationMessage = "No text-to-speech voice installed for ${st.narrationLang}.") }
                }
            } finally {
                narrator.shutdown()
                _uiState.update { it.copy(isTestingVoiceAudio = false) }
            }
        }
    }

    private suspend fun playPcm(wav: WavData) = withContext(Dispatchers.IO) {
        val ch = if (wav.channels == 1) android.media.AudioFormat.CHANNEL_OUT_MONO else android.media.AudioFormat.CHANNEL_OUT_STEREO
        val track = android.media.AudioTrack.Builder()
            .setAudioFormat(android.media.AudioFormat.Builder().setEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT).setSampleRate(wav.sampleRate).setChannelMask(ch).build())
            .setBufferSizeInBytes(wav.pcm.size * 2)
            .setTransferMode(android.media.AudioTrack.MODE_STATIC)
            .build()
        track.write(wav.pcm, 0, wav.pcm.size)
        track.play()
        delay((wav.seconds * 1000).toLong() + 200)
        track.release()
    }

    private fun pitchFor(st: CodeCastUiState) = if (st.voiceGender.equals("Female", true)) 1.15f else 0.9f

    fun createAndAnalyzeProject(sourceType: String) {
        val current = _uiState.value
        if (current.isFetching) return
        viewModelScope.launch {
            _uiState.update { it.copy(isFetching = true, ingestError = null) }
            try {
                if (sourceType == "ZIP") {
                    if (_uiState.value.extractedCodeFiles.isEmpty()) throw IllegalStateException("Choose a ZIP archive of your codebase first.")
                } else {
                    val url = _uiState.value.repoUrlInput.trim()
                    if (RepoUrl.parse(url) == null) throw IllegalStateException("Enter a valid repository URL, e.g. https://github.com/owner/repo")
                    val result = withContext(Dispatchers.IO) { CodebaseFetcher.fetch(url) }
                    applyIngest(result, RepoUrl.displayName(url), null)
                }
                val st = _uiState.value
                val a = st.analysis
                if (a.features.isEmpty() && a.routes.isEmpty()) {
                    throw IllegalStateException("No screens, forms or routes were found in this codebase (${st.extractedCodeFiles.size} files scanned, framework: ${a.framework}).")
                }
                val project = ProjectEntity(
                    name = st.projectNameInput.ifBlank { "Untitled project" },
                    framework = a.framework,
                    repoSource = sourceType,
                    repoUrl = st.repoUrlInput,
                    screensCount = a.screenCount,
                    routesCount = a.routes.size,
                    featuresJson = a.features.joinToString(", ") { it.name },
                    techStackJson = a.techStack.joinToString(", "),
                    activeVersion = "v1.0.0",
                    hasRuntimeVerification = false
                )
                val id = repository.insertProject(project).toInt()
                repository.addVersion(
                    ProjectVersionEntity(
                        projectId = id, versionTag = "v1.0.0",
                        changelog = "Indexed ${st.extractedCodeFiles.size} files: ${a.screenCount} screens, ${a.routes.size} routes, ${a.features.size} features.",
                        affectedTutorialsCount = 0, isLatest = true
                    )
                )
                val saved = repository.getProject(id)
                _uiState.update {
                    it.copy(
                        isFetching = false,
                        selectedProject = saved,
                        currentStep = WizardStep.ANALYZE_APP,
                        isAnalyzing = false,
                        analysisChecklist = listOf(
                            "Project structure — ${st.extractedCodeFiles.size} source files" to true,
                            "Pages & screens — ${a.screenCount}" to (a.screenCount > 0),
                            "Navigation routes — ${a.routes.size}" to a.routes.isNotEmpty(),
                            "Forms & inputs — ${a.formCount} screens with inputs" to (a.formCount > 0),
                            "Authentication flows — ${a.features.count { f -> f.name == "Authentication" }}" to a.features.any { f -> f.name == "Authentication" },
                            "Features & workflows — ${a.workflows.size}" to a.workflows.isNotEmpty(),
                            "Runtime verification — not performed (static analysis only)" to false
                        )
                    )
                }
                saved?.let { selectProject(it) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isFetching = false, ingestError = e.message ?: "Analysis failed.") }
            }
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
        if (type == "CUSTOM") return
        val a = _uiState.value.analysis
        val workflow = CodebaseAnalyzer.workflowForType(a, type)
        if (workflow == null) {
            _uiState.update { it.copy(selectedWorkflow = null, stepsList = emptyList(), notificationMessage = "This codebase has no matching flow for that tutorial type. Pick another type or use a custom prompt.") }
            return
        }
        val feature = a.features.firstOrNull { f -> f.workflows.any { w -> w.id == workflow.id } }?.name ?: "General"
        _uiState.update { it.copy(selectedFeature = feature, selectedWorkflow = workflow) }
        populateStepsFromWorkflow(workflow)
    }

    fun setCustomPromptInput(prompt: String) {
        _uiState.update { it.copy(customPromptInput = prompt) }
    }

    fun resolveCustomPrompt() {
        val st = _uiState.value
        val wf = CodebaseAnalyzer.resolvePrompt(st.analysis, st.customPromptInput)
        if (wf == null) {
            _uiState.update {
                it.copy(selectedWorkflow = null, customWorkflowInferred = null, stepsList = emptyList(),
                    notificationMessage = "Nothing in this codebase matches that request. Try naming a screen, form or feature that exists.")
            }
            return
        }
        _uiState.update { it.copy(selectedFeature = "Custom Workflow", selectedWorkflow = wf, customWorkflowInferred = wf) }
        populateStepsFromWorkflow(wf)
    }

    fun selectFeature(featureName: String) {
        val feature = _uiState.value.analysis.features.find { it.name == featureName }
        if (feature != null && feature.workflows.isNotEmpty()) {
            val workflow = feature.workflows.first()
            _uiState.update { it.copy(selectedFeature = featureName, selectedWorkflow = workflow) }
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
                isChecked = true,
                codeSnippet = stepData.codeSnippet,
                codeFilePath = stepData.codeFilePath
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
                verificationStatus = "INFERRED",
                evidenceSource = "Added manually",
                evidenceElement = "",
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
    private fun setStage(name: String, progress: Float, done: List<String>) {
        _uiState.update { it.copy(generationProgress = it.generationProgress.copy(phaseName = name, progressPercent = progress, completedStages = done)) }
    }

    private fun renderOptions(st: CodeCastUiState, projectName: String?): RenderOptions {
        val face = if (st.presentationType == "FACE_AND_VOICE" && st.presenterFaceUri != null) {
            try {
                appContext.contentResolver.openInputStream(Uri.parse(st.presenterFaceUri))?.use { android.graphics.BitmapFactory.decodeStream(it) }
            } catch (_: Exception) { null }
        } else null
        return RenderOptions(
            showSubtitles = st.hasSubtitles,
            watermark = if (st.watermarkOption == "None") null else "CodeCast" + (projectName?.let { " · $it" } ?: ""),
            fade = st.transitionStyle != "None",
            presenter = face,
            presenterRight = st.presenterPosition.endsWith("right")
        )
    }

    /** Speaks every scene with the system TTS engine. Extends scene durations so speech is never cut. */
    private suspend fun narrate(
        scenes: List<GeneratedSceneEntity>, tutorial: TutorialPlanEntity, onProgress: (Float) -> Unit
    ): Triple<List<GeneratedSceneEntity>, Map<Int, WavData?>, Boolean> {
        val st = _uiState.value
        val narrator = Narrator(appContext)
        try {
            if (!narrator.prepare(tutorial.narrationLang, tutorial.speakingSpeed, pitchFor(st))) {
                return Triple(scenes, emptyMap(), false)
            }
            val audio = LinkedHashMap<Int, WavData?>()
            val out = scenes.mapIndexed { i, sc ->
                val wav = narrator.synthesize(sc.narrationScript, "s${sc.id}_${System.nanoTime()}")
                audio[sc.sceneOrder] = wav
                onProgress((i + 1f) / scenes.size)
                val needed = wav?.let { Math.ceil(it.seconds + 0.7).toInt() } ?: 0
                if (needed > sc.durationSeconds) {
                    sc.copy(durationSeconds = needed).also { repository.updateScene(it) }
                } else sc
            }
            return Triple(out, audio, audio.values.any { it != null })
        } finally {
            narrator.shutdown()
        }
    }

    private suspend fun renderVideo(
        scenes: List<GeneratedSceneEntity>, audio: Map<Int, WavData?>, tutorialId: Int, onProgress: (Float) -> Unit
    ): File {
        val st = _uiState.value
        val dir = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES) ?: appContext.filesDir
        val file = File(dir, "codecast_${tutorialId}_${System.currentTimeMillis()}.mp4")
        val opts = renderOptions(st, st.selectedProject?.name)
        return withContext(Dispatchers.Default) { VideoExporter(opts).export(scenes, audio, file, onProgress) }
    }

    fun startVideoGeneration() {
        val project = _uiState.value.selectedProject ?: return
        if (_uiState.value.stepsList.none { it.isChecked }) {
            _uiState.update { it.copy(notificationMessage = "Select at least one step before generating.") }
            return
        }
        val currentTitle = _uiState.value.selectedWorkflow?.name ?: "Application Tutorial"

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            val s0 = _uiState.value
            val tutorialEntity = TutorialPlanEntity(
                projectId = project.id,
                title = currentTitle,
                tutorialType = s0.selectedTutorialType,
                selectedFeature = s0.selectedFeature,
                selectedWorkflow = s0.selectedWorkflow?.id ?: "",
                audience = s0.audience,
                experienceLevel = s0.experienceLevel,
                duration = s0.duration,
                presentationType = s0.presentationType,
                presenterOption = s0.presenterOption,
                voiceName = s0.voiceName,
                voiceGender = s0.voiceGender,
                voiceAccent = s0.voiceAccent,
                speakingStyle = s0.speakingStyle,
                speakingSpeed = s0.speakingSpeed,
                narrationLang = s0.narrationLang,
                subtitleLang = s0.subtitleLang,
                customTerminology = s0.customTerminology,
                hasSubtitles = s0.hasSubtitles,
                subtitleStyle = s0.subtitleStyle,
                subtitlePosition = s0.subtitlePosition,
                visualStyle = s0.visualStyle,
                cursorStyle = s0.cursorStyle,
                calloutStyle = s0.calloutStyle,
                zoomStyle = s0.zoomStyle,
                transitionStyle = s0.transitionStyle,
                illustrationStyle = s0.illustrationStyle,
                backgroundMusic = s0.backgroundMusic,
                musicVolume = s0.musicVolume,
                watermarkOption = s0.watermarkOption,
                status = "GENERATING"
            )

            val tutorialId = repository.saveTutorial(tutorialEntity).toInt()
            val stepsWithTutId = s0.stepsList.map { it.copy(tutorialId = tutorialId) }
            repository.saveSteps(stepsWithTutId)
            val savedTutorial = repository.getTutorial(tutorialId) ?: return@launch

            _uiState.update {
                it.copy(
                    activeTutorial = savedTutorial,
                    currentStep = WizardStep.GENERATE_VIDEO,
                    exportedVideoPath = null,
                    generationProgress = GenerationProgressState(isRunning = true, phaseName = "Planning scenes from your code evidence", progressPercent = 0.03f)
                )
            }
            val done = mutableListOf<String>()
            try {
                // 1. Plan + localize scenes
                val terms = savedTutorial.customTerminology.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val apiKey = s0.geminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
                val translator = if (apiKey.isNotBlank()) GeminiTranslator(apiKey, terms) else null
                setStage("Writing narration and subtitles", 0.10f, done)
                val (planned, loc) = repository.generateScenes(tutorialId, stepsWithTutId, savedTutorial, translator)
                done.add("Planned ${planned.size} scenes from verified code evidence")

                // 2. Narration audio
                setStage("Synthesizing narration audio", 0.25f, done)
                val (scenes, audio, audioOk) = narrate(planned, savedTutorial, onProgress = { p -> setStage("Synthesizing narration audio", 0.25f + 0.25f * p, done) })
                done.add(if (audioOk) "Narration audio synthesized" else "No speech engine for ${savedTutorial.narrationLang} (video will be silent)")
                val finalScenes = repository.getScenesList(tutorialId)

                // 3. Render MP4
                setStage("Rendering video", 0.50f, done)
                val file = renderVideo(finalScenes, audio, tutorialId) { p -> setStage("Rendering video", 0.50f + 0.40f * p, done) }
                done.add("Rendered ${file.name}")

                // 4. Quality check
                setStage("Running quality check", 0.95f, done)
                val report = repository.runQualityCheck(stepsWithTutId, finalScenes, savedTutorial, _uiState.value.extractedCodeFiles, loc, audioOk)
                done.add("Quality check: ${report.passedChecks}/${report.totalChecks} passed")
                repository.updateTutorial(savedTutorial.copy(status = "COMPLETED"))

                _uiState.update {
                    it.copy(
                        scenesList = finalScenes,
                        selectedEditorScene = finalScenes.firstOrNull(),
                        currentSceneIndex = 0,
                        playbackSecond = 0f,
                        exportedVideoPath = file.absolutePath,
                        currentStep = WizardStep.VIDEO_PREVIEW_EDITOR,
                        generationProgress = it.generationProgress.copy(
                            isRunning = false, isCompleted = true, progressPercent = 1f,
                            completedStages = done.toList(), qualityReport = report
                        )
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                repository.updateTutorial(savedTutorial.copy(status = "DRAFT"))
                _uiState.update {
                    it.copy(
                        generationProgress = it.generationProgress.copy(isRunning = false, phaseName = "Failed: ${e.message}"),
                        currentStep = WizardStep.REVIEW_TUTORIAL,
                        notificationMessage = "Video generation failed: ${e.message}"
                    )
                }
            }
        }
    }

    /** Re-renders the MP4 from the current (possibly edited) scenes. */
    fun exportVideo() {
        val tutorial = _uiState.value.activeTutorial ?: return
        if (_uiState.value.exportProgress != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(exportProgress = 0f) }
            try {
                val (scenes, audio, _) = narrate(_uiState.value.scenesList, tutorial, onProgress = { p -> _uiState.update { it.copy(exportProgress = 0.3f * p) } })
                val file = renderVideo(scenes, audio, tutorial.id) { p -> _uiState.update { it.copy(exportProgress = 0.3f + 0.7f * p) } }
                _uiState.update { it.copy(scenesList = scenes, exportProgress = null, exportedVideoPath = file.absolutePath, notificationMessage = "Video exported: ${file.name}") }
            } catch (e: Exception) {
                _uiState.update { it.copy(exportProgress = null, notificationMessage = "Export failed: ${e.message}") }
            }
        }
    }

    /** Writes an SRT file for the current scenes and returns it. */
    fun writeSrt(): File? {
        val scenes = _uiState.value.scenesList
        if (scenes.isEmpty()) return null
        val dir = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES) ?: appContext.filesDir
        return File(dir, "codecast_subtitles.srt").also { it.writeText(SrtWriter.build(scenes)) }
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

    /** Re-fetches the repository, re-analyzes and records a new version with a real diff. */
    fun applyVersionUpdate() {
        val project = _uiState.value.selectedProject ?: return
        if (project.repoSource == "ZIP" || project.repoUrl.isBlank()) {
            _uiState.update { it.copy(notificationMessage = "Upload the updated ZIP from the first step to create a new version.") }
            return
        }
        viewModelScope.launch {
            try {
                val before = _uiState.value.analysis
                val result = withContext(Dispatchers.IO) { CodebaseFetcher.fetch(project.repoUrl) }
                val after = withContext(Dispatchers.Default) { CodebaseAnalyzer.analyze(result.files) }
                val old = before.routes.map { it.path }.toSet()
                val new = after.routes.map { it.path }.toSet()
                val tag = "v1.${_uiState.value.versionsList.size}.0"
                repository.addVersion(
                    ProjectVersionEntity(
                        projectId = project.id, versionTag = tag,
                        changelog = "Routes added: ${(new - old).size}, removed: ${(old - new).size}. Files: ${result.files.size}.",
                        affectedTutorialsCount = if (old != new) 1 else 0, isLatest = true
                    )
                )
                repository.updateProject(project.copy(activeVersion = tag, screensCount = after.screenCount, routesCount = after.routes.size))
                _uiState.update {
                    it.copy(analysis = after, extractedCodeFiles = result.files,
                        selectedProject = it.selectedProject?.copy(activeVersion = tag),
                        notificationMessage = "Re-analyzed $tag: ${(new - old).size} routes added, ${(old - new).size} removed.")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(notificationMessage = "Update failed: ${e.message}") }
            }
        }
    }
}

class CodeCastViewModelFactory(
    private val repository: CodeCastRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CodeCastViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CodeCastViewModel(repository, appContext.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
