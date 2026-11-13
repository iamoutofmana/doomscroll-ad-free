package io.github.iamoutofmana.doomscroll.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SourceEntity::class, ArticleEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class DoomscrollDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao

    abstract fun articleDao(): ArticleDao
}
