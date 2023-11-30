package org.openedx.auth.presentation.signin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.openedx.auth.R
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.core.UIMessage
import org.openedx.core.Validator
import org.openedx.core.data.model.User
import org.openedx.core.system.EdxError
import org.openedx.core.system.ResourceManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.system.notifier.AppUpgradeNotifier
import java.net.UnknownHostException
import org.openedx.core.R as CoreRes

@ExperimentalCoroutinesApi
class SignInViewModelTest {

    @get:Rule
    val testInstantTaskExecutorRule: TestRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private val validator = mockk<Validator>()
    private val resourceManager = mockk<ResourceManager>()
    private val preferencesManager = mockk<CorePreferences>()
    private val interactor = mockk<AuthInteractor>()
    private val analytics = mockk<AuthAnalytics>()
    private val appUpgradeNotifier = mockk<AppUpgradeNotifier>()

    private val invalidCredential = "Invalid credentials"
    private val noInternet = "Slow or no internet connection"
    private val somethingWrong = "Something went wrong"
    private val invalidEmail = "Invalid email"
    private val invalidPassword = "Password too short"

    private val user = User(0, "", "", "")

    @Before
    fun before() {
        Dispatchers.setMain(dispatcher)
        every { resourceManager.getString(CoreRes.string.core_error_invalid_grant) } returns invalidCredential
        every { resourceManager.getString(CoreRes.string.core_error_no_connection) } returns noInternet
        every { resourceManager.getString(CoreRes.string.core_error_unknown_error) } returns somethingWrong
        every { resourceManager.getString(R.string.auth_invalid_email) } returns invalidEmail
        every { resourceManager.getString(R.string.auth_invalid_password) } returns invalidPassword
        every { appUpgradeNotifier.notifier } returns emptyFlow()
    }

    @After
    fun after() {
        Dispatchers.resetMain()
    }

    @Test
    fun `login empty credentials validation error`() = runTest {
        every { validator.isEmailValid(any()) } returns false
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        val viewModel = SignInViewModel(interactor, resourceManager, preferencesManager, validator, analytics, appUpgradeNotifier)
        viewModel.login("", "")
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage

        assertEquals(invalidEmail, message.message)
        assert(viewModel.showProgress.value != true)
        assert(viewModel.loginSuccess.value != true)
    }

    @Test
    fun `login invalid email validation error`() = runTest {
        every { validator.isEmailValid(any()) } returns false
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        val viewModel =
            SignInViewModel(interactor, resourceManager, preferencesManager, validator, analytics, appUpgradeNotifier)
        viewModel.login("acc@test.o", "")
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage

        assertEquals(invalidEmail, message.message)
        assert(viewModel.showProgress.value != true)
        assert(viewModel.loginSuccess.value != true)
    }

    @Test
    fun `login empty password validation error`() = runTest {
        every { validator.isEmailValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns false
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        coVerify(exactly = 0) { interactor.login(any(), any()) }
        val viewModel =
            SignInViewModel(interactor, resourceManager, preferencesManager, validator, analytics, appUpgradeNotifier)
        viewModel.login("acc@test.org", "")

        verify(exactly = 0) { analytics.setUserIdForSession(any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        assertEquals(invalidPassword, message.message)
        assert(viewModel.showProgress.value != true)
        assert(viewModel.loginSuccess.value != true)
    }

    @Test
    fun `login invalid password validation error`() = runTest {
        every { validator.isEmailValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns false
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        val viewModel =
            SignInViewModel(interactor, resourceManager, preferencesManager, validator, analytics, appUpgradeNotifier)
        viewModel.login("acc@test.org", "ed")

        coVerify(exactly = 0) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        assertEquals(invalidPassword, message.message)
        assert(viewModel.showProgress.value != true)
        assert(viewModel.loginSuccess.value != true)
    }

    @Test
    fun `login success`() = runTest {
        every { validator.isEmailValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns true
        every { analytics.userLoginEvent(any()) } returns Unit
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        val viewModel = SignInViewModel(interactor, resourceManager, preferencesManager, validator, analytics, appUpgradeNotifier)
        coEvery { interactor.login("acc@test.org", "edx") } returns Unit
        viewModel.login("acc@test.org", "edx")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 1) { analytics.userLoginEvent(any()) }
        verify(exactly = 1) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        assertEquals(false, viewModel.showProgress.value)
        assertEquals(true, viewModel.loginSuccess.value)
        assertEquals(null, viewModel.uiMessage.value)
    }

    @Test
    fun `login network error`() = runTest {
        every { validator.isEmailValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns true
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        val viewModel =
            SignInViewModel(interactor, resourceManager, preferencesManager, validator, analytics, appUpgradeNotifier)
        coEvery { interactor.login("acc@test.org", "edx") } throws UnknownHostException()
        viewModel.login("acc@test.org", "edx")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        val message = viewModel.uiMessage.value as? UIMessage.SnackBarMessage
        assertEquals(false, viewModel.showProgress.value)
        assert(viewModel.loginSuccess.value != true)
        assertEquals(noInternet, message?.message)
    }

    @Test
    fun `login invalid grant error`() = runTest {
        every { validator.isEmailValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns true
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        val viewModel =
            SignInViewModel(interactor, resourceManager, preferencesManager, validator, analytics, appUpgradeNotifier)
        coEvery { interactor.login("acc@test.org", "edx") } throws EdxError.InvalidGrantException()
        viewModel.login("acc@test.org", "edx")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        assertEquals(false, viewModel.showProgress.value)
        assert(viewModel.loginSuccess.value != true)
        assertEquals(invalidCredential, message.message)
    }

    @Test
    fun `login unknown exception`() = runTest {
        every { validator.isEmailValid(any()) } returns true
        every { validator.isPasswordValid(any()) } returns true
        every { preferencesManager.user } returns user
        every { analytics.setUserIdForSession(any()) } returns Unit
        val viewModel =
            SignInViewModel(interactor, resourceManager, preferencesManager, validator, analytics, appUpgradeNotifier)
        coEvery { interactor.login("acc@test.org", "edx") } throws IllegalStateException()
        viewModel.login("acc@test.org", "edx")
        advanceUntilIdle()

        coVerify(exactly = 1) { interactor.login(any(), any()) }
        verify(exactly = 0) { analytics.setUserIdForSession(any()) }
        verify(exactly = 1) { appUpgradeNotifier.notifier }

        val message = viewModel.uiMessage.value as UIMessage.SnackBarMessage
        assertEquals(false, viewModel.showProgress.value)
        assert(viewModel.loginSuccess.value != true)
        assertEquals(somethingWrong, message.message)
    }

}
