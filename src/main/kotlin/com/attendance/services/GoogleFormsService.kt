package com.attendance.services

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.forms.v1.Forms
import com.google.api.services.forms.v1.model.*
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.UserCredentials
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.server.auth.OAuth2ResponseParameters.AccessToken
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

data class FormUrls(
    val formUrl: String,
    val responseUrl: String
)

data class TokenResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String
)

class GoogleFormsService(
    private val clientId: String,
    private val clientSecret: String,
    private val refreshToken: String
) {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()

    private var accessToken: String? = null
    private var expiresAt: Instant = Instant.EPOCH

    private fun getUserCredential(): UserCredentials {
        val now = Instant.now()

        // 토큰 만료 여부 확인
        if (accessToken == null || now.isAfter(expiresAt)) {
            val tokenResponse = fetchAccessToken()
            accessToken = tokenResponse.access_token
            expiresAt = now.plusSeconds(tokenResponse.expires_in.toLong() - 30) // 30초 여유
        }

        return UserCredentials.newBuilder()
            .setClientId(clientId)
            .setClientSecret(clientSecret)
            .setRefreshToken(refreshToken)
            .setAccessToken(AccessToken(accessToken, Date.from(expiresAt)))
            .build()
    }

    private fun fetchAccessToken(): TokenResponse {
        val url = "https://oauth2.googleapis.com/token"
        val formParams = listOf(
            "client_id" to clientId,
            "client_secret" to clientSecret,
            "refresh_token" to refreshToken,
            "grant_type" to "refresh_token"
        ).joinToString("&") { "${it.first}=${URLEncoder.encode(it.second, "UTF-8")}" }

        val request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(formParams))
            .build()

        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) {
            throw RuntimeException("Failed to fetch access token: ${response.body()}")
        }

        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val adapter = moshi.adapter(TokenResponse::class.java)
        val tokenResponse = adapter.fromJson(response.body())
            ?: throw RuntimeException("Failed to parse token response")

        return tokenResponse
    }


    fun createAttendanceForm(title: String, scheduledTime: ZonedDateTime): FormUrls {
        val credential = getUserCredential()
        val forms = Forms.Builder(httpTransport, jsonFactory, HttpCredentialsAdapter(credential))
            .setApplicationName("Attendance Checker")
            .build()
        val formTitle = "$title (${scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))})"
        val form = Form().apply {
            this.info = Info().apply {
                this.title = formTitle
                this.documentTitle = formTitle
            }
        }

        // 폼 생성 요청
        val createdForm = forms.forms().create(form).execute()

        // 질문 추가
        val updateRequest = BatchUpdateFormRequest().apply {
            requests = listOf(
                Request().apply {
                    createItem = CreateItemRequest().apply {
                        item = Item().apply {
                            setTitle("이름")
                            questionItem = QuestionItem().apply {
                                question = Question().apply {
                                    required = true
                                    textQuestion = TextQuestion().apply {
                                        paragraph = false
                                    }
                                }
                            }
                        }
                        location = Location().apply {
                            index = 0
                        }
                    }
                }
            )
        }

        forms.forms().batchUpdate(createdForm.formId, updateRequest).execute()
        
        return FormUrls(
            formUrl = "https://docs.google.com/forms/d/${createdForm.formId}/viewform",
            responseUrl = "https://docs.google.com/forms/d/${createdForm.formId}/edit"
        )
        // https://docs.google.com/forms/d/19v6L9Z_1nzifYyF78l3qNui5A_xzMN6ARAaCBc-kCqg/edit
    }
} 