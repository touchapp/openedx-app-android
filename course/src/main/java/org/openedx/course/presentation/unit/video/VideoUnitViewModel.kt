package org.openedx.course.presentation.unit.video

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.openedx.core.AppDataConstants
import org.openedx.core.BaseViewModel
import org.openedx.core.module.TranscriptManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseCompletionSet
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSubtitleLanguageChanged
import org.openedx.core.system.notifier.CourseVideoPositionChanged
import org.openedx.course.data.repository.CourseRepository
import subtitleFile.TimedTextObject

class VideoUnitViewModel(
    val courseId: String,
    private val courseRepository: CourseRepository,
    private val notifier: CourseNotifier,
    private val networkConnection: NetworkConnection,
    private val transcriptManager: TranscriptManager,
) : BaseViewModel() {

    var videoUrl = ""
    var transcripts = emptyMap<String, String>()
    var isPlaying = false
    var transcriptLanguage = AppDataConstants.defaultLocale.language ?: "en"
        private set

    var isDownloaded = false

    private val _currentVideoTime = MutableLiveData<Long>(0)
    val currentVideoTime: LiveData<Long>
        get() = _currentVideoTime

    private val _isUpdated = MutableLiveData(true)
    val isUpdated: LiveData<Boolean>
        get() = _isUpdated

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex = _currentIndex.asStateFlow()

    private val _transcriptObject = MutableLiveData<TimedTextObject?>()
    val transcriptObject: LiveData<TimedTextObject?>
        get() = _transcriptObject

    private var timeList: List<Long>? = emptyList()

    val hasInternetConnection: Boolean
        get() = networkConnection.isOnline()

    private var isBlockAlreadyCompleted = false

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        viewModelScope.launch {
            notifier.notifier.collect {
                if (it is CourseVideoPositionChanged && videoUrl == it.videoUrl) {
                    _isUpdated.value = false
                    _currentVideoTime.value = it.videoTime
                    _isUpdated.value = true
                    isPlaying = it.isPlaying
                } else if (it is CourseSubtitleLanguageChanged) {
                    transcriptLanguage = it.value
                    _transcriptObject.value = null
                    downloadSubtitles()
                }
            }
        }
    }

    fun downloadSubtitles() {
        viewModelScope.launch {
            transcriptManager.downloadTranscriptsForVideo(getTranscriptUrl())?.let { result ->
                _transcriptObject.value = result
                timeList = result.captions.values.toList()
                    .map { it.start.mseconds.toLong() }
            }
        }
    }

    private fun getTranscriptUrl(): String {
        val defaultTranscripts = transcripts[transcriptLanguage]
        if (!defaultTranscripts.isNullOrEmpty()) {
            return defaultTranscripts
        }
        if (transcripts.values.isNotEmpty()) {
            transcriptLanguage = transcripts.keys.toList().first()
            return transcripts[transcriptLanguage] ?: ""
        }
        return ""
    }


    fun markBlockCompleted(blockId: String) {
        if (!isBlockAlreadyCompleted) {
            viewModelScope.launch {
                try {
                    isBlockAlreadyCompleted = true
                    courseRepository.markBlocksCompletion(
                        courseId,
                        listOf(blockId)
                    )
                    notifier.send(CourseCompletionSet())
                } catch (e: Exception) {
                    isBlockAlreadyCompleted = false
                }
            }
        }
    }

    fun setCurrentVideoTime(value: Long) {
        _currentVideoTime.value = value
        timeList?.let {
            val index = it.indexOfLast { subtitleTime ->
                subtitleTime < value
            }
            if (index != currentIndex.value) {
                _currentIndex.value = index
            }
        }
    }

    fun getCurrentVideoTime() = currentVideoTime.value ?: 0

}