package com.nickallport.findmevinyl.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FavouriteShopDao {
    /** Keeps the shop, replacing what was kept under the same id. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(shop: FavouriteShopEntity): Long

    /** Keeps the shop only if it is new, for a restore. The result is -1 when it was already there. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNotExists(shop: FavouriteShopEntity): Long

    @Query("DELETE FROM favourite_shops WHERE id = :id")
    suspend fun delete(id: String): Int

    @Query("SELECT * FROM favourite_shops")
    suspend fun getAll(): List<FavouriteShopEntity>

    @Query("SELECT * FROM favourite_shops WHERE id = :id")
    suspend fun get(id: String): FavouriteShopEntity?

    @Query("SELECT id FROM favourite_shops")
    suspend fun getIds(): List<String>
}
