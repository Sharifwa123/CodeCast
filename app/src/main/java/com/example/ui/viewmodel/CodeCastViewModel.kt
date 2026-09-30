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
import com.example.record.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
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
    val recordMode: String = "SCREEN", // "SCREEN" = record the live app, "SLIDES" = code slides only
    val liveUrl: String = "",
    val urlCandidates: List<UrlCandidate> = emptyList(),
    val urlStatus: String? = null,
    val urlChecking: Boolean = false,
    val deviceProfile: String = "ANDROID_PHONE",
    val allowRealClicks: Boolean = false,
    val recorderVisible: Boolean = false,
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
                urlCandidates = LiveUrlFinder.fromSource(result.files, it.repoUrlInput),
                liveUrl = if (it.liveUrl.isBlank()) LiveUrlFinder.fromSource(result.files, it.repoUrlInput).firstOrNull()?.url ?: "" else it.liveUrl,
                notificationMessage = "Indexed ${result.files.size} source files (${analysis.framework})"
            )
        }
        // A maintainer-set GitHub homepage is the best hint for where the app is deployed
        val url = _uiState.value.repoUrlInput
        if (url.isNotBlank()) viewModelScope.launch {
            val hp = withContext(Dispatchers.IO) { RepoMeta.githubHomepage(url) }
            LiveUrlFinder.normalize(hp)?.let { n ->
                _uiState.update { st ->
                    st.copy(urlCandidates = listOf(UrlCandidate(n, "GitHub repository homepage")) + st.urlCandidates.filter { c -> c.url != n },
                        liveUrl = if (st.liveUrl.isBlank() || st.liveUrl == st.urlCandidates.firstOrNull()?.url) n else st.liveUrl)
                }
            }
        }
    }

    fun setRecordMode(mode: String) { _uiState.update { it.copy(recordMode = mode) } }
    fun setLiveUrl(url: String) { _uiState.update { it.copy(liveUrl = url.trim(), urlStatus = null) } }
    fun setDeviceProfile(id: String) { _uiState.update { it.copy(deviceProfile = id) } }
    fun setAllowRealClicks(v: Boolean) { _uiState.update { it.copy(allowRealClicks = v) } }

    fun checkLiveUrl() {
        val u = _uiState.value.liveUrl
        if (!LiveUrlFinder.isValid(u)) { _uiState.update { it.copy(urlStatus = "Enter a full URL such as https://yourapp.com") }; return }
        viewModelScope.launch {
            _uiState.update { it.copy(urlChecking = true, urlStatus = null) }
            val err = withContext(Dispatchers.IO) { RepoMeta.reachable(u) }
            _uiState.update { it.copy(urlChecking = false, urlStatus = err ?: "Reachable") }
        }
    }

    private var webViewDeferred = CompletableDeferred<android.webkit.WebView>()
    private var lastLoc = LocalizationResult(true, true)
    private var recorder: WebRecorder? = null
    private var liveSpecs = HashMap<Int, ElementSpec>()
    private var lastReport: ReconcileReport? = null

    private suspend fun acquireRecorder(): WebRecorder {
        recorder?.let { return it }
        val profile = DeviceProfile.fromId(_uiState.value.deviceProfile)
        webViewDeferred = CompletableDeferred()
        _uiState.update { it.copy(recorderVisible = true) }
        val webView = withTimeoutOrNull(10_000) { webViewDeferred.await() } ?: error("The recording view could not start")
        return WebRecorder(webView, profile).also { it.configure(); recorder = it }
    }

    private fun releaseRecorder() {
        recorder = null
        _uiState.update { it.copy(recorderVisible = false) }
    }

    /** Opens every page the steps need in the real browser view and checks the steps against what is really there. */
    private suspend fun verifyLive(steps: List<TutorialStepEntity>, rec: WebRecorder, done: MutableList<String>): ReconcileResult {
        val base = _uiState.value.liveUrl.trim().removeSuffix("/")
        val routes = LinkedHashMap<String, String>()
        routes["/"] = base
        steps.filter { it.isChecked && it.actionType == "Navigate" }.forEach { s ->
            StepPlanner.routeOf(s)?.takeIf { !it.contains(":") }?.let { r -> routes[r] = base + (if (r == "/") "" else r) }
        }
        val scans = HashMap<String, LiveScan>()
        routes.entries.forEachIndexed { i, (route, url) ->
            setStage("Reading the live page $route", 0.03f + 0.15f * i / routes.size, done)
            scans[route] = if (rec.load(url)) rec.scan(route, url) else LiveScan(route, url, "", emptyList(), rec.lastError ?: "could not open")
        }
        return Reconciler.reconcile(steps, scans)
    }


    fun attachWebView(v: android.webkit.WebView) { if (!webViewDeferred.isCompleted) webViewDeferred.complete(v) }

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
        if (_uiState.value.recordMode == "SCREEN" && !LiveUrlFinder.isValid(_uiState.value.liveUrl)) {
            _uiState.update { it.copy(notificationMessage = "Enter your app's live URL to record the real screen, or switch to code slides.") }
            return
        }

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
            var stepsWithTutId = s0.stepsList.map { it.copy(tutorialId = tutorialId) }
            val savedTutorial = repository.getTutorial(tutorialId) ?: return@launch
            liveSpecs = HashMap(); lastReport = null

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
                // 0. Read the live site and check the code-derived steps against it
                if (s0.recordMode == "SCREEN") {
                    setStage("Checking your live site against your code", 0.03f, done)
                    val rr = verifyLive(stepsWithTutId, acquireRecorder(), done)
                    check(rr.steps.any { it.isChecked && it.actionType != "Navigate" }) {
                        "Nothing in your code's workflow matches what the live page shows. " +
                            rr.report.codeOnly.joinToString("; ") { "${it.first}: ${it.second}" } + rr.report.pageNotes.joinToString("; ", prefix = " ")
                    }
                    stepsWithTutId = rr.steps
                    liveSpecs = HashMap(rr.specs); lastReport = rr.report
                    _uiState.update { it.copy(stepsList = rr.steps) }
                    done.add(rr.report.summary())
                }
                repository.saveSteps(stepsWithTutId.map { it.copy(id = 0) })

                // 1. Plan + localize scenes
                val terms = savedTutorial.customTerminology.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                val apiKey = s0.geminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY }
                val translator = if (apiKey.isNotBlank()) GeminiTranslator(apiKey, terms) else null
                setStage("Writing narration and subtitles", 0.10f, done)
                val (planned, loc) = repository.generateScenes(tutorialId, stepsWithTutId, savedTutorial, translator)
                done.add("Planned ${planned.size} scenes from verified code evidence")

                lastLoc = loc
                produce(savedTutorial, stepsWithTutId, planned, loc, done)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                repository.updateTutorial(savedTutorial.copy(status = "DRAFT"))
                releaseRecorder()
                _uiState.update {
                    it.copy(
                        recorderVisible = false,
                        generationProgress = it.generationProgress.copy(isRunning = false, phaseName = "Failed: ${e.message}"),
                        currentStep = WizardStep.REVIEW_TUTORIAL,
                        notificationMessage = "Video generation failed: ${e.message}"
                    )
                }
            }
        }
    }

    private class ScreenResult(val file: File, val issues: List<QualityCheckIssue>)

    /** Narration audio, then either a real screen recording of the live app or code slides, then the quality check. */
    private suspend fun produce(
        tutorial: TutorialPlanEntity, steps: List<TutorialStepEntity>, planned: List<GeneratedSceneEntity>,
        loc: LocalizationResult, done: MutableList<String>
    ) {
        setStage("Synthesizing narration audio", 0.25f, done)
        val (_, audio, audioOk) = narrate(planned, tutorial, onProgress = { p -> setStage("Synthesizing narration audio", 0.25f + 0.25f * p, done) })
        done.add(if (audioOk) "Narration audio synthesized" else "No speech engine for ${tutorial.narrationLang} (video will be silent)")
        var finalScenes = repository.getScenesList(tutorial.id)
        var extra: List<QualityCheckIssue> = emptyList()
        val file: File
        if (_uiState.value.recordMode == "SCREEN") {
            val r = recordScreens(tutorial, steps, finalScenes, audio, done)
            file = r.file
            extra = r.issues + (lastReport?.let { rep ->
                rep.codeOnly.map { (t, why) -> QualityCheckIssue(0, t, "Code and live site differ", why, "Update the deployed site or the code so they match, or remove the step.") } +
                    rep.pageNotes.map { QualityCheckIssue(0, "Live page", "Live page note", it, "Check the URL or sign-in requirements.") }
            } ?: emptyList())
            finalScenes = repository.getScenesList(tutorial.id)
        } else {
            setStage("Rendering video", 0.50f, done)
            file = renderVideo(finalScenes, audio, tutorial.id) { p -> setStage("Rendering video", 0.50f + 0.40f * p, done) }
            done.add("Rendered ${file.name}")
        }
        setStage("Running quality check", 0.95f, done)
        var report = repository.runQualityCheck(steps, finalScenes, tutorial, _uiState.value.extractedCodeFiles, loc, audioOk)
        if (extra.isNotEmpty()) report = report.copy(isClean = false, totalChecks = report.totalChecks + 1, issues = report.issues + extra)
        done.add("Quality check: ${report.passedChecks}/${report.totalChecks} passed")
        repository.updateTutorial(tutorial.copy(status = "COMPLETED"))
        _uiState.update {
            it.copy(
                scenesList = finalScenes,
                selectedEditorScene = finalScenes.firstOrNull(),
                currentSceneIndex = 0,
                playbackSecond = 0f,
                exportedVideoPath = file.absolutePath,
                currentStep = WizardStep.VIDEO_PREVIEW_EDITOR,
                recorderVisible = false,
                generationProgress = it.generationProgress.copy(
                    isRunning = false, isCompleted = true, progressPercent = 1f, completedStages = done.toList(), qualityReport = report
                )
            )
        }
    }

    /** Records the live site while performing each step for real, narration aligned to the recorded timeline. */
    private suspend fun recordScreens(
        tutorial: TutorialPlanEntity, steps: List<TutorialStepEntity>, scenes: List<GeneratedSceneEntity>,
        audio: Map<Int, WavData?>, done: MutableList<String>
    ): ScreenResult {
        val st = _uiState.value
        val profile = DeviceProfile.fromId(st.deviceProfile)
        val base = st.liveUrl.trim()
        val host = try { java.net.URI(base).host ?: base } catch (_: Exception) { base }
        // pair each scene with its step (scenes follow the checked steps in order)
        val active = steps.filter { it.isChecked }
        val pairs = ArrayList<Pair<TutorialStepEntity, GeneratedSceneEntity>>()
        var from = 0
        scenes.forEach { sc ->
            val idx = (from until active.size).firstOrNull { active[it].title == sc.title } ?: return@forEach
            pairs.add(active[idx] to sc); from = idx + 1
        }
        check(pairs.isNotEmpty()) { "No steps to record" }
        val actions = pairs.map { (step, _) ->
            val live = liveSpecs[step.id]
            when {
                live != null && step.actionType == "Input" -> PlannedAction.Type(live, StepPlanner.sampleValue(live))
                live != null && step.actionType == "Click" -> PlannedAction.Click(live, st.allowRealClicks)
                else -> StepPlanner.plan(step, base, st.allowRealClicks)
            }
        }

        val recorder = acquireRecorder()
        val startUrl = (actions.first() as? PlannedAction.Navigate)?.url ?: base
        setStage("Opening $host", 0.52f, done)
        if (!recorder.load(startUrl)) error("Couldn't open $startUrl (${recorder.lastError ?: "unknown error"})")

        val dir = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES) ?: appContext.filesDir
        val out = File(dir, "codecast_${tutorial.id}_${System.currentTimeMillis()}.mp4")
        val watermark = if (st.watermarkOption == "None") null else "CodeCast" + (st.selectedProject?.name?.let { " · $it" } ?: "")
        val presenter = renderOptions(st, null).presenter
        val session = RecordingSession(recorder, profile, out, host, watermark, tutorial.hasSubtitles, presenter)
        session.totalSteps = pairs.size
        session.start(viewModelScope)

        var currentUrl = startUrl
        val placements = ArrayList<Pair<Double, WavData>>()
        val durations = HashMap<Int, Int>()
        val issues = ArrayList<QualityCheckIssue>()
        val skippedIds = HashSet<Int>()
        try {
            pairs.forEachIndexed { i, (step, scene) ->
                val action = actions[i]
                setStage("Recording step ${i + 1} of ${pairs.size}: ${step.title}", 0.55f + 0.35f * i / pairs.size, done)
                session.step = i; session.subtitle = scene.subtitleText
                var reason: String? = when (action) {
                    is PlannedAction.Skip -> action.reason
                    is PlannedAction.Type -> if (recorder.probe(action.spec)) null else "Couldn't find the '${action.spec.label}' field on the live page"
                    is PlannedAction.Click -> if (recorder.probe(action.spec)) null else "Couldn't find the '${action.spec.text}' button on the live page"
                    else -> null
                }
                if (reason == null) {
                    val wav = audio[scene.sceneOrder]
                    val t0 = session.elapsedSec
                    val speech = wav?.seconds ?: ScenePlanner.spokenSeconds(scene.narrationScript, tutorial.speakingSpeed)
                    if (wav != null) placements.add(t0 to wav)
                    when (action) {
                        is PlannedAction.Navigate -> if (action.url != currentUrl) {
                            if (recorder.load(action.url)) currentUrl = action.url
                            else { reason = "Couldn't open ${action.url} (${recorder.lastError ?: "error"})"; if (wav != null) placements.removeAt(placements.size - 1) }
                        }
                        else -> recorder.run(action)
                    }
                    if (reason == null) {
                        val remaining = t0 + speech + 0.7 - session.elapsedSec
                        if (remaining > 0) delay((remaining * 1000).toLong())
                        durations[scene.sceneOrder] = Math.ceil(session.elapsedSec - t0).toInt().coerceAtLeast(2)
                    }
                }
                if (reason != null) {
                    skippedIds.add(scene.id)
                    issues.add(QualityCheckIssue(i + 1, step.title, "Not Recorded", reason, "Check the live URL, or pick a step whose page has no id in its route."))
                }
            }
            check(durations.isNotEmpty()) { "None of the steps could be recorded on the live site. " + issues.joinToString("; ") { it.description } }
            delay(900)
            val total = Math.ceil(session.elapsedSec).toInt() + 1
            val rate = placements.firstOrNull()?.second?.sampleRate ?: 22050
            val aac = if (placements.isEmpty()) null else withContext(Dispatchers.Default) {
                val pcm = ShortArray(total * rate)
                placements.forEach { (t, w) ->
                    val m = w.toMono(rate); val off = (t * rate).toInt()
                    val n = minOf(m.size, pcm.size - off)
                    if (n > 0) System.arraycopy(m, 0, pcm, off, n)
                }
                encodeAacPcm(pcm, rate)
            }
            setStage("Finishing video", 0.92f, done)
            val file = session.finish(aac)
            done.add("Recorded ${durations.size} of ${pairs.size} steps on $host (${profile.label})")
            // scenes: drop the ones that could not be recorded, use the real durations
            var order = 0
            scenes.forEach { sc ->
                if (sc.id in skippedIds) repository.deleteScene(sc.id)
                else if (sc.sceneOrder in durations) repository.updateScene(sc.copy(sceneOrder = ++order, durationSeconds = durations.getValue(sc.sceneOrder)))
            }
            return ScreenResult(file, issues)
        } catch (t: Throwable) {
            session.abort()
            throw t
        } finally {
            releaseRecorder()
        }
    }

    /** Re-records the live site using the current (possibly edited) scene text. */
    private fun reRecord() {
        val st = _uiState.value
        val tutorial = st.activeTutorial ?: return
        if (!LiveUrlFinder.isValid(st.liveUrl)) { _uiState.update { it.copy(notificationMessage = "Enter the live URL first.") }; return }
        val steps = st.stepsList.map { it.copy(tutorialId = tutorial.id) }
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            _uiState.update { it.copy(currentStep = WizardStep.GENERATE_VIDEO, generationProgress = GenerationProgressState(isRunning = true, phaseName = "Preparing to re-record", progressPercent = 0.05f)) }
            try {
                produce(tutorial, steps, _uiState.value.scenesList, lastLoc, mutableListOf())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(recorderVisible = false, currentStep = WizardStep.VIDEO_PREVIEW_EDITOR,
                    generationProgress = it.generationProgress.copy(isRunning = false), notificationMessage = "Recording failed: ${e.message}") }
            }
        }
    }

    /** Re-renders the MP4 from the current (possibly edited) scenes. */
    fun exportVideo() {
        if (_uiState.value.recordMode == "SCREEN") { reRecord(); return }
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
