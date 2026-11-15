package io.github.iamoutofmana.doomscroll.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SourceEntity::class, ArticleEntity::class, RuleEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class DoomscrollDatabase : RoomDatabase() {
    abstract fun sourceDao(): SourceDao

    abstract fun articleDao(): ArticleDao

    abstract fun ruleDao(): RuleDao

    companion object {
        /**
         * v2 (Phase 2): reader-mode cache columns + user filter rules.
         * Additive only — existing articles/sources survive the upgrade.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN readerText TEXT")
                db.execSQL("ALTER TABLE articles ADD COLUMN readerFetchedAt INTEGER")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `rules` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`type` TEXT NOT NULL, " +
                        "`pattern` TEXT NOT NULL, " +
                        "`enabled` INTEGER NOT NULL, " +
                        "`createdAt` INTEGER NOT NULL)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_rules_pattern_type` " +
                        "ON `rules` (`pattern`, `type`)",
                )
            }
        }
    }
}
