package org.openedx.course.presentation.unit.container

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.openedx.core.BaseViewModel
import org.openedx.core.BlockType
import org.openedx.core.domain.model.Block
import org.openedx.core.extension.clearAndAddAll
import org.openedx.core.extension.indexOfFirstFromIndex
import org.openedx.core.module.db.DownloadModel
import org.openedx.core.module.db.DownloadedState
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.core.system.notifier.CourseSectionChanged
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.CourseAnalytics

class CourseUnitContainerViewModel(
    private val interactor: CourseInteractor,
    private val notifier: CourseNotifier,
    private val analytics: CourseAnalytics,
    val courseId: String
) : BaseViewModel() {

    private val blocks = ArrayList<Block>()

    var currentIndex = 0
    private var currentVerticalIndex = 0
    private var currentSectionIndex = -1

    val isFirstIndexInContainer: Boolean
        get() {
            return descendants.firstOrNull() == descendants.getOrNull(currentIndex)
        }

    val isLastIndexInContainer: Boolean
        get() {
            return descendants.lastOrNull() == descendants.getOrNull(currentIndex)
        }

    private val _verticalBlockCounts = MutableLiveData<Int>()
    val verticalBlockCounts: LiveData<Int>
        get() = _verticalBlockCounts

    private val _indexInContainer = MutableLiveData<Int>()
    val indexInContainer: LiveData<Int>
        get() = _indexInContainer

    private val _currentBlock = MutableLiveData<Block?>()
    val currentBlock: LiveData<Block?>
        get() = _currentBlock

    private val _selectBlockDialogShowed = MutableLiveData<Boolean>()
    val selectBlockDialogShowed: LiveData<Boolean>
        get() = _selectBlockDialogShowed

    var nextButtonText = ""
    var hasNextBlock = false
    private var courseName = ""

    private val descendants = mutableListOf<String>()
    private val descendantsBlocks = mutableListOf<Block>()
    private val sectionsBlocks = mutableListOf<Block>()

    fun loadBlocks(mode: CourseViewMode) {
        try {
            val courseStructure = when (mode) {
                CourseViewMode.FULL -> interactor.getCourseStructureFromCache()
                CourseViewMode.VIDEOS -> interactor.getCourseStructureForVideos()
            }
            val blocks = courseStructure.blockData
            courseName = courseStructure.name
            this.blocks.clearAndAddAll(blocks)
        } catch (e: Exception) {
            //ignore e.printStackTrace()
        }
    }

    init {
        _indexInContainer.value = 0
    }

    fun setupCurrentIndex(blockId: String) {
        if (currentSectionIndex != -1) {
            return
        }
        blocks.forEachIndexed { index, block ->
            if (block.id == blockId) {
                currentVerticalIndex = index
                currentSectionIndex = blocks.indexOfFirst {
                    it.descendants.contains(blocks[currentVerticalIndex].id)
                }
                if (block.descendants.isNotEmpty()) {
                    descendants.clearAndAddAll(block.descendants)
                    descendantsBlocks.clearAndAddAll(blocks.filter { block.descendants.contains(it.id) })
                    sectionsBlocks.clearAndAddAll(getSectionsBlocks(blocks, getSectionId(blockId)))

                } else {
                    setNextVerticalIndex()
                }
                if (currentVerticalIndex != -1) {
                    _verticalBlockCounts.value = blocks[currentVerticalIndex].descendants.size
                }
                if (block.descendants.isNotEmpty()) {
                    _currentBlock.value = blocks.first { it.id == block.descendants.first() }
                }
                return
            }
        }
    }

    private fun getSectionId(blockId: String): String {
        return blocks.firstOrNull { it.descendants.contains(blockId) }?.id ?: ""
    }

    private fun getSectionsBlocks(blocks: List<Block>, id: String): List<Block> {
        val resultList = mutableListOf<Block>()
        if (blocks.isEmpty()) return emptyList()
        val selectedBlock = blocks.first {
            it.id == id
        }
        for (descendant in selectedBlock.descendants) {
            val blockDescendant = blocks.find {
                it.id == descendant
            }
            if (blockDescendant != null) {
                if (blockDescendant.type == BlockType.VERTICAL) {
                    resultList.add(blockDescendant)
                }
            } else continue
        }
        return resultList
    }

    private fun setNextVerticalIndex() {
        currentVerticalIndex = blocks.indexOfFirstFromIndex(currentVerticalIndex) {
            it.type == BlockType.VERTICAL
        }
    }

    fun proceedToNext() {
        currentVerticalIndex = blocks.indexOfFirstFromIndex(currentVerticalIndex) {
            it.type == BlockType.VERTICAL
        }
        if (currentVerticalIndex != -1) {
            val sectionIndex = blocks.indexOfFirst {
                it.descendants.contains(blocks[currentVerticalIndex].id)
            }
            if (sectionIndex != currentSectionIndex) {
                currentSectionIndex = sectionIndex
                blocks.getOrNull(currentSectionIndex)?.id?.let {
                    sendCourseSectionChanged(it)
                }
            }
        }
    }

    fun getDownloadModelById(id: String): DownloadModel? = runBlocking(Dispatchers.IO) {
        return@runBlocking interactor.getDownloadModels().first()
            .find { it.id == id && it.downloadedState == DownloadedState.DOWNLOADED }
    }

    fun getCurrentBlock(): Block {
        return blocks[currentIndex]
    }

    fun moveToBlock(index: Int): Block? {
        descendantsBlocks.getOrNull(index)?.let { block ->
            currentIndex = index
            if (currentVerticalIndex != -1) {
                _indexInContainer.value = currentIndex
            }
            _currentBlock.value = block
            return block
        }
        return null
    }

    private fun sendCourseSectionChanged(blockId: String) {
        viewModelScope.launch {
            notifier.send(CourseSectionChanged(blockId))
        }
    }

    fun getCurrentVerticalBlock(): Block? = blocks.getOrNull(currentVerticalIndex)

    fun getNextVerticalBlock(): Block? {
        val index = blocks.indexOfFirstFromIndex(currentVerticalIndex) {
            it.type == BlockType.VERTICAL
        }
        return blocks.getOrNull(index)
    }

    fun getUnitBlocks(): List<Block> = blocks.filter { descendants.contains(it.id) }

    fun getSectionsBlocks(): List<Block> = sectionsBlocks

    fun getModuleBlock(sectionBlockId: String): Block {
        return blocks.first { it.descendants.contains(sectionBlockId) }
    }

    fun nextBlockClickedEvent(blockId: String, blockName: String) {
        analytics.nextBlockClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun prevBlockClickedEvent(blockId: String, blockName: String) {
        analytics.prevBlockClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun finishVerticalClickedEvent(blockId: String, blockName: String) {
        analytics.finishVerticalClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun finishVerticalNextClickedEvent(blockId: String, blockName: String) {
        analytics.finishVerticalNextClickedEvent(courseId, courseName, blockId, blockName)
    }

    fun finishVerticalBackClickedEvent() {
        analytics.finishVerticalBackClickedEvent(courseId, courseName)
    }

    fun showSelectBlockDialog() {
        _selectBlockDialogShowed.value = true
    }

    fun hideSelectBlockDialog() {
        _selectBlockDialogShowed.value = false
    }
}