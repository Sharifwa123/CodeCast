package com.example.analysis

import org.junit.Assert.*
import org.junit.Test

class AnalyzerTest {
    private val zip = Fixtures.read("shop-main", *Fixtures.nextApp)
    private val analysis = CodebaseAnalyzer.analyze(zip.files)

    @Test fun zipReaderKeepsSourceOnly_stripsRoot_andCountsEntries() {
        assertTrue(zip.files.none { it.path.contains("node_modules") || it.path.endsWith(".png") })
        assertTrue(zip.files.any { it.path == "src/app/login/page.tsx" })
        assertEquals(8, zip.entryCount)
    }

    @Test(expected = IllegalArgumentException::class) fun archiveWithoutSourceIsAnError_notDemoData() {
        Fixtures.read("", "logo.png" to "x", "notes.txt" to "y")
    }

    @Test fun detectsFrameworkAndStack() {
        assertEquals("Next.js", analysis.framework)
        assertTrue(analysis.techStack.containsAll(listOf("TypeScript", "TailwindCSS", "Stripe")))
    }

    @Test fun routesComeFromFilesystemAndHandlers() {
        val screens = analysis.routes.filter { it.kind == "screen" }.map { it.path }.toSet()
        assertEquals(setOf("/", "/signup", "/login", "/orders/:id"), screens)
        val api = analysis.routes.filter { it.kind == "api" }
        assertEquals(setOf("GET", "POST"), api.map { it.method }.toSet())
        assertTrue(api.all { it.path == "/api/orders" })
    }

    @Test fun signUpWorkflowIsBuiltFromTheRealForm() {
        val wf = analysis.findWorkflow("signup:")!!
        assertEquals(listOf("Open Signup", "Enter work email", "Enter your password", "Click Create account"), wf.defaultSteps.map { it.title })
        val email = wf.defaultSteps[1]
        assertEquals("src/app/(auth)/signup/page.tsx:5", email.evidenceSource)
        assertTrue(email.codeSnippet.contains("name=\"email\""))
        assertTrue(email.evidenceElement.contains("type=\"email\""))
        // highlighted line really is the evidence line inside the snippet
        val hl = email.highlightedLines.toInt()
        assertTrue(email.codeSnippet.lines()[hl - 1].contains("name=\"email\""))
    }

    @Test fun neverClaimsRuntimeVerification() {
        assertTrue(analysis.workflows.flatMap { it.defaultSteps }.none { it.verificationStatus == "RUNTIME_VERIFIED" })
        assertTrue(analysis.workflows.flatMap { it.defaultSteps }.all { it.codeFilePath.isNotEmpty() && it.codeSnippet.isNotEmpty() })
    }

    @Test fun tutorialTypesMapToRealWorkflowsOrNull() {
        assertEquals("Sign in", CodebaseAnalyzer.workflowForType(analysis, "LOGIN")!!.name)
        assertNull("no reset flow exists in this project", CodebaseAnalyzer.workflowForType(analysis, "PASSWORD_RESET"))
        assertTrue(CodebaseAnalyzer.workflowForType(analysis, "DEVELOPER_GUIDE")!!.defaultSteps.any { it.title == "POST /api/orders" })
    }

    @Test fun customPromptMatchesOrReturnsNothing() {
        val w = CodebaseAnalyzer.resolvePrompt(analysis, "Show customers how to download their invoice.")
        assertNotNull(w)
        assertTrue(w!!.defaultSteps.any { it.evidenceSource.startsWith("src/app/orders") })
        assertNull(CodebaseAnalyzer.resolvePrompt(analysis, "configure kubernetes autoscaling"))
    }

    @Test fun flaskAndFlutterAreUnderstood() {
        val py = CodebaseAnalyzer.analyze(Fixtures.read("", "requirements.txt" to "flask==3.0", "app.py" to "@app.route('/health')\ndef h():\n    return 'ok'\n@app.get('/items')\ndef i(): pass\n").files)
        assertEquals("Flask", py.framework)
        assertEquals(setOf("/health", "/items"), py.routes.map { it.path }.toSet())
        val dart = CodebaseAnalyzer.analyze(Fixtures.read("", "pubspec.yaml" to "name: x", "lib/login_screen.dart" to "class L {\n  build() {\n    TextField(decoration: InputDecoration(labelText: 'Email')),\n    ElevatedButton(onPressed: go, child: Text('Log in'))\n  }\n}\n").files)
        assertEquals("Flutter", dart.framework)
        assertEquals(listOf("Enter email", "Click Log in"), dart.findWorkflow("login:")!!.defaultSteps.map { it.title })
    }

    @Test fun repoUrlParsing() {
        assertEquals("https://codeload.github.com/o/r/zip/HEAD", RepoUrl.archiveUrls("https://github.com/o/r.git").first())
        assertEquals("https://codeload.github.com/o/r/zip/dev", RepoUrl.archiveUrls("https://github.com/o/r/tree/dev").first())
        assertTrue(RepoUrl.archiveUrls("https://gitlab.com/o/r").first().contains("/-/archive/HEAD/"))
        assertTrue(RepoUrl.archiveUrls("https://example.com/o/r").isEmpty())
    }

    @Test fun labelsAndButtonsAreReadFromRealJsxPatterns() {
        val tsx = """
export default function Register() {
  return (
    <form onSubmit={submit} className="auth-form">
      <label><span className="field-label">Business email</span><input type="email" value={email} onChange={e => setEmail(e.target.value)} className="field" required /></label>
      <label><span className="field-label">Password</span><PasswordField value={password} onChange={setPassword} /></label>
      <button className="btn" disabled={loading}>{loading ? "Creating…" : "Create account"}</button>
    </form>
  );
}
""".trimStart()
        val a = CodebaseAnalyzer.analyze(Fixtures.read("", "package.json" to """{"dependencies":{"react":"18"}}""", "frontend/pages/register.tsx" to tsx, "frontend/pages/login.tsx" to tsx.replace("Register", "Login")).files)
        val wf = a.workflows.first { it.id.startsWith("signup:") }
        assertEquals(listOf("Open Register", "Enter business email", "Enter password", "Click Create account"), wf.defaultSteps.map { it.title })
    }
}
