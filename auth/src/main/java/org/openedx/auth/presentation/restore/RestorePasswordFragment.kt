package org.openedx.auth.presentation.restore

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import org.openedx.auth.presentation.ui.LoginTextField
import org.openedx.core.UIMessage
import org.openedx.core.ui.*
import org.openedx.core.ui.theme.OpenEdXTheme
import org.openedx.core.ui.theme.appColors
import org.openedx.core.ui.theme.appShapes
import org.openedx.core.ui.theme.appTypography
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.openedx.core.presentation.global.app_upgrade.AppUpgradeRequiredScreen
import org.openedx.core.AppUpdateState
import org.openedx.auth.R as authR

class RestorePasswordFragment : Fragment() {

    private val viewModel: RestorePasswordViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OpenEdXTheme {
                val windowSize = rememberWindowSize()

                val uiState by viewModel.uiState.observeAsState(RestorePasswordUIState.Initial)
                val uiMessage by viewModel.uiMessage.observeAsState()
                val appUpgradeEvent by viewModel.appUpgradeEventUIState.observeAsState(null)

                if (appUpgradeEvent == null) {
                    RestorePasswordScreen(
                        windowSize = windowSize,
                        uiState = uiState,
                        uiMessage = uiMessage,
                        onBackClick = {
                            requireActivity().supportFragmentManager.popBackStackImmediate()
                        },
                        onRestoreButtonClick = {
                            viewModel.passwordReset(it)
                        }
                    )
                } else {
                    AppUpgradeRequiredScreen(
                        onUpdateClick = {
                            AppUpdateState.openPlayMarket(requireContext())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RestorePasswordScreen(
    windowSize: WindowSize,
    uiState: RestorePasswordUIState,
    uiMessage: UIMessage?,
    onBackClick: () -> Unit,
    onRestoreButtonClick: (String) -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val scrollState = rememberScrollState()
    var email by rememberSaveable {
        mutableStateOf("")
    }

    Scaffold(
        scaffoldState = scaffoldState,
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        backgroundColor = MaterialTheme.appColors.background
    ) { paddingValues ->

        val contentPaddings by remember {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier
                        .widthIn(Dp.Unspecified, 420.dp)
                        .padding(
                            top = 32.dp,
                            bottom = 40.dp
                        ),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                )
            )
        }

        val topBarWidth by remember {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier
                        .widthIn(Dp.Unspecified, 560.dp)
                        .padding(bottom = 24.dp),
                    compact = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                )
            )
        }

        val buttonWidth by remember(key1 = windowSize) {
            mutableStateOf(
                windowSize.windowSizeValue(
                    expanded = Modifier
                        .widthIn(232.dp, Dp.Unspecified),
                    compact = Modifier.fillMaxWidth()
                )
            )
        }

        Image(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            painter = painterResource(id = org.openedx.core.R.drawable.core_top_header),
            contentScale = ContentScale.FillBounds,
            contentDescription = null
        )

        HandleUIMessage(
            uiMessage = uiMessage,
            scaffoldState = scaffoldState
        )

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .statusBarsInset(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .then(topBarWidth),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResource(id = authR.string.auth_forgot_your_password),
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.appTypography.titleMedium
                )
                BackBtn(
                    modifier = Modifier
                        .padding(end = 16.dp),
                    tint = Color.White
                ) {
                    onBackClick()
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth(),
                color = MaterialTheme.appColors.background,
                shape = MaterialTheme.appShapes.screenBackgroundShape
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .background(MaterialTheme.appColors.background),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (uiState) {
                        RestorePasswordUIState.Initial, RestorePasswordUIState.Loading -> {
                            Column(
                                Modifier
                                    .then(contentPaddings)
                                    .displayCutoutForLandscape()
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    text = stringResource(id = authR.string.auth_forgot_your_password),
                                    style = MaterialTheme.appTypography.displaySmall,
                                    color = MaterialTheme.appColors.textPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    text = stringResource(id = authR.string.auth_please_enter_your_log_in),
                                    style = MaterialTheme.appTypography.titleSmall,
                                    color = MaterialTheme.appColors.textPrimary
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                LoginTextField(
                                    modifier = Modifier.fillMaxWidth(),
                                    onValueChanged = {
                                        email = it
                                    },
                                    imeAction = ImeAction.Done,
                                    keyboardActions = {
                                        it.clearFocus()
                                        onRestoreButtonClick(email)
                                    }
                                )
                                Spacer(Modifier.height(50.dp))
                                if (uiState == RestorePasswordUIState.Loading) {
                                    Box(
                                        modifier = Modifier
                                            .padding(bottom = 48.dp)
                                            .fillMaxWidth()
                                            .height(42.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(color = MaterialTheme.appColors.primary)
                                    }
                                } else {
                                    OpenEdXButton(
                                        width = buttonWidth,
                                        text = stringResource(id = authR.string.auth_reset_password),
                                        onClick = {
                                            onRestoreButtonClick(email)
                                        }
                                    )
                                }
                            }
                        }

                        is RestorePasswordUIState.Success -> {
                            Column(
                                Modifier
                                    .then(contentPaddings)
                                    .displayCutoutForLandscape()
                                    .fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    modifier = Modifier.size(100.dp),
                                    painter = painterResource(id = authR.drawable.auth_ic_email),
                                    contentDescription = null,
                                    tint = MaterialTheme.appColors.textPrimary
                                )
                                Spacer(Modifier.height(48.dp))
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    text = stringResource(id = authR.string.auth_check_your_email),
                                    style = MaterialTheme.appTypography.titleLarge,
                                    color = MaterialTheme.appColors.textPrimary
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    modifier = Modifier
                                        .fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    text = stringResource(
                                        authR.string.auth_restore_password_success,
                                        uiState.email
                                    ),
                                    style = MaterialTheme.appTypography.bodyMedium,
                                    color = MaterialTheme.appColors.textPrimary
                                )
                                Spacer(Modifier.height(48.dp))
                                OpenEdXButton(
                                    width = buttonWidth,
                                    text = stringResource(id = authR.string.auth_sign_in),
                                    onClick = {
                                        onBackClick()
                                    }
                                )
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
@Preview(name = "NEXUS_5_Light", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_5_Dark", device = Devices.NEXUS_5, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RestorePasswordPreview() {
    OpenEdXTheme {
        RestorePasswordScreen(
            windowSize = WindowSize(WindowType.Compact, WindowType.Compact),
            uiState = RestorePasswordUIState.Initial,
            uiMessage = null,
            onBackClick = {},
            onRestoreButtonClick = {}
        )
    }
}

@Preview(name = "NEXUS_9_Light", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Preview(name = "NEXUS_9_Dark", device = Devices.NEXUS_9, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun RestorePasswordTabletPreview() {
    OpenEdXTheme {
        RestorePasswordScreen(
            windowSize = WindowSize(WindowType.Medium, WindowType.Medium),
            uiState = RestorePasswordUIState.Initial,
            uiMessage = null,
            onBackClick = {},
            onRestoreButtonClick = {}
        )
    }
}