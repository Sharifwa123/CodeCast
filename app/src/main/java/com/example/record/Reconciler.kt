package com.example.record

import com.example.data.model.TutorialStepEntity

/** One real element found on the live page by the director's scan. */
data class LiveElement(
    val kind: String,            // "input" | "button"
    val tag: String,
    val type: String,
    val name: String,
    val id: String,
    val placeholder: String,
    val label: String,
    val text: String,
    val index: Int,              // position among visible elements of this kind (used to find it again)
    val inForm: Boolean,
    val required: Boolean = false
)

data class LiveScan(val route: String, val url: String, val title: String, val elements: List<LiveElement>, val error: String? = null)

class ReconcileReport(
    val matched: List<String>,
    val codeOnly: List<Pair<String, String>>,   // step title to reason
    val liveOnly: List<String>,
    val pageNotes: List<String>
) {
    fun summary() = "Live check: ${matched.size} steps confirmed on the real page, ${codeOnly.size} only in code, ${liveOnly.size} added from the live page"
}

class ReconcileResult(val steps: List<TutorialStepEntity>, val specs: Map<Int, ElementSpec>, val report: ReconcileReport)

/**
 * Checks the steps derived from the source code against what the live page really shows.
 * Matched steps are rewritten with the labels the user actually sees and become RUNTIME_VERIFIED;
 * steps the live page does not have are excluded and reported; fields only the live page has are added.
 */
object Reconciler {
    private val STOP = setOf("enter", "your", "the", "a", "an", "field", "input", "button", "click", "press", "fill", "to", "of", "and")
    private val SKIP_TYPES = setOf("hidden", "submit", "button", "checkbox", "radio", "file", "image", "reset")

    fun tokens(s: String): Set<String> =
        s.lowercase().replace(Regex("""[^a-z0-9@]+"""), " ").trim().split(' ').filter { it.isNotEmpty() && it !in STOP }.toSet()

    fun similarity(a: String, b: String): Double {
        val x = tokens(a); val y = tokens(b)
        if (x.isEmpty() || y.isEmpty()) return 0.0
        val inter = x.count { t -> t in y || y.any { u -> u.length > 3 && (u.startsWith(t) || t.startsWith(u)) } }
        return inter.toDouble() / maxOf(x.size, y.size)
    }

    private fun score(src: ElementSpec, el: LiveElement): Int {
        var sc = 0
        if (src.name.isNotEmpty() && src.name == el.name) sc += 6
        if (src.id.isNotEmpty() && src.id == el.id) sc += 6
        if (src.kind == "input") {
            if (src.placeholder.isNotEmpty() && tokens(src.placeholder) == tokens(el.placeholder) && el.placeholder.isNotEmpty()) sc += 5
            val best = maxOf(similarity(src.label, el.label), similarity(src.label, el.placeholder), similarity(src.label, el.name))
            if (best >= 0.5) sc += 4
            if (src.type.isNotEmpty() && src.type == el.type) sc += 2
        } else {
            val s = similarity(src.text, el.text)
            if (s >= 0.99) sc += 6 else if (s >= 0.5) sc += 3
            if (src.submit && el.type == "submit") sc += 2
        }
        return sc
    }

    fun liveSpec(el: LiveElement) = ElementSpec(
        kind = el.kind, name = el.name, id = el.id, placeholder = el.placeholder, type = el.type,
        label = el.label.ifBlank { el.placeholder }.ifBlank { el.name }, text = el.text, submit = el.type == "submit", index = el.index
    )

    private fun humanLabel(el: LiveElement) = el.label.ifBlank { el.placeholder }.ifBlank { el.name.replace('_', ' ').replace('-', ' ') }.trim().take(40)

    fun reconcile(steps: List<TutorialStepEntity>, scans: Map<String, LiveScan>): ReconcileResult {
        val matched = ArrayList<String>(); val codeOnly = ArrayList<Pair<String, String>>(); val liveOnly = ArrayList<String>(); val notes = ArrayList<String>()
        val specs = HashMap<Int, ElementSpec>()
        // split into segments that start at each Navigate step
        val segments = ArrayList<MutableList<TutorialStepEntity>>()
        steps.forEach { s -> if (s.actionType == "Navigate" || segments.isEmpty()) segments.add(mutableListOf(s)) else segments.last().add(s) }
        var nextId = (steps.maxOfOrNull { it.id } ?: 0) + 1
        val out = ArrayList<TutorialStepEntity>()

        segments.forEach { seg ->
            val nav = seg.firstOrNull { it.actionType == "Navigate" }
            val route = nav?.let { StepPlanner.routeOf(it) } ?: "/"
            val scan = if (route.contains(":")) null else (scans[route] ?: if (nav == null) scans["/"] else null)
            val segOut = ArrayList<TutorialStepEntity>()
            val used = HashSet<String>()
            seg.forEach { st ->
                if (!st.isChecked) { segOut.add(st); return@forEach }
                when (st.actionType) {
                    "Navigate" -> when {
                        scan == null -> segOut.add(st)
                        scan.error != null -> { segOut.add(st.copy(isChecked = false)); codeOnly.add(st.title to "The live page could not be opened (${scan.error})"); notes.add("$route: ${scan.error}") }
                        else -> { segOut.add(st.copy(verificationStatus = "RUNTIME_VERIFIED")); matched.add(st.title); if (scan.elements.isEmpty()) notes.add("$route opened but shows no fields or buttons (it may need a login or load content later)") }
                    }
                    "Input", "Click" -> {
                        if (scan == null || scan.error != null) {
                            segOut.add(st.copy(isChecked = false)); codeOnly.add(st.title to "No live page to check against"); return@forEach
                        }
                        val kind = if (st.actionType == "Input") "input" else "button"
                        val src = StepPlanner.specOf(st, kind)
                        val pool = scan.elements.filter { it.kind == kind && "${it.kind}${it.index}" !in used && (kind == "button" || it.type !in SKIP_TYPES) }
                        var best = pool.map { it to score(src, it) }.filter { it.second >= (if (kind == "input") 4 else 3) }.maxByOrNull { it.second }?.first
                        if (best == null && kind == "input" && src.type in setOf("password", "email", "tel", "search")) best = pool.singleOrNull { it.type == src.type }
                        if (best == null && kind == "button" && src.submit) best = pool.firstOrNull { it.type == "submit" && it.inForm }
                        if (best == null) {
                            segOut.add(st.copy(isChecked = false))
                            val live = scan.elements.filter { it.kind == kind && (kind == "button" || it.type !in SKIP_TYPES) }.map { if (kind == "input") humanLabel(it) else it.text }.filter { it.isNotBlank() }.distinct().take(6)
                            codeOnly.add(st.title to "In your code but not on the live page at $route" + if (live.isNotEmpty()) " (live page has: ${live.joinToString(", ")})" else " (no matching elements visible)")
                        } else {
                            used.add("${best.kind}${best.index}")
                            val spec = liveSpec(best); specs[st.id] = spec
                            val lab = humanLabel(best)
                            val updated = if (kind == "input") st.copy(
                                title = if (best.type == "password") "Enter your password" else "Enter " + lab.replaceFirstChar { it.lowercase() },
                                instruction = "Fill the '$lab' field", verificationStatus = "RUNTIME_VERIFIED"
                            ) else st.copy(
                                title = "Click " + best.text.ifBlank { lab }, instruction = "Press the '${best.text.ifBlank { lab }}' button", verificationStatus = "RUNTIME_VERIFIED"
                            )
                            segOut.add(updated); matched.add(updated.title)
                        }
                    }
                    else -> segOut.add(st)
                }
            }
            // fields the live form has that the code analysis did not describe
            if (scan != null && scan.error == null && seg.any { it.actionType == "Input" || it.actionType == "Click" }) {
                val extra = scan.elements.filter { it.kind == "input" && it.inForm && it.type !in SKIP_TYPES && "input${it.index}" !in used && humanLabel(it).isNotBlank() }
                val firstClick = segOut.indexOfFirst { it.actionType == "Click" }.let { if (it < 0) segOut.size else it }
                extra.forEachIndexed { k, el ->
                    val lab = humanLabel(el)
                    val step = TutorialStepEntity(
                        id = nextId++, tutorialId = seg.first().tutorialId, stepOrder = 0,
                        title = if (el.type == "password") "Enter your password" else "Enter " + lab.replaceFirstChar { it.lowercase() },
                        screenName = seg.first().screenName, actionType = "Input", instruction = "Fill the '$lab' field",
                        verificationStatus = "RUNTIME_VERIFIED", evidenceSource = "Live page only",
                        evidenceElement = "<${el.tag} name=\"${el.name}\" type=\"${el.type}\">", isChecked = true
                    )
                    specs[step.id] = liveSpec(el)
                    segOut.add(firstClick + k, step); liveOnly.add(step.title)
                }
            }
            out.addAll(segOut)
        }
        val reindexed = out.mapIndexed { i, s -> s.copy(stepOrder = i + 1) }
        return ReconcileResult(reindexed, specs, ReconcileReport(matched, codeOnly, liveOnly, notes))
    }
}
