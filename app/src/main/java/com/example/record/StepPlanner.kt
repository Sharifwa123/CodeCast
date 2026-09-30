package com.example.record

import com.example.data.model.TutorialStepEntity

/** What to look for on the live page, derived from the source line of the step's evidence. */
data class ElementSpec(
    val kind: String,            // "input" | "button"
    val name: String = "",
    val id: String = "",
    val placeholder: String = "",
    val type: String = "",
    val label: String = "",
    val text: String = "",
    val submit: Boolean = false,
    val index: Int = -1          // position on the live page when known
)

sealed class PlannedAction {
    data class Navigate(val url: String, val route: String) : PlannedAction()
    data class Type(val spec: ElementSpec, val text: String) : PlannedAction()
    data class Click(val spec: ElementSpec, val real: Boolean) : PlannedAction()
    data class Skip(val reason: String) : PlannedAction()
}

object StepPlanner {
    fun plan(step: TutorialStepEntity, baseUrl: String, allowRealClicks: Boolean): PlannedAction {
        val base = baseUrl.trim().removeSuffix("/")
        return when (step.actionType) {
            "Navigate" -> {
                val route = routeOf(step) ?: return PlannedAction.Skip("No route found for '${step.title}'")
                if (route.contains(":")) return PlannedAction.Skip("Route $route needs an id; open it from a list instead")
                PlannedAction.Navigate(base + (if (route == "/") "" else route), route)
            }
            "Input" -> {
                val spec = specOf(step, "input")
                PlannedAction.Type(spec, sampleValue(spec))
            }
            "Click" -> PlannedAction.Click(specOf(step, "button"), allowRealClicks)
            else -> PlannedAction.Skip("'${step.actionType}' steps can't be shown in a browser")
        }
    }

    fun routeOf(step: TutorialStepEntity): String? =
        Regex("""^Go to (/\S*)""").find(step.instruction)?.groupValues?.get(1)

    /** Builds a spec from the step title and the real source line (e.g. `<input name="email" type="email" placeholder="Work email">`). */
    fun specOf(step: TutorialStepEntity, kind: String): ElementSpec {
        val src = step.evidenceElement
        fun attr(n: String) = Regex("""\b$n\s*=\s*(?:\{\s*)?["'`]([^"'`]+)["'`]""").find(src)?.groupValues?.get(1) ?: ""
        val quoted = Regex("""'([^']+)'""").find(step.instruction)?.groupValues?.get(1) ?: ""
        return if (kind == "input") ElementSpec(
            kind = "input", name = attr("name"), id = attr("id"), placeholder = attr("placeholder"), type = attr("type").lowercase(),
            label = quoted.ifBlank { step.title.removePrefix("Enter ").removePrefix("your ") }
        ) else ElementSpec(
            kind = "button", id = attr("id"), text = quoted.ifBlank { step.title.removePrefix("Click ") },
            submit = attr("type").equals("submit", true)
        )
    }

    fun sampleValue(s: ElementSpec): String {
        val key = (s.type + " " + s.name + " " + s.label + " " + s.placeholder).lowercase()
        return when {
            "email" in key -> "demo@example.com"
            "pass" in key -> "Demo#12345"
            "phone" in key || "tel" in key -> "0241234567"
            "search" in key -> "invoice"
            "company" in key || "business name" in key || "organi" in key -> "Acme Inc"
            "name" in key -> "Alex Morgan"
            "number" in key || "amount" in key || "price" in key -> "42"
            "message" in key || "comment" in key || "description" in key -> "This is a sample message."
            else -> "Sample text"
        }
    }
}
