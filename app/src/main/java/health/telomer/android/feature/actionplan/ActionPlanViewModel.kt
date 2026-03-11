package health.telomer.android.feature.actionplan

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import health.telomer.android.core.data.api.TelomerApi
import health.telomer.android.core.data.local.ActionPlanCheck
import health.telomer.android.core.data.local.ActionPlanCheckDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── Pillar enum ──────────────────────────────────────────────
enum class Pillar(val emoji: String, val label: String, val color: Color) {
    ALIMENTATION("🥗", "Alimentation", Color(0xFF10B981)),       // green
    ACTIVITE("🏃", "Activité physique", Color(0xFF0596DE)),      // blue
    SOMMEIL("😴", "Sommeil", Color(0xFF8B5CF6)),                 // violet
    EMOTIONNEL("🧠", "Santé émotionnelle & sociale", Color(0xFFF59E0B)), // orange
    SUPPLEMENTS("💊", "Suppléments", Color(0xFFEF4444)),         // red
}

data class ActionPlanItem(
    val text: String,
    val isChecked: Boolean = false,
    val globalIndex: Int = 0, // index across the whole plan
)

data class ActionPlanSection(
    val pillar: Pillar,
    val items: List<ActionPlanItem>,
    val isExpanded: Boolean = true,
) {
    val completedCount: Int get() = items.count { it.isChecked }
    val totalCount: Int get() = items.size
    val progress: Float get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
}

data class ActionPlanUiState(
    val isLoading: Boolean = true,
    val hasNoPlan: Boolean = false,
    val planId: String? = null,
    val sections: List<ActionPlanSection> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val error: String? = null,
) {
    val completedCount: Int get() = sections.sumOf { it.completedCount }
    val totalCount: Int get() = sections.sumOf { it.totalCount }
    val progress: Float get() = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
}

@HiltViewModel
class ActionPlanViewModel @Inject constructor(
    private val api: TelomerApi,
    private val checkDao: ActionPlanCheckDao,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActionPlanUiState())
    val uiState: StateFlow<ActionPlanUiState> = _uiState.asStateFlow()

    private var rawContent: String = ""

    init {
        loadPlan()
    }

    fun selectDate(date: LocalDate) {
        _uiState.update { it.copy(selectedDate = date) }
        val planId = _uiState.value.planId ?: return
        observeChecks(planId, date)
    }

    fun previousDay() {
        selectDate(_uiState.value.selectedDate.minusDays(1))
    }

    fun nextDay() {
        val next = _uiState.value.selectedDate.plusDays(1)
        if (!next.isAfter(LocalDate.now())) {
            selectDate(next)
        }
    }

    fun toggleSection(pillar: Pillar) {
        _uiState.update { state ->
            state.copy(sections = state.sections.map {
                if (it.pillar == pillar) it.copy(isExpanded = !it.isExpanded) else it
            })
        }
    }

    fun toggleItem(globalIndex: Int) {
        val state = _uiState.value
        val planId = state.planId ?: return
        val date = state.selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        // Find and toggle
        val updatedSections = state.sections.map { section ->
            section.copy(items = section.items.map { item ->
                if (item.globalIndex == globalIndex) item.copy(isChecked = !item.isChecked) else item
            })
        }
        _uiState.update { it.copy(sections = updatedSections) }

        // Persist
        val item = updatedSections.flatMap { it.items }.first { it.globalIndex == globalIndex }
        viewModelScope.launch {
            checkDao.upsertCheck(
                ActionPlanCheck(
                    id = "${planId}_${globalIndex}_$date",
                    planId = planId,
                    itemIndex = globalIndex,
                    date = date,
                    isChecked = item.isChecked,
                )
            )
        }
    }

    private fun loadPlan() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val plans = api.getMyActionPlans()
                if (plans.isEmpty()) {
                    _uiState.update { it.copy(isLoading = false, hasNoPlan = true) }
                    return@launch
                }
                // Use the most recent plan
                val plan = plans.maxByOrNull { it.createdAt } ?: plans.first()
                rawContent = plan.content
                _uiState.update { it.copy(planId = plan.id) }
                observeChecks(plan.id, _uiState.value.selectedDate)
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, hasNoPlan = true, error = e.message) }
            }
        }
    }

    private fun observeChecks(planId: String, date: LocalDate) {
        val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
        viewModelScope.launch {
            checkDao.getChecks(planId, dateStr).collect { checks ->
                val checkedIndices = checks.filter { it.isChecked }.map { it.itemIndex }.toSet()
                val sections = parsePlanContent(rawContent, checkedIndices)
                _uiState.update {
                    it.copy(isLoading = false, hasNoPlan = false, sections = sections)
                }
            }
        }
    }

    /**
     * Parse markdown content into 5-pillar sections.
     * Expected format: sections starting with ## containing pillar keywords,
     * items as "- [ ] text" or "- text".
     */
    private fun parsePlanContent(
        markdown: String,
        checkedIndices: Set<Int>,
    ): List<ActionPlanSection> {
        val pillarItems = Pillar.entries.associateWith { mutableListOf<String>() }
        var currentPillar: Pillar? = null
        var globalIndex = 0

        for (line in markdown.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("## ") || trimmed.startsWith("# ")) {
                val heading = trimmed.removePrefix("## ").removePrefix("# ").lowercase()
                currentPillar = when {
                    heading.contains("alimentation") || heading.contains("nutrition") -> Pillar.ALIMENTATION
                    heading.contains("activit") || heading.contains("physique") || heading.contains("exercice") || heading.contains("sport") -> Pillar.ACTIVITE
                    heading.contains("sommeil") || heading.contains("repos") || heading.contains("sleep") -> Pillar.SOMMEIL
                    heading.contains("motionnel") || heading.contains("mental") || heading.contains("social") || heading.contains("stress") -> Pillar.EMOTIONNEL
                    heading.contains("suppl") || heading.contains("compl") || heading.contains("vitamine") -> Pillar.SUPPLEMENTS
                    else -> null
                }
            } else if (trimmed.startsWith("- ") && currentPillar != null) {
                val text = trimmed
                    .removePrefix("- [ ] ")
                    .removePrefix("- [x] ")
                    .removePrefix("- [X] ")
                    .removePrefix("- ")
                    .trim()
                if (text.isNotBlank()) {
                    pillarItems[currentPillar]?.add(text)
                }
            }
        }

        return Pillar.entries.mapNotNull { pillar ->
            val texts = pillarItems[pillar] ?: emptyList()
            if (texts.isEmpty()) return@mapNotNull null
            val items = texts.map { text ->
                val idx = globalIndex++
                ActionPlanItem(
                    text = text,
                    isChecked = idx in checkedIndices,
                    globalIndex = idx,
                )
            }
            ActionPlanSection(pillar = pillar, items = items)
        }
    }
}
