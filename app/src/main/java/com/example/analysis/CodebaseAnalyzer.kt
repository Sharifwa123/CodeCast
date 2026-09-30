package com.example.analysis

import com.example.data.model.*

/**
 * Static analysis of a real codebase. Every step it emits points at an actual file and line;
 * nothing is ever marked RUNTIME_VERIFIED because the app is never executed.
 */
object CodebaseAnalyzer {

    private data class Element(
        val file: String, val line: Int, val kind: String, // input | button | form
        val label: String, val type: String = ""
    )

    private class Screen(val file: String, val route: RouteInfo?) {
        val elements = mutableListOf<Element>()
        val name: String get() = screenName(route?.path, file)
    }

    fun analyze(files: List<CodebaseFile>): ProjectAnalysis {
        val byPath = files.associateBy { it.path }
        val (framework, tech) = detectFramework(files)
        val routes = files.flatMap { extractRoutes(it) }.distinctBy { Triple(it.path, it.kind, it.method) }
        val screenRoutes = routes.filter { it.kind == "screen" }
        val apiRoutes = routes.filter { it.kind == "api" }

        val screens = LinkedHashMap<String, Screen>()
        for (f in files) {
            if (!isUiFile(f)) continue
            val elems = extractElements(f)
            val route = screenRoutes.firstOrNull { it.file == f.path }
            if (elems.isEmpty() && route == null) continue
            screens.getOrPut(f.path) { Screen(f.path, route) }.elements.addAll(elems)
        }
        // Routes declared in a router file whose component lives elsewhere: no screen unless elements exist.
        val orphanRoutes = screenRoutes.filter { r -> screens.values.none { it.route == r } && r.file !in screens }

        val grouped = LinkedHashMap<String, MutableList<Screen>>()
        for (s in screens.values) grouped.getOrPut(featureOf(s.route?.path, s.file)) { mutableListOf() }.add(s)
        for (r in orphanRoutes) grouped.getOrPut(featureOf(r.path, r.file)) { mutableListOf() }
            .add(Screen(r.file, r))

        val features = grouped.map { (name, list) ->
            val wfs = list.mapNotNull { workflowFor(it, byPath) }.toMutableList()
            DetectedFeature(name, descriptionOf(name, list.size), iconOf(name), wfs)
        }.filter { it.workflows.isNotEmpty() }.toMutableList()

        if (apiRoutes.isNotEmpty()) {
            features.add(
                DetectedFeature(
                    "API Endpoints", "${apiRoutes.size} HTTP endpoints found in source.", "terminal",
                    listOf(apiWorkflow(apiRoutes, byPath))
                )
            )
        }
        val sorted = features.sortedWith(compareBy({ if (it.name == "Authentication") 0 else 1 }, { -it.workflows.sumOf { w -> w.defaultSteps.size } }))
        return ProjectAnalysis(
            framework = framework,
            techStack = tech,
            routes = routes,
            screenCount = (screens.size + orphanRoutes.size),
            formCount = screens.values.count { s -> s.elements.any { it.kind == "form" || it.kind == "input" } },
            features = sorted
        )
    }

    // ---------- framework ----------
    fun detectFramework(files: List<CodebaseFile>): Pair<String, List<String>> {
        val pkg = files.filter { it.path.endsWith("package.json") }.minByOrNull { it.path.count { c -> c == '/' } }
        val deps = pkg?.let { p ->
            Regex(""""([@\w./-]+)"\s*:\s*"[^"]*"""").findAll(p.content).map { it.groupValues[1] }.toSet()
        } ?: emptySet()
        val paths = files.map { it.path.lowercase() }
        fun text(name: String) = files.firstOrNull { it.path.lowercase().endsWith(name) }?.content?.lowercase() ?: ""
        val tech = mutableListOf<String>()
        fun dep(vararg n: String) = n.any { it in deps }
        val fw = when {
            dep("next") -> "Next.js"
            dep("nuxt") -> "Nuxt"
            dep("@angular/core") -> "Angular"
            dep("svelte", "@sveltejs/kit") -> "Svelte"
            dep("vue") -> "Vue"
            dep("react-native") -> "React Native"
            dep("react") -> if (dep("react-router-dom", "react-router")) "React + React Router" else "React"
            dep("@nestjs/core") -> "NestJS"
            dep("express") -> "Express"
            dep("fastify") -> "Fastify"
            paths.any { it.endsWith("pubspec.yaml") } -> "Flutter"
            paths.any { it.endsWith("artisan") || it.endsWith("composer.json") } && text("composer.json").contains("laravel") -> "Laravel"
            (text("requirements.txt") + text("pyproject.toml")).contains("fastapi") -> "FastAPI"
            (text("requirements.txt") + text("pyproject.toml")).contains("django") || paths.any { it.endsWith("manage.py") } -> "Django"
            (text("requirements.txt") + text("pyproject.toml")).contains("flask") -> "Flask"
            paths.any { it.endsWith("androidmanifest.xml") } -> "Android (Kotlin/Java)"
            paths.any { it.endsWith("go.mod") } -> "Go"
            paths.any { it.endsWith(".html") } -> "Static HTML"
            else -> "Unknown"
        }
        if (dep("typescript") || paths.any { it.endsWith(".ts") || it.endsWith(".tsx") }) tech.add("TypeScript")
        listOf(
            "tailwindcss" to "TailwindCSS", "prisma" to "Prisma", "@prisma/client" to "Prisma", "mongoose" to "MongoDB",
            "pg" to "PostgreSQL", "stripe" to "Stripe", "firebase" to "Firebase", "@supabase/supabase-js" to "Supabase",
            "axios" to "Axios", "zod" to "Zod", "redux" to "Redux", "@reduxjs/toolkit" to "Redux", "graphql" to "GraphQL"
        ).forEach { (d, n) -> if (d in deps && n !in tech) tech.add(n) }
        return fw to (listOf(fw) + tech).distinct()
    }

    // ---------- routes ----------
    private fun isSource(f: CodebaseFile) = f.path.substringAfterLast('.', "").lowercase() in
        setOf("ts", "tsx", "js", "jsx", "mjs", "vue", "svelte", "py", "php", "dart", "html", "rb", "go", "java", "kt")

    private fun isUiFile(f: CodebaseFile): Boolean {
        val ext = f.path.substringAfterLast('.', "").lowercase()
        val p = f.path.lowercase()
        if (ext !in setOf("tsx", "jsx", "vue", "svelte", "html", "dart", "js", "ts", "php", "kt")) return false
        if (ext in setOf("js", "ts") && !(p.contains("component") || p.contains("page") || p.contains("view") || p.contains("screen"))) return false
        if (ext == "kt" && !f.content.contains("@Composable")) return false
        if (p.contains(".test.") || p.contains(".spec.") || p.contains("/test/") || p.contains("__tests__")) return false
        if (ext == "php" && !p.contains("view") && !p.endsWith(".blade.php")) return false
        return true
    }

    fun extractRoutes(f: CodebaseFile): List<RouteInfo> {
        if (!isSource(f)) return emptyList()
        val out = mutableListOf<RouteInfo>()
        val p = f.path
        val lower = p.lowercase()
        Regex("""(?:^|/)app/((?:.*/)?)page\.(?:tsx|jsx|ts|js)$""").find(p)?.let {
            out.add(RouteInfo(nextPath(it.groupValues[1]), p, 1, "screen"))
        }
        Regex("""(?:^|/)app/((?:.*/)?)route\.(?:ts|js)$""").find(p)?.let {
            val methods = Regex("""export\s+(?:async\s+)?function\s+(GET|POST|PUT|DELETE|PATCH)""").findAll(f.content)
            val list = methods.toList()
            if (list.isEmpty()) out.add(RouteInfo(nextPath(it.groupValues[1]), p, 1, "api", "ANY"))
            list.forEach { m -> out.add(RouteInfo(nextPath(it.groupValues[1]), p, lineOf(f.content, m.range.first), "api", m.groupValues[1])) }
        }
        Regex("""(?:^|/)pages/(.+)\.(?:tsx|jsx|ts|js|vue)$""").find(p)?.let {
            val rel = it.groupValues[1]
            val leaf = rel.substringAfterLast('/')
            if (!leaf.startsWith("_") && !rel.startsWith("api/")) {
                out.add(RouteInfo(nextPath(rel.removeSuffix("index").removeSuffix("/")), p, 1, "screen"))
            } else if (rel.startsWith("api/")) out.add(RouteInfo("/" + rel, p, 1, "api", "ANY"))
        }
        val lines = f.content.lines()
        fun add(regex: Regex, kind: String, methodGroup: Int?, pathGroup: Int, prefix: String = "") {
            lines.forEachIndexed { i, l ->
                regex.findAll(l).forEach { m ->
                    val path = m.groupValues[pathGroup]
                    val full = if (path.startsWith("/") || prefix.isEmpty()) path else prefix + path
                    out.add(RouteInfo(if (full.startsWith("/")) full else "/$full", p, i + 1, kind, methodGroup?.let { g -> m.groupValues[g].uppercase() } ?: ""))
                }
            }
        }
        val ext = lower.substringAfterLast('.')
        if (ext in setOf("tsx", "jsx", "js", "ts", "vue", "svelte")) {
            add(Regex("""<Route[^>]*\spath=["']([^"']+)["']"""), "screen", null, 1)
            add(Regex("""\bpath:\s*["'](/[^"']*)["']"""), "screen", null, 1)
            add(Regex("""\b(?:app|router|server|fastify)\.(get|post|put|delete|patch)\(\s*["'`](/[^"'`]*)["'`]"""), "api", 1, 2)
        }
        if (ext == "py") {
            add(Regex("""@\w+\.(get|post|put|delete|patch)\(\s*["']([^"']*)["']"""), "api", 1, 2)
            add(Regex("""@\w+\.route\(\s*["']([^"']*)["']"""), "screen", null, 1)
            add(Regex("""\bpath\(\s*["']([^"']*)["']"""), "screen", null, 1, "/")
        }
        if (ext == "php") add(Regex("""Route::(get|post|put|delete|patch)\(\s*['"]([^'"]*)['"]"""), "screen", 1, 2, "/")
        if (ext == "dart") {
            add(Regex("""GoRoute\(\s*path:\s*['"]([^'"]*)['"]"""), "screen", null, 1)
            add(Regex("""['"](/[^'"]*)['"]\s*:\s*\("""), "screen", null, 1)
        }
        return out
    }

    private fun nextPath(dir: String): String {
        val segs = dir.split('/').filter { it.isNotEmpty() && !(it.startsWith("(") && it.endsWith(")")) }
            .map { s -> if (s.startsWith("[") && s.endsWith("]")) ":" + s.trim('[', ']', '.') else s }
        return "/" + segs.joinToString("/")
    }

    private fun lineOf(content: String, index: Int) = content.substring(0, index.coerceAtMost(content.length)).count { it == '\n' } + 1

    // ---------- UI elements ----------
    private fun extractElements(f: CodebaseFile): List<Element> {
        val lines = f.content.lines()
        val out = mutableListOf<Element>()
        val ext = f.path.substringAfterLast('.').lowercase()
        for (i in lines.indices) {
            val l = lines[i]
            val win = lines.subList(i, minOf(i + 4, lines.size)).joinToString(" ")
            val back = lines.subList(maxOf(0, i - 2), i + 1).joinToString(" ")
            val inputRe = Regex("""<\s*(?:input|textarea|select)\b|<\s*[A-Z]\w*(?:Input|Field|Select|Textarea)\b""", RegexOption.IGNORE_CASE)
            val buttonRe = Regex("""<\s*(?:button|Button)\b""")
            val tag = tagAt(win, (inputRe.find(win)?.takeIf { l.contains(it.value) }?.range?.first ?: buttonRe.find(win)?.range?.first ?: 0))
            when {
                Regex("""<\s*(input|textarea|select)\b""", RegexOption.IGNORE_CASE).containsMatchIn(l) ||
                    Regex("""<\s*[A-Z]\w*(Input|Field|Select|Textarea)\b""").containsMatchIn(l) -> {
                    val type = attr(tag, "type") ?: if (l.contains("textarea", true)) "textarea" else "text"
                    if (type.lowercase() in setOf("hidden", "submit", "button")) continue
                    val label = labelTextBefore(back) ?: attr(tag, "aria-label") ?: attr(tag, "label") ?: attr(tag, "placeholder")
                        ?: attr(tag, "name")?.let { humanize(it) } ?: attr(tag, "id")?.let { humanize(it) } ?: typeLabel(type)
                    out.add(Element(f.path, i + 1, "input", humanize(label), type.lowercase()))
                }
                (ext == "dart" && Regex("""\bText(Form)?Field\(""").containsMatchIn(l)) -> {
                    val label = Regex("""(?:labelText|hintText):\s*['"]([^'"]+)['"]""").find(win)?.groupValues?.get(1) ?: "Text field"
                    out.add(Element(f.path, i + 1, "input", label, if (label.contains("pass", true)) "password" else "text"))
                }
                (ext == "kt" && Regex("""\b(Outlined)?TextField\(""").containsMatchIn(l)) -> {
                    val label = Regex("""label\s*=\s*\{\s*Text\(\s*"([^"]+)"""").find(win)?.groupValues?.get(1) ?: "Text field"
                    out.add(Element(f.path, i + 1, "input", label, "text"))
                }
                Regex("""<\s*(button|Button)\b""").containsMatchIn(l) -> {
                    val label = buttonText(win) ?: attr(tag, "aria-label") ?: attr(tag, "value") ?: attr(tag, "id")?.let { humanize(it) } ?: "Button"
                    out.add(Element(f.path, i + 1, "button", humanize(label), attr(tag, "type") ?: ""))
                }
                (ext == "dart" && Regex("""\b(Elevated|Text|Outlined|Filled)Button\(""").containsMatchIn(l)) -> {
                    val label = Regex("""child:\s*(?:const\s+)?Text\(\s*['"]([^'"]+)['"]""").find(win)?.groupValues?.get(1) ?: "Button"
                    out.add(Element(f.path, i + 1, "button", label))
                }
                Regex("""<\s*form\b""", RegexOption.IGNORE_CASE).containsMatchIn(l) -> out.add(Element(f.path, i + 1, "form", "Form"))
            }
        }
        return out
    }

    /** The opening tag starting at [start], ending at the first '>' outside {...} (so `=>` in handlers is skipped). */
    private fun tagAt(text: String, start: Int): String {
        var depth = 0
        var i = start
        while (i < text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> if (depth > 0) depth--
                '>' -> if (depth == 0) return text.substring(start, i)
            }
            i++
        }
        return text.substring(start)
    }

    /** Visible text of the <label> that wraps/precedes an input, e.g. <label><span>Business email</span><input/> */
    private fun labelTextBefore(back: String): String? {
        val m = Regex("""<label\b[^>]*>(.*?)<\s*(?:input|select|textarea|[A-Z]\w*(?:Input|Field|Select|Textarea))""", RegexOption.IGNORE_CASE)
            .findAll(back).lastOrNull() ?: return null
        val t = m.groupValues[1].replace(Regex("""<[^>]*>"""), " ").replace(Regex("""\{[^}]*\}"""), " ").replace(Regex("""\s+"""), " ").trim()
        return t.takeIf { it.isNotBlank() }
    }

    /** Button caption: plain text, or the last string literal of a {cond ? "Loading" : "Create account"} expression. */
    private fun buttonText(win: String): String? {
        val inner = Regex("""<\s*(?:button|Button)\b[^>]*>\s*(.+?)\s*</""", RegexOption.IGNORE_CASE).find(win)?.groupValues?.get(1) ?: return null
        val quoted = Regex("""["'`]([^"'`]{2,})["'`]""").findAll(inner).lastOrNull()?.groupValues?.get(1)
        val text = quoted ?: inner.replace(Regex("""<[^>]*>"""), " ").replace(Regex("""\{[^}]*\}"""), " ").replace(Regex("""\s+"""), " ").trim()
        return text.takeIf { it.isNotBlank() }
    }

    private fun typeLabel(type: String) = when (type.lowercase()) {
        "email" -> "Email address"; "password" -> "Password"; "tel" -> "Phone number"; "checkbox" -> "Checkbox"
        "number" -> "Number"; "textarea" -> "Message"; "search" -> "Search"; "date" -> "Date"; else -> "Text field"
    }

    private fun attr(tag: String, name: String): String? =
        Regex("""\b$name\s*=\s*(?:\{\s*)?["'`]([^"'`]+)["'`]""").find(tag)?.groupValues?.get(1)

    private fun humanize(s: String): String {
        val t = s.trim().replace(Regex("""[_\-]+"""), " ").replace(Regex("""([a-z])([A-Z])"""), "$1 $2").trim()
        return t.replaceFirstChar { it.uppercase() }.take(60)
    }

    // ---------- features & workflows ----------
    private val FEATURE_KEYWORDS = listOf(
        "Authentication" to listOf("login", "signin", "sign-in", "signup", "sign-up", "register", "auth", "password", "forgot", "reset", "verify", "otp"),
        "Dashboard" to listOf("dashboard", "overview", "analytics", "stats", "home"),
        "Settings & Profile" to listOf("settings", "preferences", "profile", "account"),
        "Products & Catalog" to listOf("product", "catalog", "inventory", "item"),
        "Orders & Checkout" to listOf("order", "invoice", "cart", "checkout", "receipt"),
        "Payments & Billing" to listOf("payment", "billing", "subscription", "pricing", "stripe", "paystack"),
        "Messaging" to listOf("message", "chat", "whatsapp", "notification", "inbox"),
        "Users & Teams" to listOf("user", "team", "member", "admin", "role")
    )

    private fun featureOf(route: String?, file: String): String {
        val key = ((route ?: "") + " " + file).lowercase()
        FEATURE_KEYWORDS.firstOrNull { (_, kws) -> kws.any { key.contains(it) } }?.let { return it.first }
        val seg = route?.split('/')?.firstOrNull { it.isNotEmpty() && !it.startsWith(":") }
            ?: file.substringAfterLast('/').substringBeforeLast('.')
        return if (seg.isEmpty()) "General" else humanize(seg)
    }

    private fun descriptionOf(name: String, screens: Int) = "$name — $screens screen${if (screens == 1) "" else "s"} found in source."
    private fun iconOf(name: String) = when (name) {
        "Authentication" -> "security"; "Products & Catalog" -> "inventory"; "Orders & Checkout" -> "receipt_long"
        "Payments & Billing" -> "credit_card"; "Messaging" -> "chat"; else -> "apps"
    }

    fun screenName(route: String?, file: String): String {
        if (route != null && route != "/") return humanize(route.trim('/').replace('/', ' ').replace(":", ""))
        if (route == "/") return "Home"
        return humanize(file.substringAfterLast('/').substringBeforeLast('.').replace(Regex("""^page$|^index$""", RegexOption.IGNORE_CASE), file.substringBeforeLast('/', "").substringAfterLast('/')))
    }

    private fun intentOf(s: Screen): Pair<String, String> {
        val k = ((s.route?.path ?: "") + " " + s.file).lowercase()
        return when {
            listOf("signup", "sign-up", "register").any { k.contains(it) } -> "signup" to "Create an account (Sign Up)"
            listOf("forgot", "reset").any { k.contains(it) } -> "reset" to "Reset a forgotten password"
            listOf("login", "signin", "sign-in").any { k.contains(it) } -> "login" to "Sign in"
            else -> "screen" to "Use the ${s.name} screen"
        }
    }

    private fun workflowFor(s: Screen, files: Map<String, CodebaseFile>): DetectedWorkflow? {
        val (kind, title) = intentOf(s)
        val steps = mutableListOf<WorkflowStepData>()
        val screen = s.name.ifBlank { "Screen" }
        s.route?.let { r ->
            steps.add(step("Open ${screen}", screen, "Navigate", "Go to ${r.path}", "CODE_VERIFIED", r.file, r.line, "route ${r.path}", files, r.file))
        }
        val inputs = s.elements.filter { it.kind == "input" }.distinctBy { it.label + it.line }.take(6)
        inputs.forEach { e ->
            val what = if (e.type == "password") "Enter your password" else "Enter ${e.label.lowercase()}"
            steps.add(step(what, screen, "Input", "Fill the '${e.label}' field", "CODE_VERIFIED", e.file, e.line, e.kind, files, e.file))
        }
        val buttons = s.elements.filter { it.kind == "button" }
        val submit = buttons.firstOrNull { it.type == "submit" || Regex("""(?i)sign|log|submit|save|create|register|send|continue|pay|confirm|add""").containsMatchIn(it.label) }
            ?: buttons.firstOrNull()
        submit?.let { e -> steps.add(step("Click ${e.label}", screen, "Click", "Press the '${e.label}' button", "CODE_VERIFIED", e.file, e.line, e.kind, files, e.file)) }
        if (steps.isEmpty()) return null
        return DetectedWorkflow(
            id = "$kind:${s.file}",
            name = title,
            description = "${steps.size} steps derived from ${s.file}",
            defaultSteps = steps
        )
    }

    private fun apiWorkflow(routes: List<RouteInfo>, files: Map<String, CodebaseFile>): DetectedWorkflow {
        val steps = routes.take(10).map { r ->
            val m = r.method.ifEmpty { "ANY" }
            step("$m ${r.path}", "API", "Request", "Explain what ${m} ${r.path} does", "CODE_VERIFIED", r.file, r.line, "$m ${r.path}", files, r.file)
        }
        return DetectedWorkflow("api:all", "REST API tour", "${routes.size} endpoints found in source", steps)
    }

    private fun step(
        title: String, screen: String, action: String, instruction: String, status: String,
        file: String, line: Int, element: String, files: Map<String, CodebaseFile>, snippetFile: String
    ): WorkflowStepData {
        val (snippet, hl) = snippetAround(files[snippetFile]?.content ?: "", line)
        val src = files[snippetFile]?.content?.lines()?.getOrNull(line - 1)?.trim()?.take(120)?.takeIf { it.isNotEmpty() } ?: element
        return WorkflowStepData(
            title = title, screenName = screen, actionType = action, defaultInstruction = instruction,
            verificationStatus = status, evidenceSource = "$file:$line", evidenceElement = src,
            screenDrawable = "", codeFilePath = file, codeSnippet = snippet, highlightedLines = hl
        )
    }

    /** Up to 9 real lines around [line]; returns snippet and the highlighted line number within it. */
    fun snippetAround(content: String, line: Int): Pair<String, String> {
        val lines = content.lines()
        if (lines.isEmpty()) return "" to "1"
        val at = line.coerceIn(1, lines.size)
        val from = maxOf(1, at - 3)
        val to = minOf(lines.size, at + 5)
        return lines.subList(from - 1, to).joinToString("\n") to (at - from + 1).toString()
    }

    // ---------- tutorial types & prompts ----------
    fun workflowForType(a: ProjectAnalysis, type: String): DetectedWorkflow? = when (type) {
        "SIGN_UP" -> a.findWorkflow("signup:")
        "LOGIN" -> a.findWorkflow("login:")
        "PASSWORD_RESET" -> a.findWorkflow("reset:")
        "DEVELOPER_GUIDE" -> a.findWorkflow("api:")
        "GETTING_STARTED", "PRODUCT_TOUR" -> tour(a)
        "ADMIN_GUIDE" -> a.features.firstOrNull { it.name.contains("Settings") || it.name.contains("Users") }?.workflows?.firstOrNull()
        "TROUBLESHOOTING" -> a.findWorkflow("reset:") ?: a.findWorkflow("login:")
        else -> a.features.firstOrNull { it.name != "Authentication" && it.name != "API Endpoints" }?.workflows?.firstOrNull() ?: a.workflows.firstOrNull()
    }

    private fun tour(a: ProjectAnalysis): DetectedWorkflow? {
        val nav = a.workflows.filter { it.id.startsWith("screen:") || it.id.startsWith("login:") }.mapNotNull { it.defaultSteps.firstOrNull { s -> s.actionType == "Navigate" } }
            .distinctBy { it.evidenceSource }.take(8)
        if (nav.isEmpty()) return null
        return DetectedWorkflow("tour:all", "Product tour", "Key screens of ${a.framework} app", nav)
    }

    /** Scores workflows by shared words with the prompt. Returns null when nothing in the codebase matches. */
    fun resolvePrompt(a: ProjectAnalysis, prompt: String): DetectedWorkflow? {
        val stop = setOf("the", "a", "an", "to", "how", "show", "their", "your", "my", "of", "for", "and", "in", "on", "customers", "users", "user", "do", "i", "can", "with", "is", "it", "me", "tutorial", "video")
        val words = prompt.lowercase().split(Regex("""[^a-z0-9]+""")).filter { it.length > 2 && it !in stop }.map { stem(it) }.toSet()
        if (words.isEmpty()) return null
        return a.workflows.map { w ->
            val hay = (w.name + " " + w.description + " " + w.id + " " + w.defaultSteps.joinToString(" ") { it.title + " " + it.evidenceSource }).lowercase()
            val tokens = hay.split(Regex("""[^a-z0-9]+""")).map { stem(it) }.toSet()
            w to words.count { it in tokens }
        }.filter { it.second > 0 }.maxByOrNull { it.second }?.first
    }

    private fun stem(w: String) = w.removeSuffix("ing").removeSuffix("es").removeSuffix("s").ifEmpty { w }
}
