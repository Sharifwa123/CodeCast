package com.example.data.model

data class DetectedFeature(
    val name: String,
    val description: String,
    val iconName: String,
    val workflows: List<DetectedWorkflow>
)

data class DetectedWorkflow(
    val id: String,
    val name: String,
    val description: String,
    val defaultSteps: List<WorkflowStepData>
)

data class CodebaseFile(
    val path: String,
    val language: String,
    val lineCount: Int,
    val sizeBytes: Long,
    val content: String,
    val exportedSymbols: List<String> = emptyList(),
    val category: String = "Source" // "Frontend", "Backend", "Config", "Database"
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

data class WorkflowStepData(
    val title: String,
    val screenName: String,
    val actionType: String,
    val defaultInstruction: String,
    val verificationStatus: String, // "RUNTIME_VERIFIED", "CODE_VERIFIED", "INFERRED", "UNABLE_TO_VERIFY"
    val evidenceSource: String,
    val evidenceElement: String,
    val screenDrawable: String = "demo_screen_auth",
    val codeFilePath: String = "",
    val codeSnippet: String = "",
    val highlightedLines: String = "1,2,3"
)

data class QualityCheckIssue(
    val stepOrder: Int,
    val stepTitle: String,
    val issueType: String,
    val description: String,
    val suggestedFix: String
)

data class QualityCheckReport(
    val isClean: Boolean,
    val passedChecks: Int,
    val totalChecks: Int,
    val issues: List<QualityCheckIssue>
)

data class FrameworkInfo(
    val name: String,
    val category: String,
    val badgeColorHex: Long
)

data class RouteInfo(
    val path: String,
    val file: String,
    val line: Int,
    val kind: String, // "screen" | "api"
    val method: String = ""
)

data class ProjectAnalysis(
    val framework: String,
    val techStack: List<String>,
    val routes: List<RouteInfo>,
    val screenCount: Int,
    val formCount: Int,
    val features: List<DetectedFeature>
) {
    val workflows: List<DetectedWorkflow> get() = features.flatMap { it.workflows }
    fun findWorkflow(prefix: String): DetectedWorkflow? = workflows.firstOrNull { it.id.startsWith(prefix) }
    companion object {
        val EMPTY = ProjectAnalysis("Unknown", emptyList(), emptyList(), 0, 0, emptyList())
    }
}
