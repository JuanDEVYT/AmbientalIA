package com.ambientalia.ai

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// ── Petición ───────────────────────────────────────────────────────────────

@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean = false,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
)

/**
 * `content` puede ser un string (chat normal) o una lista de partes
 * (texto + imagen) tal como exige OpenAI para visión.
 */
@Serializable
data class ChatMessage(
    val role: String,
    val content: JsonElement,
)

@Serializable
data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerialName("image_url") val imageUrl: ImageUrl? = null,
)

@Serializable
data class ImageUrl(val url: String)

// ── Respuesta ──────────────────────────────────────────────────────────────

@Serializable
data class ChatResponse(
    val id: String? = null,
    val model: String? = null,
    val choices: List<Choice> = emptyList(),
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: AssistantMessage? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class AssistantMessage(
    val content: String? = null,
    val role: String? = null,
)

// ── Streaming (SSE) ────────────────────────────────────────────────────────

@Serializable
data class StreamChunk(
    val id: String? = null,
    val choices: List<StreamChoice> = emptyList(),
)

@Serializable
data class StreamChoice(
    val index: Int = 0,
    val delta: Delta? = null,
    @SerialName("finish_reason") val finishReason: String? = null,
)

@Serializable
data class Delta(
    val content: String? = null,
    val role: String? = null,
    @SerialName("reasoning_content") val reasoningContent: String? = null,
)

// ── Modelos ────────────────────────────────────────────────────────────────

@Serializable
data class ModelList(val data: List<ModelItem> = emptyList())

@Serializable
data class ModelItem(
    val id: String,
    @SerialName("owned_by") val ownedBy: String? = null,
)
