package health.telomer.android.feature.actionplan

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import javax.inject.Inject

private val Context.actionPlanDataStore: DataStore<Preferences> by preferencesDataStore(name = "action_plan")
private val CHECKED_ITEMS_KEY = stringPreferencesKey("checked_items")

data class ActionPlanItem(
    val text: String,
    val isChecked: Boolean = false,
)

data class ActionPlanSection(
    val title: String,
    val items: List<ActionPlanItem>,
)

data class ActionPlanUiState(
    val isLoading: Boolean = true,
    val sections: List<ActionPlanSection> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
)

@HiltViewModel
class ActionPlanViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActionPlanUiState())
    val uiState: StateFlow<ActionPlanUiState> = _uiState.asStateFlow()

    private var checkedKeys = mutableSetOf<String>()

    companion object {
        private val PLACEHOLDER_PLAN = """
## Objectifs de la semaine

- [ ] Marcher 30 minutes par jour
- [ ] Prendre vitamine D 4000 UI/jour
- [ ] Méditation 10 minutes le matin
- [ ] 2L d'eau minimum par jour
- [ ] Coucher avant 23h

## Examens à programmer

- [ ] Bilan sanguin complet
- [ ] Échographie thyroïdienne
- [ ] Consultation dermatologie

## Suivi nutritionnel

- [ ] Réduire les sucres raffinés
- [ ] Augmenter les protéines (1.6g/kg)
- [ ] Oméga-3 : 2g EPA+DHA/jour
""".trimIndent()
    }

    init {
        loadPlan()
    }

    private fun loadPlan() {
        viewModelScope.launch {
            // Load saved checked state from DataStore
            context.actionPlanDataStore.data.first().let { prefs ->
                val savedJson = prefs[CHECKED_ITEMS_KEY]
                if (savedJson != null) {
                    try {
                        val arr = JSONArray(savedJson)
                        for (i in 0 until arr.length()) {
                            checkedKeys.add(arr.getString(i))
                        }
                    } catch (_: Exception) {}
                }
            }

            // Parse the plan (placeholder for now, will use API later)
            val sections = parsePlan(PLACEHOLDER_PLAN)
            updateState(sections)
        }
    }

    private fun parsePlan(markdown: String): List<ActionPlanSection> {
        val sections = mutableListOf<ActionPlanSection>()
        var currentTitle = ""
        var currentItems = mutableListOf<ActionPlanItem>()

        for (line in markdown.lines()) {
            when {
                line.startsWith("## ") -> {
                    if (currentTitle.isNotEmpty() && currentItems.isNotEmpty()) {
                        sections.add(ActionPlanSection(currentTitle, currentItems.toList()))
                    }
                    currentTitle = line.removePrefix("## ").trim()
                    currentItems = mutableListOf()
                }
                line.startsWith("- [ ] ") || line.startsWith("- [x] ") -> {
                    val text = line.removePrefix("- [ ] ").removePrefix("- [x] ").trim()
                    val key = "$currentTitle::$text"
                    val isChecked = checkedKeys.contains(key)
                    currentItems.add(ActionPlanItem(text, isChecked))
                }
            }
        }
        if (currentTitle.isNotEmpty() && currentItems.isNotEmpty()) {
            sections.add(ActionPlanSection(currentTitle, currentItems.toList()))
        }

        return sections
    }

    fun toggleItem(sectionTitle: String, itemIndex: Int) {
        val sections = _uiState.value.sections.toMutableList()
        val sectionIdx = sections.indexOfFirst { it.title == sectionTitle }
        if (sectionIdx < 0) return

        val section = sections[sectionIdx]
        val items = section.items.toMutableList()
        val item = items[itemIndex]
        val key = "$sectionTitle::${item.text}"

        if (item.isChecked) {
            checkedKeys.remove(key)
        } else {
            checkedKeys.add(key)
        }

        items[itemIndex] = item.copy(isChecked = !item.isChecked)
        sections[sectionIdx] = section.copy(items = items)
        updateState(sections)

        // Persist to DataStore
        viewModelScope.launch {
            context.actionPlanDataStore.edit { prefs ->
                val arr = JSONArray()
                checkedKeys.forEach { arr.put(it) }
                prefs[CHECKED_ITEMS_KEY] = arr.toString()
            }
        }
    }

    private fun updateState(sections: List<ActionPlanSection>) {
        val allItems = sections.flatMap { it.items }
        _uiState.value = ActionPlanUiState(
            isLoading = false,
            sections = sections,
            completedCount = allItems.count { it.isChecked },
            totalCount = allItems.size,
        )
    }
}
