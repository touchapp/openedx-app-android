package org.openedx.course.presentation.outline

import org.openedx.core.domain.model.Block

data class CourseSection(val blocks: List<Block>, var expanded: Boolean = false)