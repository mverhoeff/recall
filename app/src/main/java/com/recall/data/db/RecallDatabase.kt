package com.recall.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Deck::class, Card::class, ReviewState::class, ReviewLog::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class RecallDatabase : RoomDatabase() {
    abstract fun deckDao(): DeckDao
    abstract fun cardDao(): CardDao
    abstract fun reviewDao(): ReviewDao

    companion object {
        @Volatile
        private var instance: RecallDatabase? = null

        fun create(context: Context): RecallDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, RecallDatabase::class.java, "recall.db")
                .build()
                .also { instance = it }
        }
    }
}
