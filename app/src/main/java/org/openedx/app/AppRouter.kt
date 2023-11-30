package org.openedx.app

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import org.openedx.auth.presentation.AuthRouter
import org.openedx.auth.presentation.restore.RestorePasswordFragment
import org.openedx.auth.presentation.signin.SignInFragment
import org.openedx.auth.presentation.signup.SignUpFragment
import org.openedx.core.FragmentViewType
import org.openedx.core.domain.model.CoursewareAccess
import org.openedx.core.presentation.course.CourseViewMode
import org.openedx.core.presentation.global.app_upgrade.AppUpgradeRouter
import org.openedx.core.presentation.global.app_upgrade.UpgradeRequiredFragment
import org.openedx.course.presentation.CourseRouter
import org.openedx.course.presentation.container.CourseContainerFragment
import org.openedx.course.presentation.container.NoAccessCourseContainerFragment
import org.openedx.course.presentation.detail.CourseDetailsFragment
import org.openedx.course.presentation.handouts.HandoutsType
import org.openedx.course.presentation.handouts.WebViewFragment
import org.openedx.course.presentation.section.CourseSectionFragment
import org.openedx.course.presentation.unit.container.CourseUnitContainerFragment
import org.openedx.course.presentation.unit.video.VideoFullScreenFragment
import org.openedx.course.presentation.unit.video.YoutubeVideoFullScreenFragment
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.discovery.presentation.search.CourseSearchFragment
import org.openedx.discussion.domain.model.DiscussionComment
import org.openedx.discussion.domain.model.Thread
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.presentation.comments.DiscussionCommentsFragment
import org.openedx.discussion.presentation.responses.DiscussionResponsesFragment
import org.openedx.discussion.presentation.search.DiscussionSearchThreadFragment
import org.openedx.discussion.presentation.threads.DiscussionAddThreadFragment
import org.openedx.discussion.presentation.threads.DiscussionThreadsFragment
import org.openedx.profile.domain.model.Account
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.presentation.anothers_account.AnothersProfileFragment
import org.openedx.profile.presentation.delete.DeleteProfileFragment
import org.openedx.profile.presentation.edit.EditProfileFragment
import org.openedx.profile.presentation.profile.ProfileFragment
import org.openedx.profile.presentation.settings.video.VideoQualityFragment
import org.openedx.profile.presentation.settings.video.VideoSettingsFragment
import org.openedx.whatsnew.WhatsNewRouter
import org.openedx.whatsnew.presentation.whatsnew.WhatsNewFragment
import java.util.Date

class AppRouter : AuthRouter, DiscoveryRouter, DashboardRouter, CourseRouter, DiscussionRouter,
    ProfileRouter, AppUpgradeRouter, WhatsNewRouter {

    //region AuthRouter
    override fun navigateToMain(fm: FragmentManager) {
        fm.popBackStack()
        fm.beginTransaction()
            .replace(R.id.container, MainFragment())
            .commit()
    }

    override fun navigateToSignUp(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, SignUpFragment())
    }

    override fun navigateToRestorePassword(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, RestorePasswordFragment())
    }

    override fun navigateToWhatsNew(fm: FragmentManager) {
        fm.popBackStack()
        fm.beginTransaction()
            .replace(R.id.container, WhatsNewFragment())
            .commit()
    }
    //endregion

    //region DiscoveryRouter
    override fun navigateToCourseDetail(fm: FragmentManager, courseId: String) {
        replaceFragmentWithBackStack(fm, CourseDetailsFragment.newInstance(courseId))
    }

    override fun navigateToCourseSearch(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, CourseSearchFragment())
    }

    override fun navigateToUpgradeRequired(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, UpgradeRequiredFragment())
    }
    //endregion

    //region DashboardRouter

    override fun navigateToCourseOutline(
        fm: FragmentManager,
        courseId: String,
        courseTitle: String
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseContainerFragment.newInstance(courseId, courseTitle)
        )
    }

    override fun navigateToNoAccess(
        fm: FragmentManager,
        title: String,
        coursewareAccess: CoursewareAccess,
        auditAccessExpires: Date?
    ) {
        replaceFragment(fm, NoAccessCourseContainerFragment.newInstance(title,coursewareAccess, auditAccessExpires))
    }
    //endregion

    //region CourseRouter

    override fun navigateToCourseSubsections(
        fm: FragmentManager,
        courseId: String,
        blockId: String,
        mode: CourseViewMode,
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseSectionFragment.newInstance(courseId, blockId, mode)
        )
    }

    override fun navigateToCourseContainer(
        fm: FragmentManager,
        blockId: String,
        courseId: String,
        courseName: String,
        mode: CourseViewMode
    ) {
        replaceFragmentWithBackStack(
            fm,
            CourseUnitContainerFragment.newInstance(blockId, courseId, courseName, mode)
        )
    }

    override fun replaceCourseContainer(
        fm: FragmentManager,
        blockId: String,
        courseId: String,
        courseName: String,
        mode: CourseViewMode
    ) {
        replaceFragment(
            fm,
            CourseUnitContainerFragment.newInstance(blockId, courseId, courseName, mode),
            FragmentTransaction.TRANSIT_FRAGMENT_FADE
        )
    }

    override fun navigateToFullScreenVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String,
        isPlaying: Boolean
    ) {
        replaceFragmentWithBackStack(
            fm,
            VideoFullScreenFragment.newInstance(videoUrl, videoTime, blockId, courseId, isPlaying)
        )
    }

    override fun navigateToFullScreenYoutubeVideo(
        fm: FragmentManager,
        videoUrl: String,
        videoTime: Long,
        blockId: String,
        courseId: String,
        isPlaying: Boolean
    ) {
        replaceFragmentWithBackStack(
            fm,
            YoutubeVideoFullScreenFragment.newInstance(videoUrl, videoTime, blockId, courseId, isPlaying)
        )
    }

    override fun navigateToHandoutsWebView(
        fm: FragmentManager,
        courseId: String,
        title: String,
        type: HandoutsType
    ) {
        replaceFragmentWithBackStack(
            fm,
            WebViewFragment.newInstance(title, type.name, courseId)
        )
    }
    //endregion

    //region DiscussionRouter
    override fun navigateToDiscussionThread(
        fm: FragmentManager,
        action: String,
        courseId: String,
        topicId: String,
        title: String,
        viewType: FragmentViewType
    ) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionThreadsFragment.newInstance(action, courseId, topicId, title, viewType.name)
        )
    }

    override fun navigateToDiscussionComments(fm: FragmentManager, thread: Thread) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionCommentsFragment.newInstance(thread)
        )
    }

    override fun navigateToDiscussionResponses(
        fm: FragmentManager,
        comment: DiscussionComment,
        isClosed: Boolean
    ) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionResponsesFragment.newInstance(comment, isClosed)
        )
    }

    override fun navigateToAddThread(
        fm: FragmentManager,
        topicId: String,
        courseId: String,
    ) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionAddThreadFragment.newInstance(topicId, courseId)
        )
    }

    override fun navigateToSearchThread(fm: FragmentManager, courseId: String) {
        replaceFragmentWithBackStack(
            fm,
            DiscussionSearchThreadFragment.newInstance(courseId)
        )
    }

    override fun navigateToAnothersProfile(
        fm: FragmentManager,
        username: String
    ) {
        replaceFragmentWithBackStack(
            fm,
            AnothersProfileFragment.newInstance(username)
        )
    }
    //endregion

    //region ProfileRouter
    override fun navigateToEditProfile(fm: FragmentManager, account: Account) {
        replaceFragmentWithBackStack(fm, EditProfileFragment.newInstance(account))
    }

    override fun navigateToVideoSettings(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, VideoSettingsFragment())
    }

    override fun navigateToVideoQuality(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, VideoQualityFragment())
    }

    override fun navigateToDeleteAccount(fm: FragmentManager) {
        replaceFragmentWithBackStack(fm, DeleteProfileFragment())
    }

    override fun restartApp(fm: FragmentManager) {
        fm.apply {
            for (fragment in fragments) {
                beginTransaction().remove(fragment).commit()
            }
            popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            beginTransaction().replace(R.id.container, SignInFragment())
                .commit()
        }
    }
    //endregion

    private fun replaceFragmentWithBackStack(fm: FragmentManager, fragment: Fragment) {
        fm.beginTransaction()
            .replace(R.id.container, fragment, fragment.javaClass.simpleName)
            .addToBackStack(fragment.javaClass.simpleName)
            .commit()
    }

    private fun replaceFragment(fm: FragmentManager, fragment: Fragment, transaction: Int = FragmentTransaction.TRANSIT_NONE) {
        fm.beginTransaction()
            .setTransition(transaction)
            .replace(R.id.container, fragment, fragment.javaClass.simpleName)
            .commit()
    }

    //App upgrade
    override fun navigateToUserProfile(fm: FragmentManager) {
        fm.popBackStack()
        fm.beginTransaction()
            .replace(R.id.container, ProfileFragment())
            .commit()
    }
}