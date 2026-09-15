package com.example.progressiontimer.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.progressiontimer.data.AppDatabase
import com.example.progressiontimer.data.EventFolder
import com.example.progressiontimer.data.TimeRecord
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TimerViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).timerDao()

    val folders: StateFlow<List<EventFolder>> = dao.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val allRecords: StateFlow<List<TimeRecord>> = dao.observeAllRecords()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _timeMillis = MutableStateFlow(0L)
    val timeMillis: StateFlow<Long> = _timeMillis.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _selectedFolderId = MutableStateFlow<Int?>(null)
    val selectedFolderId: StateFlow<Int?> = _selectedFolderId.asStateFlow()

    private var timerJob: Job? = null
    private var startElapsedMillis = 0L

    init {
        seedDefaultFolders()
    }

    fun startTimer() {
        if (_isRunning.value) return

        _isRunning.value = true
        startElapsedMillis = SystemClock.elapsedRealtime() - _timeMillis.value
        timerJob = viewModelScope.launch {
            while (isActive) {
                _timeMillis.value = SystemClock.elapsedRealtime() - startElapsedMillis
                delay(10L)
            }
        }
    }

    fun stopTimer() {
        if (_isRunning.value) {
            _timeMillis.value = SystemClock.elapsedRealtime() - startElapsedMillis
        }
        _isRunning.value = false
        timerJob?.cancel()
        timerJob = null
    }

    fun resetTimer() {
        stopTimer()
        _timeMillis.value = 0L
    }

    fun saveCurrentTime() {
        val folderId = _selectedFolderId.value ?: return
        val elapsed = _timeMillis.value
        if (elapsed <= 0L) return

        viewModelScope.launch {
            dao.insertTime(TimeRecord(folderId = folderId, timeInMillis = elapsed))
            resetTimer()
        }
    }

    fun createFolder(name: String, isHigherBetter: Boolean) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return

        viewModelScope.launch {
            val id = dao.insertFolder(
                EventFolder(
                    name = cleanName,
                    isHigherBetter = isHigherBetter,
                    sortOrder = dao.maxSortOrder() + 1,
                ),
            ).toInt()
            _selectedFolderId.value = id
        }
    }

    fun selectFolder(folderId: Int) {
        _selectedFolderId.value = folderId
    }

    fun ensureFolderSelected(currentFolders: List<EventFolder>) {
        val selected = _selectedFolderId.value
        if (currentFolders.isEmpty()) {
            _selectedFolderId.value = null
            return
        }
        if (selected == null || currentFolders.none { it.id == selected }) {
            _selectedFolderId.value = currentFolders.first().id
        }
    }

    fun observeFolder(folderId: Int) = dao.observeFolder(folderId)

    fun observeRecords(folderId: Int) = dao.observeRecords(folderId)

    fun deleteRecord(recordId: Int) {
        viewModelScope.launch {
            dao.deleteRecord(recordId)
        }
    }

    fun deleteFolder(folderId: Int) {
        if (_selectedFolderId.value == folderId) {
            resetTimer()
            _selectedFolderId.value = null
        }

        viewModelScope.launch {
            dao.deleteFolder(folderId)
        }
    }

    fun moveFolderUp(folderId: Int) {
        moveFolder(folderId, -1)
    }

    fun moveFolderDown(folderId: Int) {
        moveFolder(folderId, 1)
    }

    private fun moveFolder(folderId: Int, direction: Int) {
        val currentFolders = folders.value
        val currentIndex = currentFolders.indexOfFirst { it.id == folderId }
        val otherIndex = currentIndex + direction

        if (currentIndex == -1 || otherIndex !in currentFolders.indices) return

        val currentFolder = currentFolders[currentIndex]
        val otherFolder = currentFolders[otherIndex]

        viewModelScope.launch {
            dao.updateFolderSortOrder(currentFolder.id, otherFolder.sortOrder)
            dao.updateFolderSortOrder(otherFolder.id, currentFolder.sortOrder)
        }
    }

    private fun seedDefaultFolders() {
        viewModelScope.launch {
            if (dao.folderCount() > 0) return@launch

            dao.insertFolder(EventFolder(name = "Holding Breath", isHigherBetter = true, sortOrder = 0))
            dao.insertFolder(EventFolder(name = "Plank Hold", isHigherBetter = true, sortOrder = 1))
            dao.insertFolder(EventFolder(name = "Reciting Alphabet", isHigherBetter = false, sortOrder = 2))
            dao.insertFolder(EventFolder(name = "Speed Cubing", isHigherBetter = false, sortOrder = 3))
        }
    }

    override fun onCleared() {
        timerJob?.cancel()
        super.onCleared()
    }
}
