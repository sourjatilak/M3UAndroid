package com.m3u.business.setting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.m3u.data.worker.PresetImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PresetImportViewModel @Inject constructor(
    private val presetImporter: PresetImporter,
) : ViewModel() {

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing

    /** 0..100 percentage, -1 = indeterminate */
    private val _progress = MutableStateFlow(-1)
    val progress: StateFlow<Int> = _progress

    private val _statusText = MutableStateFlow("")
    val statusText: StateFlow<String> = _statusText

    private var started = false

    init {
        viewModelScope.launch {
            val pending = presetImporter.pendingPresets()
            if (pending.isNotEmpty()) {
                _showDialog.value = true
                _statusText.value = "${pending.size} playlist(s) to import"
            }
        }
    }

    fun importAll() {
        if (started) return
        started = true
        _importing.value = true
        viewModelScope.launch {
            val pending = presetImporter.pendingPresets()
            for ((index, preset) in pending.withIndex()) {
                _statusText.value = "Importing ${preset.title} (${index + 1}/${pending.size})"
                try {
                    presetImporter.importPreset(preset) { completed, total ->
                        if (total > 0) {
                            _progress.value = (completed * 100 / total).coerceIn(0, 100)
                            _statusText.value = "Importing ${preset.title}: $completed/$total (${_progress.value}%)"
                        }
                    }
                } catch (_: Exception) { }
            }
            _importing.value = false
            _progress.value = 100
            _statusText.value = "Import complete"
            _showDialog.value = false
        }
    }
}
