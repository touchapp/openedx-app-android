package org.openedx.whatsnew.presentation.whatsnew

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.presentation.global.AppData
import org.openedx.core.ui.WindowSize
import org.openedx.core.ui.calculateCurrentOffsetForPage
import org.openedx.core.ui.rememberWindowSize
import org.openedx.core.ui.statusBarsInset
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appTypography
import org.openedx.core.ui.windowSizeValue
import org.openedx.whatsnew.WhatsNewRouter
import org.openedx.whatsnew.data.storage.WhatsNewPreferences
import org.openedx.whatsnew.domain.model.WhatsNewItem
import org.openedx.whatsnew.domain.model.WhatsNewMessage
import org.openedx.whatsnew.presentation.ui.NavigationUnitsButtons
import org.openedx.whatsnew.presentation.ui.PageIndicator

class WhatsNewFragment : Fragment() {

    private val viewModel: WhatsNewViewModel by viewModel()
    private val preferencesManager by inject<WhatsNewPreferences>()
    private val router by inject<WhatsNewRouter>()
    private val appData: AppData by inject()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()
                val whatsNewItem = viewModel.whatsNewItem
                WhatsNewScreen(
                    windowSize = windowSize,
                    whatsNewItem = whatsNewItem.value,
                    onCloseClick = {
                        val versionName = appData.versionName
                        preferencesManager.lastWhatsNewVersion = versionName
                        router.navigateToMain(parentFragmentManager)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WhatsNewScreen(
    windowSize: WindowSize,
    whatsNewItem: WhatsNewItem?,
    onCloseClick: () -> Unit
) {
    whatsNewItem?.let { item ->
        OpenEdXTheme {
            val scaffoldState = rememberScaffoldState()
            val pagerState = rememberPagerState {
                whatsNewItem.messages.size
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                scaffoldState = scaffoldState,
                topBar = {
                    WhatsNewTopBar(
                        windowSize = windowSize,
                        onCloseClick = onCloseClick
                    )
                },
                content = { paddingValues ->
                    val configuration = LocalConfiguration.current
                    when (configuration.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE ->
                            WhatsNewScreenLandscape(
                                modifier = Modifier.padding(paddingValues),
                                whatsNewItem = item,
                                pagerState = pagerState,
                                onCloseClick = onCloseClick
                            )

                        else ->
                            WhatsNewScreenPortrait(
                                modifier = Modifier.padding(paddingValues),
                                whatsNewItem = item,
                                pagerState = pagerState,
                                onCloseClick = onCloseClick
                            )
                    }
                }
            )
        }
    }
}

@Composable
private fun WhatsNewTopBar(
    windowSize: WindowSize,
    onCloseClick: () -> Unit
) {
    val topBarWidth by remember(key1 = windowSize) {
        mutableStateOf(
            windowSize.windowSizeValue(
                expanded = Modifier.widthIn(Dp.Unspecified, 560.dp),
                compact = Modifier
                    .fillMaxWidth()
            )
        )
    }

    OpenEdXTheme {
        Column(
            Modifier
                .fillMaxWidth()
                .statusBarsInset(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .then(topBarWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(id = org.openedx.whatsnew.R.string.whats_new_title),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.appColors.textPrimary,
                    style = MaterialTheme.appTypography.titleMedium
                )
                IconButton(
                    modifier = Modifier.padding(end = 16.dp),
                    onClick = onCloseClick
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(id = org.openedx.core.R.string.core_cancel),
                        tint = MaterialTheme.appColors.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhatsNewScreenPortrait(
    modifier: Modifier = Modifier,
    whatsNewItem: WhatsNewItem,
    pagerState: PagerState,
    onCloseClick: () -> Unit
) {
    OpenEdXTheme {
        val coroutineScope = rememberCoroutineScope()
        val message = whatsNewItem.messages[pagerState.currentPage]

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.appColors.background),
            contentAlignment = Alignment.TopCenter
        ) {
            HorizontalPager(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.Top,
                state = pagerState
            ) { page ->
                val image = whatsNewItem.messages[page].image
                Image(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 36.dp, vertical = 48.dp),
                    painter = painterResource(id = image),
                    contentDescription = null
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 120.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    PageIndicator(
                        numberOfPages = pagerState.pageCount,
                        selectedPage = pagerState.currentPage,
                        defaultRadius = 12.dp,
                        selectedLength = 24.dp,
                        space = 4.dp,
                        animationDurationInMillis = 500,
                    )

                    Crossfade(
                        targetState = message,
                        modifier = Modifier.fillMaxWidth(),
                        label = ""
                    ) { targetText ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                text = targetText.title,
                                color = MaterialTheme.appColors.textPrimary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.appTypography.titleMedium
                            )
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp),
                                text = targetText.message,
                                color = MaterialTheme.appColors.textPrimary,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.appTypography.bodyMedium
                            )
                        }
                    }

                    NavigationUnitsButtons(
                        hasPrevPage = pagerState.canScrollBackward && pagerState.currentPage != 0,
                        hasNextPage = pagerState.canScrollForward,
                        onPrevClick = remember {
                            {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                        onNextClick = remember {
                            {
                                if (pagerState.canScrollForward) {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                } else {
                                    onCloseClick()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WhatsNewScreenLandscape(
    modifier: Modifier = Modifier,
    whatsNewItem: WhatsNewItem,
    pagerState: PagerState,
    onCloseClick: () -> Unit
) {
    OpenEdXTheme {
        val coroutineScope = rememberCoroutineScope()
        val message = whatsNewItem.messages[pagerState.currentPage]

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.appColors.background),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                HorizontalPager(
                    verticalAlignment = Alignment.CenterVertically,
                    state = pagerState
                ) { page ->
                    val image = whatsNewItem.messages[page].image
                    val alpha = (0.2f + pagerState.calculateCurrentOffsetForPage(page))*10
                    Image(
                        modifier = Modifier
                            .alpha(alpha)
                            .fillMaxHeight()
                            .padding(vertical = 24.dp)
                            .padding(start = 140.dp),
                        painter = painterResource(id = image),
                        contentDescription = null
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(400.dp)
                        .padding(end = 140.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        Crossfade(
                            targetState = message,
                            modifier = Modifier.fillMaxWidth(),
                            label = ""
                        ) { targetText ->
                            Column(
                                verticalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    text = targetText.title,
                                    color = MaterialTheme.appColors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.appTypography.titleMedium
                                )
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(80.dp),
                                    text = targetText.message,
                                    color = MaterialTheme.appColors.textPrimary,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.appTypography.bodyMedium
                                )
                            }
                        }

                        NavigationUnitsButtons(
                            hasPrevPage = pagerState.canScrollBackward && pagerState.currentPage != 0,
                            hasNextPage = pagerState.canScrollForward,
                            onPrevClick = remember {
                                {
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    }
                                }
                            },
                            onNextClick = remember {
                                {
                                    if (pagerState.canScrollForward) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    } else {
                                        onCloseClick()
                                    }
                                }
                            }
                        )
                    }
                }
            }

            PageIndicator(
                modifier = Modifier.weight(0.25f),
                numberOfPages = pagerState.pageCount,
                selectedPage = pagerState.currentPage,
                defaultRadius = 12.dp,
                selectedLength = 24.dp,
                space = 4.dp,
                animationDurationInMillis = 500,
            )
        }
    }
}

val whatsNewMessagePreview = WhatsNewMessage(
    image = org.openedx.core.R.drawable.core_no_image_course,
    title = "title",
    message = "Message message message"
)
val whatsNewItemPreview = WhatsNewItem(
    version = "1.0",
    messages = listOf(whatsNewMessagePreview, whatsNewMessagePreview, whatsNewMessagePreview)
)

@OptIn(ExperimentalFoundationApi::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WhatsNewPortraitPreview() {
    OpenEdXTheme {
        WhatsNewScreenPortrait(
            whatsNewItem = whatsNewItemPreview,
            onCloseClick = {},
            pagerState = rememberPagerState { 4 }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_NO, device = Devices.AUTOMOTIVE_1024p, widthDp = 720, heightDp = 360)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, device = Devices.AUTOMOTIVE_1024p, widthDp = 720, heightDp = 360)
@Composable
private fun WhatsNewLandscapePreview() {
    OpenEdXTheme {
        WhatsNewScreenLandscape(
            whatsNewItem = whatsNewItemPreview,
            onCloseClick = {},
            pagerState = rememberPagerState { 4 }
        )
    }
}
