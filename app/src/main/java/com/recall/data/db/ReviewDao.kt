package com.recall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review_state WHERE cardId = :cardId")
    suspend fun state(cardId: Long): ReviewState?

    @Upsert
    suspend fun upsertState(state: ReviewState)

    @Insert
    suspend fun insertLog(log: ReviewLog): Long

    @Query("SELECT s.* FROM review_state s JOIN cards c ON c.id = s.cardId WHERE c.deckId = :deckId")
    suspend fun statesByDeck(deckId: Long): List<ReviewState>

    @Query("SELECT * FROM review_logs WHERE reviewedAt >= :fromMillis ORDER BY reviewedAt")
    suspend fun logsSince(fromMillis: Long): List<ReviewLog>

    @Query("SELECT * FROM review_logs WHERE reviewedAt >= :fromMillis ORDER BY reviewedAt")
    fun observeLogsSince(fromMillis: Long): Flow<List<ReviewLog>>

    // Ties on reviewedAt fall back to the later insert.
    @Query(
        """
        SELECT * FROM review_logs AS l
        WHERE l.deckId = :deckId AND l.id = (
            SELECT n.id FROM review_logs AS n WHERE n.cardId = l.cardId
            ORDER BY n.reviewedAt DESC, n.id DESC LIMIT 1
        )
        """,
    )
    suspend fun latestLogPerCard(deckId: Long): List<ReviewLog>

    @Query("SELECT cardId, MIN(reviewedAt) AS firstAt FROM review_logs WHERE deckId = :deckId GROUP BY cardId")
    suspend fun firstLogTimes(deckId: Long): List<CardFirstLog>
}
