package com.attendance.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NotionPage(
    val parent: Parent,
    val properties: Properties
)

@JsonClass(generateAdapter = true)
data class Parent(
    @Json(name = "database_id") val databaseId: String
)

@JsonClass(generateAdapter = true)
data class Properties(
    val title: TitleProperty,
    val formUrl: UrlProperty
)

@JsonClass(generateAdapter = true)
data class TitleProperty(
    val type: String = "title",
    val title: List<TitleContent>
)

@JsonClass(generateAdapter = true)
data class TitleContent(
    val text: Text
)

@JsonClass(generateAdapter = true)
data class Text(
    val content: String
)

@JsonClass(generateAdapter = true)
data class UrlProperty(
    val url: String?
) 