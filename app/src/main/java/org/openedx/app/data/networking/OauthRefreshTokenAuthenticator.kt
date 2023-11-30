package org.openedx.app.data.networking

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import org.openedx.app.system.notifier.AppNotifier
import org.openedx.app.system.notifier.LogoutEvent
import org.openedx.auth.data.api.AuthApi
import org.openedx.auth.domain.model.AuthResponse
import org.openedx.core.ApiConstants
import org.openedx.core.ApiConstants.TOKEN_TYPE_JWT
import org.openedx.core.BuildConfig
import org.openedx.core.BuildConfig.ACCESS_TOKEN_TYPE
import org.openedx.core.data.storage.CorePreferences
import org.openedx.core.utils.TimeUtils
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class OauthRefreshTokenAuthenticator(
    private val preferencesManager: CorePreferences,
    private val appNotifier: AppNotifier,
) : Authenticator, Interceptor {

    private val authApi: AuthApi
    private var lastTokenRefreshRequestTime = 0L

    override fun intercept(chain: Interceptor.Chain): Response {
        if (isTokenExpired()) {
            val response = createUnauthorizedResponse(chain)
            val request = authenticate(chain.connection()?.route(), response)

            return request?.let { chain.proceed(it) } ?: chain.proceed(chain.request())
        }
        return chain.proceed(chain.request())
    }

    init {
        val okHttpClient = OkHttpClient.Builder().apply {
            writeTimeout(60, TimeUnit.SECONDS)
            readTimeout(60, TimeUnit.SECONDS)
            if (BuildConfig.DEBUG) {
                addNetworkInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
            }
        }.build()
        authApi = Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()
            .create(AuthApi::class.java)
    }

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        val accessToken = preferencesManager.accessToken
        val refreshToken = preferencesManager.refreshToken

        if (refreshToken.isEmpty()) {
            return null
        }

        val errorCode = getErrorCode(response.peekBody(200).string())
        if (errorCode != null) {
            when (errorCode) {
                TOKEN_EXPIRED_ERROR_MESSAGE, JWT_TOKEN_EXPIRED -> {
                    try {
                        val newAuth = refreshAccessToken(refreshToken)
                        if (newAuth != null) {
                            return response.request.newBuilder()
                                .header(
                                    HEADER_AUTHORIZATION,
                                    ACCESS_TOKEN_TYPE + " " + newAuth.accessToken
                                )
                                .build()
                        } else {
                            val actualToken = preferencesManager.accessToken
                            if (actualToken != accessToken) {
                                return response.request.newBuilder()
                                    .header(
                                        HEADER_AUTHORIZATION,
                                        "$ACCESS_TOKEN_TYPE $actualToken"
                                    )
                                    .build()
                            }
                            return null
                        }
                    } catch (e: Exception) {
                        return null
                    }
                }

                TOKEN_NONEXISTENT_ERROR_MESSAGE, TOKEN_INVALID_GRANT_ERROR_MESSAGE, JWT_INVALID_TOKEN -> {
                    // Retry request with the current access_token if the original access_token used in
                    // request does not match the current access_token. This case can occur when
                    // asynchronous calls are made and are attempting to refresh the access_token where
                    // one call succeeds but the other fails. https://github.com/edx/edx-app-android/pull/834
                    val authHeaders = response.request.headers[HEADER_AUTHORIZATION]
                        ?.split(" ".toRegex())
                    if (authHeaders?.toTypedArray()?.getOrNull(1) != accessToken) {
                        return response.request.newBuilder()
                            .header(
                                HEADER_AUTHORIZATION,
                                "$ACCESS_TOKEN_TYPE $accessToken"
                            ).build()
                    }

                    runBlocking {
                        appNotifier.send(LogoutEvent())
                    }
                }

                DISABLED_USER_ERROR_MESSAGE, JWT_DISABLED_USER_ERROR_MESSAGE -> {
                    runBlocking {
                        appNotifier.send(LogoutEvent())
                    }
                }
            }
        }
        return null
    }

    private fun isTokenExpired(): Boolean {
        val time = TimeUtils.getCurrentTime() + REFRESH_TOKEN_EXPIRY_THRESHOLD
        return time >= preferencesManager.accessTokenExpiresAt
    }

    private fun canRequestTokenRefresh(): Boolean {
        return TimeUtils.getCurrentTime() - lastTokenRefreshRequestTime >
                REFRESH_TOKEN_INTERVAL_MINIMUM
    }

    @Throws(IOException::class)
    private fun refreshAccessToken(refreshToken: String): AuthResponse? {
        var authResponse: AuthResponse? = null
        if (canRequestTokenRefresh()) {
            val response = authApi.refreshAccessToken(
                ApiConstants.TOKEN_TYPE_REFRESH,
                BuildConfig.CLIENT_ID,
                refreshToken,
                ACCESS_TOKEN_TYPE
            ).execute()
            authResponse = response.body()?.mapToDomain()
            if (response.isSuccessful && authResponse != null) {
                val newAccessToken = authResponse.accessToken ?: ""
                val newRefreshToken = authResponse.refreshToken ?: ""
                val newExpireTime = authResponse.getTokenExpiryTime()

                if (newAccessToken.isNotEmpty() && newRefreshToken.isNotEmpty()) {
                    preferencesManager.accessToken = newAccessToken
                    preferencesManager.refreshToken = newRefreshToken
                    preferencesManager.accessTokenExpiresAt = newExpireTime
                    lastTokenRefreshRequestTime = TimeUtils.getCurrentTime()
                }
            } else if (response.code() == 400) {
                //another refresh already in progress
                Thread.sleep(1500)
            }
        }

        return authResponse
    }

    private fun getErrorCode(responseBody: String): String? {
        try {
            val jsonObj = JSONObject(responseBody)
            if (jsonObj.has(FIELD_ERROR_CODE)) {
                return jsonObj.getString(FIELD_ERROR_CODE)
            } else {
                return if (TOKEN_TYPE_JWT.equals(ACCESS_TOKEN_TYPE, ignoreCase = true)) {
                    val errorType =
                        if (jsonObj.has(FIELD_DETAIL)) FIELD_DETAIL else FIELD_DEVELOPER_MESSAGE
                    jsonObj.getString(errorType)
                } else {
                    val errorCode = jsonObj
                        .optJSONObject(FIELD_DEVELOPER_MESSAGE)
                        ?.optString(FIELD_ERROR_CODE, "") ?: ""
                    if (errorCode != "") {
                        errorCode
                    } else {
                        null
                    }
                }
            }
        } catch (ex: JSONException) {
            Log.d("OauthRefreshTokenAuthenticator", "Unable to get error_code from 401 response")
            return null
        }
    }

    /**
     * [createUnauthorizedResponse] creates an unauthorized okhttp response with the initial chain
     * request for [authenticate] method of [OauthRefreshTokenAuthenticator]. The response is
     * specially designed to trigger the 'Token Expired' case of the [authenticate] method so that
     * it can handle the refresh logic of the access token accordingly.
     *
     * @param chain Chain request for authentication
     * @return Custom unauthorized response builder with initial request
     */
    private fun createUnauthorizedResponse(chain: Interceptor.Chain) = Response.Builder()
        .code(401)
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .message("Unauthorized")
        .headers(chain.request().headers)
        .body(getResponseBody())
        .build()

    /**
     * [getResponseBody] generates an error response body based on access token type because both
     * Bearer and JWT have their own sets of errors.
     *
     * @return ResponseBody based on access token type
     */
    private fun getResponseBody(): ResponseBody {
        val tokenType = ACCESS_TOKEN_TYPE
        val jsonObject = if (TOKEN_TYPE_JWT.equals(tokenType, ignoreCase = true)) {
            JSONObject().put("detail", JWT_TOKEN_EXPIRED)
        } else {
            JSONObject().put("error_code", TOKEN_EXPIRED_ERROR_MESSAGE)
        }

        return jsonObject.toString().toResponseBody("application/json".toMediaType())
    }

    companion object {
        private const val HEADER_AUTHORIZATION = "Authorization"

        private const val TOKEN_EXPIRED_ERROR_MESSAGE = "token_expired"
        private const val TOKEN_NONEXISTENT_ERROR_MESSAGE = "token_nonexistent"
        private const val TOKEN_INVALID_GRANT_ERROR_MESSAGE = "invalid_grant"
        private const val DISABLED_USER_ERROR_MESSAGE = "user_is_disabled"
        private const val JWT_TOKEN_EXPIRED = "Token has expired."
        private const val JWT_INVALID_TOKEN = "Invalid token."
        private const val JWT_DISABLED_USER_ERROR_MESSAGE = "User account is disabled."

        private const val FIELD_ERROR_CODE = "error_code"
        private const val FIELD_DETAIL = "detail"
        private const val FIELD_DEVELOPER_MESSAGE = "developer_message"

        /**
         * [REFRESH_TOKEN_EXPIRY_THRESHOLD] behave as a buffer time to be used in the expiry
         * verification method of the access token to ensure that the token doesn't expire during
         * an active session.
         */
        private const val REFRESH_TOKEN_EXPIRY_THRESHOLD = 60 * 1000

        /**
         * [REFRESH_TOKEN_INTERVAL_MINIMUM] behave as a buffer time for refresh token network
         * requests. It prevents multiple calls to refresh network requests in case of an
         * unauthorized access token during async requests.
         */
        private const val REFRESH_TOKEN_INTERVAL_MINIMUM = 60 * 1000
    }
}
