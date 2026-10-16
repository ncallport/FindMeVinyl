package com.nickallport.findmevinyl.shops

import com.nickallport.findmevinyl.holidays.HolidayRegion
import com.nickallport.findmevinyl.storage.FavouriteShopDao
import com.nickallport.findmevinyl.storage.FavouriteShopEntity

/** A shop the user keeps, with the region it was found in. */
data class FavouriteShop(val shop: Shop, val region: HolidayRegion?)

/**
 * The shops the user keeps as favourites. They are saved with what is known about them, so the list and each
 * shop's page work with no search and no network.
 */
class FavouriteShopsViewModel(
    private val dao: FavouriteShopDao,
    private val clock: () -> Long = System::currentTimeMillis
) {
    suspend fun isFavourite(id: String): Boolean = dao.get(id) != null

    /** Keeps [shop], or updates it if it is kept already. */
    suspend fun add(shop: Shop, region: HolidayRegion?) {
        dao.save(FavouriteShop(shop, region).toEntity(clock()))
    }

    suspend fun remove(id: String) {
        dao.delete(id)
    }

    suspend fun favouriteIds(): Set<String> = dao.getIds().toSet()

    /** In order of town, then name, both ignoring case. Shops with no town come last. */
    suspend fun loadAll(): List<FavouriteShop> =
        dao.getAll().map { it.toFavourite() }.sortedWith(
            compareBy<FavouriteShop, String?>(nullsLast()) { it.shop.address?.city?.lowercase() }
                .thenBy { it.shop.name.lowercase() }
        )
}

internal fun FavouriteShop.toEntity(savedAt: Long) = FavouriteShopEntity(
    id = shop.id, name = shop.name, category = shop.category.name, latitude = shop.latitude, longitude = shop.longitude,
    openingHours = shop.openingHours,
    street = shop.address?.street, houseNumber = shop.address?.houseNumber, postcode = shop.address?.postcode,
    city = shop.address?.city, addressCountry = shop.address?.country,
    phone = shop.phone, website = shop.website, email = shop.email, secondHandOnly = shop.secondHandOnly,
    regionCountry = region?.country, regionSubdivision = region?.subdivision, savedAt = savedAt
)

internal fun FavouriteShopEntity.toFavourite(): FavouriteShop {
    val address = ShopAddress(street, houseNumber, postcode, city, addressCountry).takeUnless { it.isEmpty && addressCountry == null }
    return FavouriteShop(
        shop = Shop(
            id = id, name = name,
            category = runCatching { ShopCategory.valueOf(category) }.getOrDefault(ShopCategory.RECORD_SHOP),
            latitude = latitude, longitude = longitude, openingHours = openingHours, address = address,
            phone = phone, website = website, email = email, secondHandOnly = secondHandOnly
        ),
        region = regionCountry?.let { HolidayRegion(it, regionSubdivision) }
    )
}
