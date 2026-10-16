package com.nickallport.findmevinyl.storage

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavouriteShopDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: FavouriteShopDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.favouriteShopDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun shop(id: String, name: String = "Louie Louie", city: String? = "Lisboa") = FavouriteShopEntity(
        id = id, name = name, category = "RECORD_SHOP", latitude = 38.71, longitude = -9.14,
        openingHours = "Mo-Sa 11:00-19:30", street = "Escadinhas", houseNumber = "3", postcode = "1100-225",
        city = city, addressCountry = "PT", phone = "+351 213 472 232", website = "https://www.louielouie.biz/",
        email = "a@b.pt", secondHandOnly = true, regionCountry = "PT", regionSubdivision = null, savedAt = 5
    )

    @Test
    fun `a saved shop can be read back, every part of it`() = runBlocking {
        dao.save(shop("node/1"))

        assertEquals(shop("node/1"), dao.get("node/1"))
    }

    @Test
    fun `a shop that was never saved is not there`() = runBlocking {
        assertNull(dao.get("node/9"))
    }

    @Test
    fun `saving a shop again replaces what was kept`() = runBlocking {
        dao.save(shop("node/1", name = "Old name"))

        dao.save(shop("node/1", name = "New name"))

        assertEquals("New name", dao.get("node/1")?.name)
        assertEquals(1, dao.getAll().size)
    }

    @Test
    fun `a shop can be removed, and only that one`() = runBlocking {
        dao.save(shop("node/1")); dao.save(shop("node/2"))

        assertEquals(1, dao.delete("node/1"))

        assertEquals(listOf("node/2"), dao.getAll().map { it.id })
        assertEquals(0, dao.delete("node/1"))
    }

    @Test
    fun `lists the ids of the saved shops`() = runBlocking {
        dao.save(shop("node/1")); dao.save(shop("way/2"))

        assertEquals(setOf("node/1", "way/2"), dao.getIds().toSet())
    }

    @Test
    fun `saving only if it is new leaves a shop that is already kept as it was`() = runBlocking {
        dao.save(shop("node/1", name = "Mine"))

        assertEquals(-1L, dao.insertIfNotExists(shop("node/1", name = "Theirs")))
        assertEquals("Mine", dao.get("node/1")?.name)
        assertEquals(true, dao.insertIfNotExists(shop("node/2")) > 0)
    }

    @Test
    fun `a shop that was added by hand keeps its approximate position and its map search text`() = runBlocking {
        val added = shop("custom/abc").copy(approximatePosition = true, mapQuery = "Silver Tentacle, Caldas da Rainha")

        dao.save(added)

        assertEquals(added, dao.get("custom/abc"))
    }

    @Test
    fun `a shop is exact, with no map search text, unless it says otherwise`() = runBlocking {
        dao.save(shop("node/1"))

        assertEquals(false, dao.get("node/1")?.approximatePosition)
        assertNull(dao.get("node/1")?.mapQuery)
    }
}
