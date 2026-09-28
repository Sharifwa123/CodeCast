package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TutorialPlanEntity
import com.example.data.repository.CodeCastRepository
import com.example.ui.components.*
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.CodeCastViewModel
import com.example.ui.viewmodel.WizardStep

@Composable
fun CodeCastApp(
    viewModel: CodeCastViewModel,
    repository: CodeCastRepository
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showVersionDialog by remember { mutableStateOf(false) }
    var showKnowledgeScreen by remember { mutableStateOf(false) }

    LaunchedEffect(state.notificationMessage) {
        state.notificationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissNotification()
        }
    }

    // Handle Android system back press
    BackHandler(enabled = state.currentStep != WizardStep.CREATE_PROJECT || showKnowledgeScreen) {
        if (showKnowledgeScreen) {
            showKnowledgeScreen = false
        } else {
            viewModel.prevStep()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Slate950,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                CodeCastTopBar(
                    currentStep = state.currentStep,
                    projectName = state.selectedProject?.name,
                    activeVersion = state.selectedProject?.activeVersion,
                    onDemoClick = { viewModel.loadBuiltInDemo() },
                    onVersionClick = { showVersionDialog = true }
                )

                if (!showKnowledgeScreen && state.currentStep != WizardStep.VIDEO_PREVIEW_EDITOR) {
                    WorkflowStepperHeader(
                        currentStep = state.currentStep,
                        onStepClick = { step -> viewModel.goToStep(step) }
                    )
                }
            }
        },
        bottomBar = {
            if (state.currentStep == WizardStep.VIDEO_PREVIEW_EDITOR && !showKnowledgeScreen) {
                Surface(
                    color = Slate900,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { showKnowledgeScreen = true },
                            colors = ButtonDefaults.textButtonColors(contentColor = Cyan400)
                        ) {
                            Icon(imageVector = Icons.Default.Hub, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Knowledge Hub", fontSize = 12.sp)
                        }

                        TextButton(
                            onClick = { viewModel.toggleQualityReportDialog(true) },
                            colors = ButtonDefaults.textButtonColors(contentColor = Emerald400),
                            modifier = Modifier.testTag("quality_check_badge_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Quality Check (Passed)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { viewModel.goToStep(WizardStep.CHOOSE_TUTORIAL) },
                            colors = ButtonDefaults.buttonColors(containerColor = Indigo600),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("New Tutorial", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (showKnowledgeScreen) {
                ProjectKnowledgeScreen(
                    project = state.selectedProject,
                    onBackClick = { showKnowledgeScreen = false }
                )
            } else {
                when (state.currentStep) {
                    WizardStep.CREATE_PROJECT, WizardStep.UPLOAD_CONNECT -> {
                        ProjectSetupAndImportScreen(
                            repoSourceOption = state.repoSourceOption,
                            projectNameInput = state.projectNameInput,
                            repoUrlInput = state.repoUrlInput,
                            onRepoSourceChange = { viewModel.setRepoSourceOption(it) },
                            onProjectNameChange = { viewModel.setProjectNameInput(it) },
                            onRepoUrlChange = { viewModel.setRepoUrlInput(it) },
                            onContinueClick = { viewModel.createAndAnalyzeProject(state.repoSourceOption) },
                            onQuickDemoClick = { viewModel.loadBuiltInDemo() }
                        )
                    }

                    WizardStep.ANALYZE_APP -> {
                        AnalysisProgressScreen(
                            project = state.selectedProject,
                            isAnalyzing = state.isAnalyzing,
                            checklist = state.analysisChecklist,
                            onContinueClick = { viewModel.goToStep(WizardStep.CHOOSE_TUTORIAL) }
                        )
                    }

                    WizardStep.CHOOSE_TUTORIAL -> {
                        TutorialTypeScreen(
                            selectedType = state.selectedTutorialType,
                            customPromptInput = state.customPromptInput,
                            onSelectType = { viewModel.selectTutorialType(it) },
                            onCustomPromptChange = { viewModel.setCustomPromptInput(it) },
                            onResolveCustomPrompt = { viewModel.resolveCustomPrompt() },
                            onContinueClick = {
                                if (state.selectedTutorialType == "CUSTOM") {
                                    viewModel.goToStep(WizardStep.SELECT_STEPS)
                                } else {
                                    viewModel.goToStep(WizardStep.CHOOSE_FEATURE_WORKFLOW)
                                }
                            }
                        )
                    }

                    WizardStep.CHOOSE_FEATURE_WORKFLOW -> {
                        FeatureWorkflowScreen(
                            features = repository.getDiscoveredFeatures(),
                            selectedFeatureName = state.selectedFeature,
                            selectedWorkflow = state.selectedWorkflow,
                            onSelectFeature = { viewModel.selectFeature(it) },
                            onSelectWorkflow = { viewModel.selectWorkflow(it) },
                            onContinueClick = { viewModel.goToStep(WizardStep.SELECT_STEPS) }
                        )
                    }

                    WizardStep.SELECT_STEPS -> {
                        StepSelectionScreen(
                            workflowName = state.selectedWorkflow?.name,
                            steps = state.stepsList,
                            inspectingEvidenceStep = state.inspectingEvidenceStep,
                            onToggleCheck = { viewModel.toggleStepChecked(it) },
                            onMoveUp = { viewModel.moveStepUp(it) },
                            onMoveDown = { viewModel.moveStepDown(it) },
                            onRemoveStep = { viewModel.removeStep(it) },
                            onUpdateStep = { id, title, inst -> viewModel.updateStepTitleAndInstruction(id, title, inst) },
                            onAddManualStep = { screen, act, title, inst -> viewModel.addManualStep(screen, act, title, inst) },
                            onInspectEvidence = { viewModel.setEvidenceInspectionStep(it) },
                            onContinueClick = { viewModel.goToStep(WizardStep.CHOOSE_AUDIENCE) }
                        )
                    }

                    WizardStep.CHOOSE_AUDIENCE -> {
                        AudienceSelectionScreen(
                            selectedAudience = state.audience,
                            selectedExperienceLevel = state.experienceLevel,
                            onSelectAudience = { viewModel.setAudience(it) },
                            onSelectExperienceLevel = { viewModel.setExperienceLevel(it) },
                            onContinueClick = { viewModel.goToStep(WizardStep.CHOOSE_DURATION_PRESENTATION) }
                        )
                    }

                    WizardStep.CHOOSE_DURATION_PRESENTATION -> {
                        DurationPresentationScreen(
                            selectedDuration = state.duration,
                            selectedPresentationType = state.presentationType,
                            selectedPresenterOption = state.presenterOption,
                            presenterLikenessConsent = state.presenterLikenessConsent,
                            onSelectDuration = { viewModel.setDuration(it) },
                            onSelectPresentationType = { viewModel.setPresentationType(it) },
                            onSelectPresenterOption = { viewModel.setPresenterOption(it) },
                            onToggleConsent = { viewModel.setPresenterLikenessConsent(it) },
                            onContinueClick = { viewModel.goToStep(WizardStep.CHOOSE_VOICE) }
                        )
                    }

                    WizardStep.CHOOSE_VOICE, WizardStep.CHOOSE_LANGUAGE_SUBTITLES -> {
                        VoiceLanguageScreen(
                            voiceName = state.voiceName,
                            voiceGender = state.voiceGender,
                            voiceAccent = state.voiceAccent,
                            speakingStyle = state.speakingStyle,
                            speakingSpeed = state.speakingSpeed,
                            narrationLang = state.narrationLang,
                            subtitleLang = state.subtitleLang,
                            customTerminology = state.customTerminology,
                            hasSubtitles = state.hasSubtitles,
                            subtitleStyle = state.subtitleStyle,
                            subtitlePosition = state.subtitlePosition,
                            onVoiceChange = { name, gender, accent, style, speed ->
                                viewModel.setVoiceSettings(name, gender, accent, style, speed)
                            },
                            onNarrationLangChange = { viewModel.setNarrationLanguage(it) },
                            onSubtitleLangChange = { viewModel.setSubtitleLanguage(it) },
                            onCustomTermsChange = { viewModel.setCustomTerminology(it) },
                            onSubtitleConfigChange = { en, sty, pos ->
                                viewModel.setSubtitleConfig(en, sty, pos)
                            },
                            onContinueClick = { viewModel.goToStep(WizardStep.CUSTOMIZE_APPEARANCE) }
                        )
                    }

                    WizardStep.CUSTOMIZE_APPEARANCE -> {
                        AppearanceBrandingScreen(
                            cursorStyle = state.cursorStyle,
                            calloutStyle = state.calloutStyle,
                            zoomStyle = state.zoomStyle,
                            transitionStyle = state.transitionStyle,
                            backgroundMusic = state.backgroundMusic,
                            musicVolume = state.musicVolume,
                            watermarkOption = state.watermarkOption,
                            onVisualChange = { cur, cal, zm, tr ->
                                viewModel.setVisualAndIllustration(
                                    state.visualStyle,
                                    cur,
                                    cal,
                                    zm,
                                    tr,
                                    state.illustrationStyle,
                                    state.illustrationAnimation
                                )
                            },
                            onMusicChange = { m, v ->
                                viewModel.setBrandingAndAudio(m, v, state.watermarkOption)
                            },
                            onWatermarkChange = { wm ->
                                viewModel.setBrandingAndAudio(state.backgroundMusic, state.musicVolume, wm)
                            },
                            onContinueClick = { viewModel.goToStep(WizardStep.REVIEW_TUTORIAL) }
                        )
                    }

                    WizardStep.REVIEW_TUTORIAL -> {
                        ReviewTutorialScreen(
                            workflowTitle = state.selectedWorkflow?.name ?: "Application Walkthrough",
                            audience = state.audience,
                            duration = state.duration,
                            presentationType = state.presentationType,
                            voiceName = state.voiceName,
                            narrationLang = state.narrationLang,
                            subtitleLang = state.subtitleLang,
                            steps = state.stepsList,
                            onGenerateClick = { viewModel.startVideoGeneration() },
                            onBackToStepsClick = { viewModel.goToStep(WizardStep.SELECT_STEPS) }
                        )
                    }

                    WizardStep.GENERATE_VIDEO -> {
                        GenerationPipelineScreen(
                            progressState = state.generationProgress,
                            onViewResultClick = { viewModel.goToStep(WizardStep.VIDEO_PREVIEW_EDITOR) }
                        )
                    }

                    WizardStep.VIDEO_PREVIEW_EDITOR -> {
                        val scroll = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scroll)
                        ) {
                            state.activeTutorial?.let { tut ->
                                VideoPlayerAndEditorView(
                                    tutorial = tut,
                                    scenes = state.scenesList,
                                    currentSceneIndex = state.currentSceneIndex,
                                    playbackSecond = state.playbackSecond,
                                    isPlaying = state.isPlaying,
                                    selectedScene = state.selectedEditorScene,
                                    onPlayPauseToggle = { viewModel.togglePlayPause() },
                                    onSeekScene = { viewModel.seekToScene(it) },
                                    onSelectEditorScene = { viewModel.selectEditorScene(it) },
                                    onUpdateScene = { nar, sub, dur, cal ->
                                        viewModel.updateSelectedScene(nar, sub, dur, cal)
                                    },
                                    onRegenerateScene = { viewModel.regenerateSingleScene(it) },
                                    onExportRequested = { format ->
                                        when (format) {
                                            "MP4" -> viewModel.updateSelectedScene(
                                                state.selectedEditorScene?.narrationScript ?: "",
                                                state.selectedEditorScene?.subtitleText ?: "",
                                                state.selectedEditorScene?.durationSeconds ?: 8,
                                                state.selectedEditorScene?.calloutText ?: ""
                                            )
                                            else -> {}
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Quality Report Modal
    if (state.showQualityReportDialog) {
        val report = state.generationProgress.qualityReport ?: repository.runQualityCheck(
            state.stepsList,
            state.activeTutorial ?: TutorialPlanEntity(projectId = 0, title = "Tutorial", tutorialType = "SIGN_UP")
        )
        QualityCheckReportDialog(
            report = report,
            onDismiss = { viewModel.toggleQualityReportDialog(false) },
            onFixAutomatically = {
                viewModel.toggleQualityReportDialog(false)
            },
            onContinueAnyway = {
                viewModel.toggleQualityReportDialog(false)
            }
        )
    }

    // Versioning Dialog
    if (showVersionDialog) {
        ProjectVersionDialog(
            versions = state.versionsList,
            onDismiss = { showVersionDialog = false },
            onApplyUpdate = { viewModel.applyVersionUpdate() }
        )
    }
}
