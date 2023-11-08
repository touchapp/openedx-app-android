package org.openedx.course.presentation.outline

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.BlockType
import org.openedx.core.R
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.domain.model.CourseStructure
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.container.CourseContainerFragment
import org.openedx.course.presentation.outline.CourseOutlineFragment.Companion.getUnitBlockIcon
import org.openedx.course.presentation.ui.CourseImageHeader
import org.openedx.course.presentation.ui.CourseSectionCard
import org.openedx.course.presentation.ui.CourseSubsectionItem
import java.io.File
import java.util.Date

class CourseOutlineFragment : Fragment() {

    private val viewModel by viewModel<CourseOutlineViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        with(requireArguments()) {
            viewModel.courseTitle = getString(ARG_TITLE, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState()
                val uiMessage by viewModel.uiMessage.observeAsState()
                val refreshing by viewModel.isUpdating.observeAsState(false)

                CourseOutlineScreen(
                    windowSize = windowSize,
                    uiState = uiState!!,
                    courseTitle = viewModel.courseTitle,
                    uiMessage = uiMessage,
                    refreshing = refreshing,
                    onSwipeRefresh = {
                        viewModel.setIsUpdating()
                        (parentFragment as CourseContainerFragment).updateCourseStructure(true)
                    },
                    hasInternetConnection = viewModel.hasInternetConnection,
                    onReloadClick = {
                        (parentFragment as CourseContainerFragment).updateCourseStructure(false)
                    },
                    onItemClick = { block ->
                        if (viewModel.switchCourseSections(block.id)) {
                            viewModel.sequentialClickedEvent(block.blockId, block.displayName)
                        }
                    },
                    onSectionClick = { block ->
                        viewModel.verticalClickedEvent(block.blockId, block.displayName)
                        router.navigateToCourseContainer(
                            requireActivity().supportFragmentManager,
                            block.id,
                            courseId = viewModel.courseId,
                            courseName = block.displayName,
                            mode = CourseViewMode.FULL
                        )
                    },
                    onResumeClick = { blockId ->
                        viewModel.resumeSectionBlock?.let { sequential ->
                            viewModel.resumeCourseTappedEvent(sequential.blockId)
                            router.navigateToCourseSubsections(
                                requireActivity().supportFragmentManager,
                                viewModel.courseId,
                                sequential.id,
                                sequential.displayName,
                                CourseViewMode.FULL
                            )
                        }
                    },
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onDownloadClick = {
                        if (viewModel.isBlockDownloading(it.id)) {
                            viewModel.cancelWork(it.id)
                        } else if (viewModel.isBlockDownloaded(it.id)) {
                            viewModel.removeDownloadedModels(it.id)
                        } else {
                            viewModel.saveDownloadModels(
                                requireContext().externalCacheDir.toString() +
                                        File.separator +
                                        requireContext()
                                            .getString(R.string.app_name)
                                            .replace(Regex("\\s"), "_"), it.id
                            )
                        }
                    }
                )
            }
        }
    }


    companion object {
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_TITLE = "title"
        fun newInstance(
            courseId: String,
            title: String
        ): CourseOutlineFragment {
            val fragment = CourseOutlineFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_TITLE to title
            )
            return fragment
        }

        fun getUnitBlockIcon(block: Block): Int {
            return when (block.type) {
                BlockType.VIDEO -> org.openedx.course.R.drawable.ic_course_video
                BlockType.PROBLEM -> org.openedx.course.R.drawable.ic_course_pen
                BlockType.DISCUSSION -> org.openedx.course.R.drawable.ic_course_discussion
                else -> org.openedx.course.R.drawable.ic_course_block
            }
        }
    }
}


@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun CourseOutlineScreen(
    windowSize: WindowSize,
    uiState: CourseOutlineUIState,
    courseTitle: String,
    uiMessage: UIMessage?,
    refreshing: Boolean,
    hasInternetConnection: Boolean,
    onReloadClick: () -> Unit,
    onSwipeRefresh: () -> Unit,
    onItemClick: (Block) -> Unit,
    onSectionClick: (Block) -> Unit,
    onResumeClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onDownloadClick: (Block) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val pullRefreshState =
        rememberPullRefreshState(refreshing = refreshing, onRefresh = { onSwipeRefresh() })

    var isInternetConnectionShown by rememberSaveable {
        mutableStateOf(false)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) {

        val screenWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        val listBottomPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = PaddingValues(bottom = 24.dp),
                    compact = PaddingValues(bottom = 24.dp)
                )
            )
        }

        val listPadding by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.padding(horizontal = 6.dp),
                    compact = Modifier.padding(horizontal = 24.dp)
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(it)
                .statusBarsInset()
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                screenWidth
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .zIndex(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BackBtn {
                        onBackClick()
                    }
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        text = courseTitle,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.appColors.background
                ) {
                    Box(Modifier.pullRefresh(pullRefreshState)) {
                        when (uiState) {
                            is CourseOutlineUIState.Loading -> {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                }
                            }

                            is CourseOutlineUIState.CourseData -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = listBottomPadding
                                ) {
                                    item {
                                        CourseImageHeader(
                                            modifier = Modifier
                                                .aspectRatio(1.86f)
                                                .padding(6.dp),
                                            courseImage = uiState.courseStructure.media?.image?.large
                                                ?: "",
                                            courseCertificate = uiState.courseStructure.certificate
                                        )
                                    }
                                    if (uiState.resumeBlock != null) {
                                        item {
                                            Spacer(Modifier.height(28.dp))
                                            Box(listPadding) {
                                                if (windowSize.isTablet) {
                                                    ResumeCourseTablet(
                                                        block = uiState.resumeBlock,
                                                        onResumeClick = onResumeClick
                                                    )
                                                } else {
                                                    ResumeCourse(
                                                        block = uiState.resumeBlock,
                                                        onResumeClick = onResumeClick
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    uiState.courseStructure.blockData.forEach { block ->
                                        val courseSections = uiState.courseSections[block.id]

                                        item {
                                            Column(listPadding) {
                                                if (block.type == BlockType.CHAPTER) {
                                                    Text(
                                                        modifier = Modifier.padding(
                                                            top = 36.dp,
                                                            bottom = 8.dp
                                                        ),
                                                        text = block.displayName,
                                                        style = MaterialTheme.appTypography.titleMedium,
                                                        color = MaterialTheme.appColors.textPrimaryVariant
                                                    )
                                                } else {
                                                    CourseSectionCard(
                                                        block = block,
                                                        downloadedState = uiState.downloadedState[block.id],
                                                        onItemClick = { blockSelected ->
                                                            onItemClick(blockSelected)
                                                        },
                                                        onDownloadClick = onDownloadClick,
                                                        arrowDegrees = if (courseSections?.expanded == true) -90f else 90f
                                                    )
                                                    Divider()
                                                }
                                            }
                                        }


                                        courseSections?.blocks?.forEach { sectionBlock ->
                                            item {
                                                Column(listPadding) {
                                                    AnimatedVisibility(
                                                        visible = courseSections.expanded
                                                    ) {
                                                        Column {
                                                            CourseSubsectionItem(
                                                                block = sectionBlock,
                                                                downloadedState = uiState.downloadedState[block.id],
                                                                onClick = { sectionBlock ->
                                                                    onSectionClick(sectionBlock)
                                                                },
                                                                onDownloadClick = onDownloadClick
                                                            )
                                                            Box(
                                                                modifier = Modifier.padding(
                                                                    start = 20.dp
                                                                )
                                                            ) {
                                                                Divider()
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        PullRefreshIndicator(
                            refreshing,
                            pullRefreshState,
                            Modifier.align(Alignment.TopCenter)
                        )
                        if (!isInternetConnectionShown && !hasInternetConnection) {
                            OfflineModeDialog(
                                Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                onDismissCLick = {
                                    isInternetConnectionShown = true
                                },
                                onReloadClick = {
                                    isInternetConnectionShown = true
                                    onReloadClick()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumeCourse(
    block: Block,
    onResumeClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(id = org.openedx.course.R.string.course_continue_with),
            style = MaterialTheme.appTypography.labelMedium,
            color = MaterialTheme.appColors.textPrimaryVariant
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = getUnitBlockIcon(block)),
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary
            )
            Text(
                text = block.displayName,
                color = MaterialTheme.appColors.textPrimary,
                style = MaterialTheme.appTypography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(24.dp))
        OpenEdXButton(
            text = stringResource(id = org.openedx.course.R.string.course_continue),
            onClick = {
                onResumeClick(block.id)
            },
            content = {
                TextIcon(
                    text = stringResource(id = org.openedx.course.R.string.course_continue),
                    painter = painterResource(id = R.drawable.core_ic_forward),
                    color = MaterialTheme.appColors.buttonText,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            }
        )
    }
}


@Composable
private fun ResumeCourseTablet(
    block: Block,
    onResumeClick: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            Modifier
                .weight(1f)
                .padding(end = 35.dp)
        ) {
            Text(
                text = stringResource(id = org.openedx.course.R.string.course_continue_with),
                style = MaterialTheme.appTypography.labelMedium,
                color = MaterialTheme.appColors.textPrimaryVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    modifier = Modifier.size((MaterialTheme.appTypography.titleMedium.fontSize.value + 4).dp),
                    painter = painterResource(id = getUnitBlockIcon(block)),
                    contentDescription = null,
                    tint = MaterialTheme.appColors.textPrimary
                )
                Text(
                    text = block.displayName,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 4
                )
            }
        }
        OpenEdXButton(
            width = Modifier.width(194.dp),
            text = stringResource(id = org.openedx.course.R.string.course_continue),
            onClick = {
                onResumeClick(block.id)
            },
            content = {
                TextIcon(
                    text = stringResource(id = org.openedx.course.R.string.course_continue),
                    painter = painterResource(id = R.drawable.core_ic_forward),
                    color = MaterialTheme.appColors.buttonText,
                    textStyle = MaterialTheme.appTypography.labelLarge
                )
            }
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun CourseOutlineScreenPreview() {
    OpenEdXTheme {
        CourseOutlineScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseOutlineUIState.CourseData(
                mockCourseStructure,
                mapOf(),
                mockChapterBlock,
                mapOf()
            ),
            courseTitle = "",
            uiMessage = null,
            refreshing = false,
            hasInternetConnection = true,
            onSwipeRefresh = {},
            onItemClick = {},
            onSectionClick = {},
            onResumeClick = {},
            onBackClick = {},
            onReloadClick = {},
            onDownloadClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseOutlineScreenTabletPreview() {
    OpenEdXTheme {
        CourseOutlineScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseOutlineUIState.CourseData(
                mockCourseStructure,
                mapOf(),
                mockChapterBlock,
                mapOf()
            ),
            courseTitle = "",
            uiMessage = null,
            refreshing = false,
            hasInternetConnection = true,
            onSwipeRefresh = {},
            onItemClick = {},
            onSectionClick = {},
            onResumeClick = {},
            onBackClick = {},
            onReloadClick = {},
            onDownloadClick = {}
        )
    }
}

@Preview(uiMode = UI_MODE_NIGHT_NO)
@Preview(uiMode = UI_MODE_NIGHT_YES)
@Composable
private fun ResumeCoursePreview() {
    OpenEdXTheme {
        ResumeCourse(mockChapterBlock) {}
    }
}

private val mockChapterBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.CHAPTER,
    displayName = "Chapter",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    descendantsType = BlockType.CHAPTER,
    completion = 0.0
)
private val mockSequentialBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.SEQUENTIAL,
    displayName = "Sequential",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(1),
    descendants = emptyList(),
    descendantsType = BlockType.CHAPTER,
    completion = 0.0
)

private val mockCourseStructure = CourseStructure(
    root = "",
    blockData = listOf(mockSequentialBlock, mockSequentialBlock),
    id = "id",
    name = "Course name",
    number = "",
    org = "Org",
    start = Date(),
    startDisplay = "",
    startType = "",
    end = Date(),
    coursewareAccess = CoursewareAccess(
        true,
        "",
        "",
        "",
        "",
        ""
    ),
    media = null,
    certificate = null,
    isSelfPaced = false
)
