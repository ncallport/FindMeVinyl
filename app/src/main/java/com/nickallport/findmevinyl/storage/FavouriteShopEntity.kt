package com.nickallport.findmevinyl.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A shop the user keeps as a favourite, with what was known about it when it was saved, so it can be shown
 * with no search and no network. [id] is the OpenStreetMap element (`node/123`). [category] is the name of a
 * `ShopCategory`. The address is kept in parts. [regionCountry] and [regionSubdivision] are the region the
 * shop was found in, for public holidays and for laying out the address. [savedAt] is in epoch milliseconds.
 */
@Entity(tableName = "favourite_shops")
data class FavouriteShopEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val openingHours: String?,
    val street: String?,
    val houseNumber: String?,
    val postcode: String?,
    val city: String?,
    val addressCountry: String?,
    val phone: String?,
    val website: String?,
    val email: String?,
    val secondHandOnly: Boolean,
    val regionCountry: String?,
    val regionSubdivision: String?,
    val savedAt: Long
)
