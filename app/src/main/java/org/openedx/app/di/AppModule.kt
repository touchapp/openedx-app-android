package org.openedx.app.di

import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidApplication
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.openedx.app.AnalyticsManager
import org.openedx.app.AppAnalytics
import org.openedx.app.AppRouter
import org.openedx.app.BuildConfig
import org.openedx.app.data.storage.PreferencesManager
import org.openedx.app.room.AppDatabase
import org.openedx.app.room.DATABASE_NAME
import org.openedx.app.system.notifier.AppNotifier
import org.openedx.auth.presentation.AuthAnalytics
import org.openedx.auth.presentation.AuthRouter
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.data.storage.InAppReviewPreferences
import org.openedx.core.module.DownloadWorkerController
import org.openedx.core.module.TranscriptManager
import org.openedx.core.module.download.FileDownloader
import org.openedx.core.presentation.dialog.appreview.AppReviewManager
import org.openedx.core.presentation.global.AppData
import org.openedx.core.presentation.global.WhatsNewGlobalManager
import org.openedx.core.system.AppCookieManager
import org.openedx.core.system.ResourceManager
import org.openedx.core.system.connection.NetworkConnection
import org.openedx.core.system.notifier.CourseNotifier
import org.openedx.course.presentation.CourseAnalytics
import org.openedx.course.presentation.CourseRouter
import org.openedx.dashboard.presentation.DashboardAnalytics
import org.openedx.dashboard.presentation.DashboardRouter
import org.openedx.discovery.presentation.DiscoveryAnalytics
import org.openedx.discovery.presentation.DiscoveryRouter
import org.openedx.discussion.presentation.DiscussionAnalytics
import org.openedx.discussion.presentation.DiscussionRouter
import org.openedx.discussion.system.notifier.DiscussionNotifier
import org.openedx.profile.data.storage.ProfilePreferences
import org.openedx.profile.presentation.ProfileAnalytics
import org.openedx.profile.presentation.ProfileRouter
import org.openedx.profile.system.notifier.ProfileNotifier
import org.openedx.core.presentation.global.app_upgrade.AppUpgradeRouter
import org.openedx.core.system.notifier.AppUpgradeNotifier
import org.openedx.whatsnew.WhatsNewManager
import org.openedx.whatsnew.WhatsNewRouter
import org.openedx.whatsnew.data.storage.WhatsNewPreferences

val appModule = module {

    single { PreferencesManager(get()) }
    single<CorePreferences> { get<PreferencesManager>() }
    single<ProfilePreferences> { get<PreferencesManager>() }
    single<WhatsNewPreferences> { get<PreferencesManager>() }
    single<InAppReviewPreferences> { get<PreferencesManager>() }

    single { ResourceManager(get()) }
    single { AppCookieManager(get()) }
    single { ReviewManagerFactory.create(get()) }

    single<Gson> { GsonBuilder().create() }

    single { AppNotifier() }
    single { CourseNotifier() }
    single { DiscussionNotifier() }
    single { ProfileNotifier() }
    single { AppUpgradeNotifier() }

    single { AppRouter() }
    single<AuthRouter> { get<AppRouter>() }
    single<DiscoveryRouter> { get<AppRouter>() }
    single<DashboardRouter> { get<AppRouter>() }
    single<CourseRouter> { get<AppRouter>() }
    single<DiscussionRouter> { get<AppRouter>() }
    single<ProfileRouter> { get<AppRouter>() }
    single<WhatsNewRouter> { get<AppRouter>() }
    single<AppUpgradeRouter> { get<AppRouter>() }

    single { NetworkConnection(get()) }

    single(named("IODispatcher")) {
        Dispatchers.IO
    }

    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration()
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    single<RoomDatabase> {
        get<AppDatabase>()
    }

    single {
        val room = get<AppDatabase>()
        room.discoveryDao()
    }

    single {
        val room = get<AppDatabase>()
        room.courseDao()
    }

    single {
        val room = get<AppDatabase>()
        room.dashboardDao()
    }

    single {
        val room = get<AppDatabase>()
        room.downloadDao()
    }

    single {
        FileDownloader()
    }

    single {
        DownloadWorkerController(get(), get(), get())
    }

    single { AppData(BuildConfig.VERSION_NAME) }
    factory { (activity: AppCompatActivity) -> AppReviewManager(activity, get(), get()) }

    single { TranscriptManager(get()) }
    single { WhatsNewManager(get(), get(), get()) }
    single<WhatsNewGlobalManager> { get<WhatsNewManager>() }

    single { AnalyticsManager(get()) }
    single<DashboardAnalytics> { get<AnalyticsManager>() }
    single<AuthAnalytics> { get<AnalyticsManager>() }
    single<AppAnalytics> { get<AnalyticsManager>() }
    single<DiscoveryAnalytics> { get<AnalyticsManager>() }
    single<ProfileAnalytics> { get<AnalyticsManager>() }
    single<CourseAnalytics> { get<AnalyticsManager>() }
    single<DiscussionAnalytics> { get<AnalyticsManager>() }
}