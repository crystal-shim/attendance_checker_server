package com.attendance.services

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.forms.v1.Forms
import com.google.api.services.forms.v1.FormsScopes
import com.google.api.services.forms.v1.model.*
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader

class GoogleFormsService {
    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    private val scopes = listOf(FormsScopes.FORMS_BODY)
    
    private val forms by lazy {
        val credentials = getCredentials()
        Forms.Builder(httpTransport, jsonFactory, credentials)
            .setApplicationName("Attendance Checker")
            .build()
    }

    private fun getCredentials(): Credential {
        val credentialsFile = File("credentials.json")
        val clientSecrets = GoogleClientSecrets.load(
            jsonFactory,
            InputStreamReader(FileInputStream(credentialsFile))
        )

        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets, scopes
        )
            .setDataStoreFactory(FileDataStoreFactory(File("tokens")))
            .setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder().setPort(8888).build()
        return AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
    }

    suspend fun createAttendanceForm(scheduleId: Int, title: String): String {
        // 폼 생성
        val form = Form().apply {
            this.info = Info().apply {
                this.title = "출석체크: $title"
                this.documentTitle = "출석체크: $title"
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
        return "https://docs.google.com/forms/d/${createdForm.formId}/viewform"
    }
} 