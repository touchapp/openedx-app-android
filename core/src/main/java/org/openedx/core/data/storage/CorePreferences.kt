package org.openedx.core.data.storage

import org.openedx.core.data.model.User
import org.openedx.core.domain.model.VideoSettings

interface CorePreferences {
    var accessToken: String
    var refreshToken: String
    var accessTokenExpiresAt: Long
    var user: User?
    var videoSettings: VideoSettings

    fun clear()
}
