package better.volume.slider.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class AppPreferencesStore(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val key = stringPreferencesKey("apps")

        private val json = Json { ignoreUnknownKeys = true }
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    @Serializable
    private data class SerializedState(
        val values: MutableList<AppPreferences>, val indices: MutableMap<String, Int>
    )

    private var state = SerializedState(mutableListOf(), mutableMapOf())
    val values: List<AppPreferences>
        get() = state.values
    val indices: Map<String, Int>
        get() = state.indices

    fun track(onChange: (first: Boolean) -> Unit) {
        var first = true

        scope.launch {
            dataStore.data.collect { preferences ->
                val valueJson = preferences[key]
                if (valueJson != null) {
                    state = json.decodeFromString<SerializedState>(valueJson)
                }

                onChange(first)
                @Suppress("AssignedValueIsNeverRead")
                first = false
            }
        }
    }

    fun getOrCreate(packageName: String): AppPreferences {
        synchronized(state) {
            val index = state.indices[packageName]
            if (index != null) {
                return state.values[index]
            }

            val value = AppPreferences()
            state.indices[packageName] = state.values.size
            state.values.add(value)
            return value
        }
    }

    fun save() {
        scope.launch {
            dataStore.edit { preferences ->
                preferences[key] = Json.encodeToString(state)
            }
        }
    }
}