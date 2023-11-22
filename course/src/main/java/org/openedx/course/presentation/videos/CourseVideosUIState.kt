package org.openedx.course.presentation.videos

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadedState
import org.openedx.course.presentation.outline.CourseSection

sealed class CourseVideosUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
        val courseSections: Map<String, List<Block>>,
        val courseSectionsState: Map<String, Boolean>
    ) : CourseVideosUIState()

    data class Empty(val message: String) : CourseVideosUIState()
    object Loading : CourseVideosUIState()
}