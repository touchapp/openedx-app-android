package org.openedx.course.presentation.outline

import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.module.db.DownloadedState

sealed class CourseOutlineUIState {
    data class CourseData(
        val courseStructure: CourseStructure,
        val downloadedState: Map<String, DownloadedState>,
        val resumeBlock: Block?,
        val courseSections: Map<String, List<Block>>,
        val courseSectionsState: Map<String, Boolean>
    ) : CourseOutlineUIState()

    object Loading : CourseOutlineUIState()
}
