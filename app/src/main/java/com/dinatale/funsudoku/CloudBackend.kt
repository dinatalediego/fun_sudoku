package com.dinatale.funsudoku

import android.content.Intent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class RemoteGameRecord(
    val id: String,
    @SerialName("user_id") val userId: String,
    val difficulty: String,
    @SerialName("elapsed_seconds") val elapsedSeconds: Long,
    val mistakes: Int,
    @SerialName("hints_used") val hintsUsed: Int,
    @SerialName("completed_at_ms") val completedAtMillis: Long
) {
    fun toLocal(): GameRecord = GameRecord(
        id = id,
        difficulty = Difficulty.valueOf(difficulty),
        elapsedSeconds = elapsedSeconds,
        mistakes = mistakes,
        hintsUsed = hintsUsed,
        completedAtMillis = completedAtMillis
    )

    companion object {
        fun fromLocal(record: GameRecord, userId: String) = RemoteGameRecord(
            id = record.id,
            userId = userId,
            difficulty = record.difficulty.name,
            elapsedSeconds = record.elapsedSeconds,
            mistakes = record.mistakes,
            hintsUsed = record.hintsUsed,
            completedAtMillis = record.completedAtMillis
        )
    }
}

object CloudBackend {
    // These are intentionally blank until a dedicated Fun Sudoku Supabase project is created.
    // A Supabase publishable key is safe for a client app; authorization is enforced by RLS.
    private const val SUPABASE_URL = ""
    private const val SUPABASE_PUBLISHABLE_KEY = ""

    val configured: Boolean
        get() = SUPABASE_URL.isNotBlank() && SUPABASE_PUBLISHABLE_KEY.isNotBlank()

    val client: SupabaseClient? by lazy {
        if (!configured) {
            null
        } else {
            createSupabaseClient(
                supabaseUrl = SUPABASE_URL,
                supabaseKey = SUPABASE_PUBLISHABLE_KEY
            ) {
                install(Auth) {
                    scheme = "funsudoku"
                    host = "auth"
                }
                install(Postgrest)
            }
        }
    }

    fun handleDeepLink(intent: Intent) {
        client?.handleDeeplinks(intent)
    }

    suspend fun signInWithGoogle() {
        val supabase = client ?: error("Cloud sync is not configured yet")
        supabase.auth.signInWith(Google)
    }

    suspend fun signOut() {
        client?.auth?.signOut()
    }

    fun currentEmail(): String? = client?.auth?.currentUserOrNull()?.email

    suspend fun sync(localRecords: List<GameRecord>): List<GameRecord> {
        val supabase = client ?: return localRecords
        val user = supabase.auth.currentUserOrNull() ?: return localRecords

        localRecords.forEach { record ->
            supabase.from("game_sessions").upsert(RemoteGameRecord.fromLocal(record, user.id))
        }

        val remoteRecords = supabase.from("game_sessions")
            .select()
            .decodeList<RemoteGameRecord>()
            .mapNotNull { remote -> runCatching { remote.toLocal() }.getOrNull() }

        return (localRecords + remoteRecords)
            .associateBy { it.id }
            .values
            .sortedByDescending { it.completedAtMillis }
    }
}
