package com.recall.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
abstract class CardDao {
    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY position")
    abstract fun observeByDeck(deckId: Long): Flow<List<Card>>

    @Query("SELECT * FROM cards WHERE deckId = :deckId ORDER BY position")
    abstract suspend fun byDeck(deckId: Long): List<Card>

    @Query("SELECT * FROM cards WHERE id IN (:ids)")
    abstract suspend fun byIds(ids: List<Long>): List<Card>

    @Insert
    abstract suspend fun insert(card: Card): Long

    @Insert
    abstract suspend fun insertAll(cards: List<Card>): List<Long>

    @Update
    abstract suspend fun update(card: Card)

    @Query("DELETE FROM cards WHERE id = :id")
    abstract suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM cards WHERE deckId = :deckId")
    abstract fun countByDeck(deckId: Long): Flow<Int>

    @Query("SELECT MAX(position) FROM cards WHERE deckId = :deckId")
    abstract suspend fun maxPosition(deckId: Long): Int?

    @Insert
    protected abstract suspend fun insertState(state: ReviewState)

    /** Every card needs a review state, so the two are only ever written together. */
    @Transaction
    open suspend fun insertWithState(card: Card): Long {
        val id = insert(card)
        insertState(ReviewState.new(id))
        return id
    }
}
