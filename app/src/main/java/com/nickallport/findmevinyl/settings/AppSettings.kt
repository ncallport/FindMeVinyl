package com.nickallport.findmevinyl.settings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The settings as they are now. Screens read [settings], and it changes the moment something is
 * [update]d, which also saves it.
 */
class AppSettings(private val store: SettingsStore) {
    private val current = MutableStateFlow(store.load())

    val settings: StateFlow<Settings> = current.asStateFlow()

    @Synchronized
    fun update(transform: (Settings) -> Settings) = set(transform(current.value))

    @Synchronized
    fun set(next: Settings) {
        store.save(next)
        current.value = next
    }
}
