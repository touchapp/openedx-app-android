package org.openedx.app.di

import org.openedx.auth.data.repository.AuthRepository
import org.openedx.auth.domain.interactor.AuthInteractor
import org.openedx.auth.presentation.restore.RestorePasswordViewModel
import org.openedx.auth.presentation.signin.SignInViewModel
import org.openedx.auth.presentation.signup.SignUpViewModel
import org.openedx.core.Validator
import org.openedx.profile.domain.model.Account
import org.openedx.core.presentation.dialog.SelectDialogViewModel
import org.openedx.course.data.repository.CourseRepository
import org.openedx.course.domain.interactor.CourseInteractor
import org.openedx.course.presentation.container.CourseContainerViewModel
import org.openedx.course.presentation.detail.CourseDetailsViewModel
import org.openedx.course.presentation.handouts.HandoutsViewModel
import org.openedx.course.presentation.outline.CourseOutlineViewModel
import org.openedx.discovery.presentation.search.CourseSearchViewModel
import org.openedx.course.presentation.section.CourseSectionViewModel
import org.openedx.course.presentation.unit.container.CourseUnitContainerViewModel
import org.openedx.course.presentation.unit.video.VideoUnitViewModel
import org.openedx.course.presentation.unit.video.VideoViewModel
import org.openedx.course.presentation.videos.CourseVideoViewModel
import org.openedx.dashboard.data.repository.DashboardRepository
import org.openedx.dashboard.domain.interactor.DashboardInteractor
import org.openedx.dashboard.presentation.DashboardViewModel
import org.openedx.discovery.data.repository.DiscoveryRepository
import org.openedx.discovery.domain.interactor.DiscoveryInteractor
import org.openedx.discovery.presentation.DiscoveryViewModel
import org.openedx.discussion.data.repository.DiscussionRepository
import org.openedx.discussion.domain.interactor.DiscussionInteractor
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.presentation.comments.DiscussionCommentsViewModel
import org.openedx.discussion.presentation.responses.DiscussionResponsesViewModel
import org.openedx.discussion.presentation.search.DiscussionSearchThreadViewModel
import org.openedx.discussion.presentation.threads.DiscussionAddThreadViewModel
import org.openedx.discussion.presentation.threads.DiscussionThreadsViewModel
import org.openedx.discussion.presentation.topics.DiscussionTopicsViewModel
import org.openedx.app.AppViewModel
import org.openedx.profile.data.repository.ProfileRepository
import org.openedx.profile.domain.interactor.ProfileInteractor
import org.openedx.profile.presentation.delete.DeleteProfileViewModel
import org.openedx.profile.presentation.edit.EditProfileViewModel
import org.openedx.profile.presentation.profile.ProfileViewModel
import org.openedx.profile.presentation.settings.video.VideoQualityViewModel
import org.openedx.profile.presentation.settings.video.VideoSettingsViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.openedx.profile.presentation.anothers_account.AnothersProfileViewModel

val screenModule = module {

    viewModel { AppViewModel(get(), get(), get(), get(named("IODispatcher")), get()) }

    factory { AuthRepository(get(), get()) }
    factory { AuthInteractor(get()) }
    factory { Validator() }
    viewModel { SignInViewModel(get(), get(), get(), get(), get()) }
    viewModel { SignUpViewModel(get(), get(), get(), get()) }
    viewModel { RestorePasswordViewModel(get(), get(), get()) }

    factory { DashboardRepository(get(), get(),get()) }
    factory { DashboardInteractor(get()) }
    viewModel { DashboardViewModel(get(), get(), get(), get(), get()) }

    factory { DiscoveryRepository(get(), get()) }
    factory { DiscoveryInteractor(get()) }
    viewModel { DiscoveryViewModel(get(), get(), get(), get()) }

    factory { ProfileRepository(get(), get(), get(), get()) }
    factory { ProfileInteractor(get()) }
    viewModel { ProfileViewModel(get(), get(), get(), get(named("IODispatcher")), get(), get(), get()) }
    viewModel { (account: Account) -> EditProfileViewModel(get(), get(), get(), get(), account) }
    viewModel { VideoSettingsViewModel(get(), get()) }
    viewModel { VideoQualityViewModel(get(), get()) }
    viewModel { DeleteProfileViewModel(get(), get(), get(), get()) }
    viewModel { (username: String) -> AnothersProfileViewModel(get(), get(), username) }

    single { CourseRepository(get(), get(), get(),get()) }
    factory { CourseInteractor(get()) }
    viewModel { (courseId: String) -> CourseDetailsViewModel(courseId, get(), get(), get(), get(), get()) }
    viewModel { (courseId: String) -> CourseContainerViewModel(courseId, get(), get(), get(), get(), get()) }
    viewModel { (courseId: String) -> CourseOutlineViewModel(courseId, get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (courseId: String) -> CourseSectionViewModel(get(), get(), get(), get(), get(), get(), get(), get(), courseId) }
    viewModel { (courseId: String) -> CourseUnitContainerViewModel(get(), get(), get(), courseId) }
    viewModel { (courseId: String) -> CourseVideoViewModel(courseId, get(), get(), get(), get(), get(), get(), get(), get()) }
    viewModel { (courseId: String) -> VideoViewModel(courseId, get(), get()) }
    viewModel { (courseId: String) -> VideoUnitViewModel(courseId, get(), get(), get(), get()) }
    viewModel { (courseId:String, handoutsType: String) -> HandoutsViewModel(courseId, handoutsType, get()) }
    viewModel { CourseSearchViewModel(get(), get(), get()) }
    viewModel { SelectDialogViewModel(get()) }

    single { DiscussionRepository(get(), get()) }
    factory { DiscussionInteractor(get()) }
    viewModel { (courseId: String) -> DiscussionTopicsViewModel(get(), get(), get(), courseId) }
    viewModel { (courseId: String, topicId: String, threadType: String) ->  DiscussionThreadsViewModel(get(), get(), get(), courseId, topicId, threadType) }
    viewModel { (thread: org.openedx.discussion.domain.model.Thread) -> DiscussionCommentsViewModel(get(), get(), get(), thread) }
    viewModel { (comment: DiscussionComment) -> DiscussionResponsesViewModel(get(), get(), get(), comment) }
    viewModel { (courseId: String) -> DiscussionAddThreadViewModel(get(), get(), get(), courseId) }
    viewModel { (courseId: String) -> DiscussionSearchThreadViewModel(get(), get(), get(), courseId) }
}