package org.openedx.course.presentation.section

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
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
import org.openedx.core.UIMessage
import org.openedx.core.domain.model.Block
import org.openedx.core.domain.model.BlockCounts
import org.openedx.core.extension.serializable
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.ui.CourseSubsectionItem
import java.io.File

class CourseSectionFragment : Fragment() {

    private val viewModel by viewModel<CourseSectionViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }
    private val router by inject<CourseRouter>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        val blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        viewModel.mode = requireArguments().serializable(ARG_MODE)!!
        viewModel.getBlocks(blockId, viewModel.mode)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(CourseSectionUIState.Loading)
                val uiMessage by viewModel.uiMessage.observeAsState()
                CourseSectionScreen(
                    windowSize = windowSize,
                    uiState = uiState,
                    uiMessage = uiMessage,
                    onBackClick = {
                        requireActivity().supportFragmentManager.popBackStack()
                    },
                    onItemClick = { block ->
                        if (block.descendants.isNotEmpty()) {
                            viewModel.verticalClickedEvent(block.blockId, block.displayName)
                            router.navigateToCourseContainer(
                                requireActivity().supportFragmentManager,
                                block.id,
                                courseId = viewModel.courseId,
                                courseName = block.displayName,
                                mode = viewModel.mode
                            )
                        }
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
                                            .getString(org.openedx.core.R.string.app_name)
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
        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_MODE = "mode"
        fun newInstance(
            courseId: String,
            blockId: String,
            mode: CourseViewMode,
        ): CourseSectionFragment {
            val fragment = CourseSectionFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId,
                ARG_BLOCK_ID to blockId,
                ARG_MODE to mode
            )
            return fragment
        }
    }
}

@Composable
private fun CourseSectionScreen(
    windowSize: WindowSize,
    uiState: CourseSectionUIState,
    uiMessage: UIMessage?,
    onBackClick: () -> Unit,
    onItemClick: (Block) -> Unit,
    onDownloadClick: (Block) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val title = when (uiState) {
        is CourseSectionUIState.Blocks -> uiState.sectionName
        else -> ""
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        scaffoldState = scaffoldState,
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->

        val contentWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        HandleUIMessage(uiMessage = uiMessage, scaffoldState = scaffoldState)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .statusBarsInset(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(contentWidth) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .displayCutoutForLandscape()
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
                        text = title,
                        color = MaterialTheme.appColors.textPrimary,
                        style = MaterialTheme.appTypography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(6.dp))
                Surface(
                    color = MaterialTheme.appColors.background,
                    shape = MaterialTheme.appShapes.screenBackgroundShape
                ) {
                    when (uiState) {
                        is CourseSectionUIState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                            }
                        }

                        is CourseSectionUIState.Blocks -> {
                            Column(Modifier.fillMaxSize()) {
                                LazyColumn {
                                    items(uiState.blocks) { block ->
                                        CourseSubsectionItem(
                                            modifier = Modifier
                                                .padding(end = 20.dp),
                                            block = block,
                                            downloadedState = uiState.downloadedState[block.id],
                                            onClick = {
                                                onItemClick(it)
                                            },
                                            onDownloadClick = onDownloadClick
                                        )
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


@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CourseSectionScreenPreview() {
    OpenEdXTheme {
        CourseSectionScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = CourseSectionUIState.Blocks(
                listOf(
                    mockBlock,
                    mockBlock,
                    mockBlock,
                    mockBlock
                ),
                mapOf(),
                "",
                "Course default"
            ),
            uiMessage = null,
            onBackClick = {},
            onItemClick = {},
            onDownloadClick = {}
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun CourseSectionScreenTabletPreview() {
    OpenEdXTheme {
        CourseSectionScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = CourseSectionUIState.Blocks(
                listOf(
                    mockBlock,
                    mockBlock,
                    mockBlock,
                    mockBlock
                ),
                mapOf(),
                "",
                "Course default",
            ),
            uiMessage = null,
            onBackClick = {},
            onItemClick = {},
            onDownloadClick = {}
        )
    }
}

private val mockBlock = Block(
    id = "id",
    blockId = "blockId",
    lmsWebUrl = "lmsWebUrl",
    legacyWebUrl = "legacyWebUrl",
    studentViewUrl = "studentViewUrl",
    type = BlockType.HTML,
    displayName = "Block",
    graded = false,
    studentViewData = null,
    studentViewMultiDevice = false,
    blockCounts = BlockCounts(0),
    descendants = emptyList(),
    descendantsType = BlockType.HTML,
    completion = 0.0,
    containsGatedContent = false
)