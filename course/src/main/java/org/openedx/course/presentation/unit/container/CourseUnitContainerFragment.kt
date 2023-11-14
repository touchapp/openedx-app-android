package org.openedx.course.presentation.unit.container

import android.os.Bundle
import android.os.SystemClock
import android.view.View
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import org.openedx.core.BlockType
import org.openedx.core.domain.model.Block
import org.openedx.core.extension.serializable
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.global.InsetHolder
import org.openedx.core.presentation.global.viewBinding
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.course.R
import org.openedx.course.databinding.FragmentCourseUnitContainerBinding
import org.openedx.course.presentation.ChapterEndFragmentDialog
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.DialogListener
import org.openedx.course.presentation.ui.CourseUnitToolbar
import org.openedx.course.presentation.ui.NavigationUnitsButtons
import org.openedx.course.presentation.ui.UnitSectionsList
import org.openedx.course.presentation.ui.VideoTitle

class CourseUnitContainerFragment : Fragment(R.layout.fragment_course_unit_container) {

    private val binding by viewBinding(FragmentCourseUnitContainerBinding::bind)

    private val viewModel by viewModel<CourseUnitContainerViewModel> {
        parametersOf(requireArguments().getString(ARG_COURSE_ID, ""))
    }

    private val router by inject<CourseRouter>()

    private var blockId: String = ""

    private lateinit var adapter: CourseUnitContainerAdapter

    private var lastClickTime = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycle.addObserver(viewModel)
        blockId = requireArguments().getString(ARG_BLOCK_ID, "")
        viewModel.loadBlocks(requireArguments().serializable(ARG_MODE)!!)
        viewModel.setupCurrentIndex(blockId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val insetHolder = requireActivity() as InsetHolder
        val containerParams = binding.viewPager.layoutParams as ConstraintLayout.LayoutParams
        containerParams.bottomMargin = insetHolder.bottomInset
        binding.viewPager.layoutParams = containerParams

        initViewPager()
        if (savedInstanceState == null) {
            val currentBlockIndex = viewModel.getUnitBlocks().indexOfFirst {
                viewModel.getCurrentBlock().id == it.id
            }
            if (currentBlockIndex != -1) {
                binding.viewPager.currentItem = currentBlockIndex
            }
        }

        binding.cvVideoTitle?.setContent {
            OpenEdXTheme {
                val block by viewModel.currentBlock.observeAsState()
                if (block?.type == BlockType.VIDEO) {
                    VideoTitle(
                        text = block?.displayName ?: "",
                        modifier = Modifier.statusBarsPadding()
                    )
                }
            }
        }

        binding.cvNavigationBar.setContent {
            NavigationBar()
        }

        binding.btnBack.setContent {
            val currentSection = viewModel.getSectionsBlocks().first { it.id == blockId }
            val title = viewModel.getModuleBlock(currentSection.id).displayName
            val sectionName = currentSection.displayName
            val blockShowed by viewModel.selectBlockDialogShowed.observeAsState()

            val index by viewModel.indexInContainer.observeAsState(1)
            val units by viewModel.verticalBlockCounts.observeAsState(1)

            CourseUnitToolbar(
                title = title,
                numberOfPages = units,
                selectedPage = index,
                sectionName = sectionName,
                blockListShowed = blockShowed,
                onBlockClick = { handleSectionClick() },
                onBackClick = {
                    requireActivity().supportFragmentManager.popBackStack()
                }
            )
        }

        binding.sectionsBlocksBg?.setOnClickListener { handleSectionClick() }

        binding.sectionsBlocksList?.setContent {
            val index = viewModel.getSectionsBlocks().indexOfFirst { it.id == blockId }
            UnitSectionsList(
                sectionsBlocks = viewModel.getSectionsBlocks(),
                selectedSection = index
            ) { block ->
                proceedToNextSection(block)
            }
        }
    }

    private fun updateNavigationButtons(updatedData: (String, Boolean, Boolean) -> Unit) {
        val hasPrevBlock: Boolean = !viewModel.isFirstIndexInContainer
        val hasNextBlock: Boolean
        val nextButtonText = if (viewModel.isLastIndexInContainer) {
            hasNextBlock = false
            getString(R.string.course_navigation_finish)
        } else {
            hasNextBlock = true
            getString(R.string.course_navigation_next)
        }
        updatedData(nextButtonText, hasPrevBlock, hasNextBlock)
    }

    private fun restrictDoubleClick(): Boolean {
        if (SystemClock.elapsedRealtime() - lastClickTime < 500) {
            return true
        }
        lastClickTime = SystemClock.elapsedRealtime()
        return false
    }

    private fun initViewPager() {
        binding.viewPager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.viewPager.offscreenPageLimit = 1
        adapter = CourseUnitContainerAdapter(this, viewModel, viewModel.getUnitBlocks())
        binding.viewPager.adapter = adapter
        binding.viewPager.isUserInputEnabled = false
    }

    private fun handlePrevClick(
        prevIndex: Int = viewModel.currentIndex - 1,
        buttonChanged: (String, Boolean, Boolean) -> Unit
    ) {
        if (!restrictDoubleClick()) {
            val block = viewModel.moveToBlock(prevIndex)
            if (block != null) {
                viewModel.prevBlockClickedEvent(block.blockId, block.displayName)
                if (!block.type.isContainer()) {
                    binding.viewPager.setCurrentItem(
                        viewModel.currentIndex, true
                    )
                    updateNavigationButtons { next, hasPrev, hasNext ->
                        buttonChanged(next, hasPrev, hasNext)
                    }
                }
            }
        }
    }

    private fun handleNextClick(
        nextIndex: Int = viewModel.currentIndex + 1,
        buttonChanged: (String, Boolean, Boolean) -> Unit
    ) {
        if (!restrictDoubleClick()) {
            val block = viewModel.moveToBlock(nextIndex)
            if (block != null) {
                viewModel.nextBlockClickedEvent(block.blockId, block.displayName)
                if (!block.type.isContainer()) {
                    binding.viewPager.setCurrentItem(
                        viewModel.currentIndex, true
                    )
                    updateNavigationButtons { next, hasPrev, hasNext ->
                        buttonChanged(next, hasPrev, hasNext)
                    }
                }
            } else {
                val currentVerticalBlock = viewModel.getCurrentVerticalBlock()
                val nextVerticalBlock = viewModel.getNextVerticalBlock()
                val dialog = ChapterEndFragmentDialog.newInstance(
                    currentVerticalBlock?.displayName ?: "",
                    nextVerticalBlock?.displayName ?: ""
                )
                currentVerticalBlock?.let {
                    viewModel.finishVerticalClickedEvent(
                        it.blockId,
                        it.displayName
                    )
                }
                dialog.listener = object : DialogListener {
                    override fun <T> onClick(value: T) {
                        viewModel.proceedToNext()
                        val nextBlock = viewModel.getCurrentVerticalBlock()
                        nextBlock?.let {
                            viewModel.finishVerticalNextClickedEvent(
                                it.blockId,
                                it.displayName
                            )
                            proceedToNextSection(it)
                        }
                    }

                    override fun onDismiss() {
                        viewModel.finishVerticalBackClickedEvent()
                    }
                }
                dialog.show(
                    requireActivity().supportFragmentManager,
                    ChapterEndFragmentDialog::class.simpleName
                )
            }
        }
    }

    private fun handleSectionClick() {
        if (binding.sectionsBlocksCardView?.visibility == View.VISIBLE) {
            binding.sectionsBlocksCardView?.visibility = View.GONE
            binding.sectionsBlocksBg?.visibility = View.GONE
            viewModel.hideSelectBlockDialog()

        } else {
            binding.sectionsBlocksCardView?.visibility = View.VISIBLE
            binding.sectionsBlocksBg?.visibility = View.VISIBLE
            viewModel.showSelectBlockDialog()
        }
    }

    private fun proceedToNextSection(nextBlock: Block) {
        if (nextBlock.type.isContainer()) {
            router.replaceCourseContainer(
                requireActivity().supportFragmentManager,
                nextBlock.id,
                viewModel.courseId,
                requireArguments().getString(ARG_COURSE_NAME, ""),
                requireArguments().serializable(ARG_MODE)!!
            )
        }
    }

    @Composable
    private fun NavigationBar() {
        OpenEdXTheme {
            var nextButtonText by rememberSaveable {
                mutableStateOf(viewModel.nextButtonText)
            }
            var hasNextBlock by rememberSaveable {
                mutableStateOf(viewModel.hasNextBlock)
            }
            var hasPrevBlock by rememberSaveable {
                mutableStateOf(viewModel.hasNextBlock)
            }

            updateNavigationButtons { next, hasPrev, hasNext ->
                nextButtonText = next
                hasPrevBlock = hasPrev
                hasNextBlock = hasNext
            }
            val windowSize = rememberWindowSize()

            NavigationUnitsButtons(
                windowSize = windowSize,
                hasPrevBlock = hasPrevBlock,
                nextButtonText = nextButtonText,
                hasNextBlock = hasNextBlock,
                onPrevClick = {
                    handlePrevClick { next, hasPrev, hasNext ->
                        nextButtonText = next
                        hasPrevBlock = hasPrev
                        hasNextBlock = hasNext
                    }
                },
                onNextClick = {
                    handleNextClick { next, hasPrev, hasNext ->
                        nextButtonText = next
                        hasPrevBlock = hasPrev
                        hasNextBlock = hasNext
                    }
                }
            )
        }
    }

    companion object {

        private const val ARG_BLOCK_ID = "blockId"
        private const val ARG_COURSE_ID = "courseId"
        private const val ARG_COURSE_NAME = "courseName"
        private const val ARG_MODE = "mode"

        fun newInstance(
            blockId: String,
            courseId: String,
            courseName: String,
            mode: CourseViewMode,
        ): CourseUnitContainerFragment {
            val fragment = CourseUnitContainerFragment()
            fragment.arguments = bundleOf(
                ARG_BLOCK_ID to blockId,
                ARG_COURSE_ID to courseId,
                ARG_COURSE_NAME to courseName,
                ARG_MODE to mode
            )
            return fragment
        }
    }

}