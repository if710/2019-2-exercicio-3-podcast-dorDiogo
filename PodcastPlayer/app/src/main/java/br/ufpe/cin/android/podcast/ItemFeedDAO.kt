package br.ufpe.cin.android.podcast

import androidx.room.*

@Dao
interface ItemFeedDAO {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertItemFeeds(vararg itemFeeds: ItemFeed)

    @Update
    fun updateItemFeeds(vararg itemFeeds : ItemFeed)

    @Query("SELECT * FROM item_feeds")
    fun getAllItemFeeds(): List<ItemFeed>

    @Query("SELECT * FROM item_feeds WHERE downloadLink LIKE :q")
    fun getItemFeed(q: String): ItemFeed
}