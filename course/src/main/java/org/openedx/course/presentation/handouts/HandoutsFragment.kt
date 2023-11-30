package org.openedx.course.presentation.handouts

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
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
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.ui.CardArrow
import org.koin.android.ext.android.inject
import org.openedx.course.R as courseR

class HandoutsFragment : Fragment() {

    private val router by inject<CourseRouter>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                HandoutsScreen(
                    windowSize = windowSize,
                    onHandoutsClick = {
                        router.navigateToHandoutsWebView(
                            requireActivity().supportFragmentManager,
                            requireArguments().getString(ARG_COURSE_ID, ""),
                            getString(courseR.string.course_handouts),
                            HandoutsType.Handouts
                        )
                    },
                    onAnnouncementsClick = {
                        router.navigateToHandoutsWebView(
                            requireActivity().supportFragmentManager,
                            requireArguments().getString(ARG_COURSE_ID, ""),
                            getString(courseR.string.course_announcements),
                            HandoutsType.Announcements
                        )
                    })
            }
        }
    }

    companion object {
        private const val ARG_COURSE_ID = "argCourseId"
        fun newInstance(courseId: String): HandoutsFragment {
            val fragment = HandoutsFragment()
            fragment.arguments = bundleOf(
                ARG_COURSE_ID to courseId
            )
            return fragment
        }
    }

}

@Composable
private fun HandoutsScreen(
    windowSize: WindowSize,
    onHandoutsClick: () -> Unit,
    onAnnouncementsClick: () -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(it)
                .displayCutoutForLandscape(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(screenWidth) {
                Surface(
                    color = MaterialTheme.appColors.background
                ) {
                    LazyColumn(
                        Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 10.dp, horizontal = 24.dp)
                    ) {
                        item {
                            HandoutsItem(
                                title = stringResource(id = courseR.string.course_handouts),
                                description = stringResource(id = courseR.string.course_find_important_info),
                                painter = painterResource(id = courseR.drawable.course_ic_handouts),
                                onClick = onHandoutsClick
                            )
                        }
                        item {
                            HandoutsItem(
                                title = stringResource(id = courseR.string.course_announcements),
                                description = stringResource(id = courseR.string.course_latest_news),
                                painter = painterResource(id = courseR.drawable.course_ic_announcements),
                                onClick = onAnnouncementsClick
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HandoutsItem(
    title: String,
    description: String,
    painter: Painter,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painter,
                contentDescription = null,
                tint = MaterialTheme.appColors.textPrimary
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.appTypography.titleSmall,
                    color = MaterialTheme.appColors.textPrimary
                )
                Text(
                    text = description,
                    style = MaterialTheme.appTypography.labelSmall,
                    color = MaterialTheme.appColors.textFieldHint
                )
            }
        }
        CardArrow(degrees = 0f)
    }
    Divider()
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HandoutsScreenPreview() {
    OpenEdXTheme {
        HandoutsScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            onHandoutsClick = {}, onAnnouncementsClick = {})
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.NEXUS_9)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.NEXUS_9)
@Composable
private fun HandoutsScreenTabletPreview() {
    OpenEdXTheme {
        HandoutsScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            onHandoutsClick = {}, onAnnouncementsClick = {})
    }
}