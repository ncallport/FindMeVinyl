package com.nickallport.findmevinyl.shops

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nickallport.findmevinyl.holidays.HolidayRegion
import com.nickallport.findmevinyl.storage.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FavouriteShopsViewModelTest {

    private lateinit var database: AppDatabase
    private lateinit var viewModel: FavouriteShopsViewModel
    private var now = 1_000L

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        viewModel = FavouriteShopsViewModel(database.favouriteShopDao(), clock = { now })
    }

    @After
    fun tearDown() {
        database.close()
    }

    private val louie = Shop(
        id = "node/1", name = "Louie Louie", category = ShopCategory.RECORD_SHOP, latitude = 38.71, longitude = -9.14,
        openingHours = "Mo-Sa 11:00-19:30; Su,PH 15:00-19:30",
        address = ShopAddress("Escadinhas", "3", "1100-225", "Lisboa", "PT"),
        phone = "+351 213 472 232", website = "https://www.louielouie.biz/", email = "a@b.pt", secondHandOnly = true
    )
    private val pt = HolidayRegion("PT", null)

    private fun shop(id: String, name: String, city: String?) =
        louie.copy(id = id, name = name, address = city?.let { ShopAddress(city = it) })

    @Test
    fun `a shop is not a favourite until it is added`() = runBlocking {
        assertFalse(viewModel.isFavourite("node/1"))

        viewModel.add(louie, pt)

        assertTrue(viewModel.isFavourite("node/1"))
        assertFalse(viewModel.isFavourite("node/2"))
    }

    @Test
    fun `a shop comes back as it was added, with its region`() = runBlocking {
        viewModel.add(louie, HolidayRegion("PT", "PT-11"))

        val kept = viewModel.loadAll().single()

        assertEquals(louie, kept.shop)
        assertEquals(HolidayRegion("PT", "PT-11"), kept.region)
    }

    @Test
    fun `a shop added with no region has none`() = runBlocking {
        viewModel.add(louie, null)

        assertEquals(null, viewModel.loadAll().single().region)
    }

    @Test
    fun `a shop with no address, hours or contact details is kept as it is`() = runBlocking {
        val plain = Shop("way/9", "Plain Records", ShopCategory.CHARITY_SHOP, 1.0, 2.0, null)

        viewModel.add(plain, null)

        assertEquals(plain, viewModel.loadAll().single().shop)
    }

    @Test
    fun `removing a shop takes it out and leaves the others`() = runBlocking {
        viewModel.add(louie, pt)
        viewModel.add(louie.copy(id = "node/2", name = "Carbono"), pt)

        viewModel.remove("node/1")

        assertEquals(listOf("Carbono"), viewModel.loadAll().map { it.shop.name })
        assertFalse(viewModel.isFavourite("node/1"))
    }

    @Test
    fun `adding a shop again updates what is kept about it`() = runBlocking {
        viewModel.add(louie, pt)

        viewModel.add(louie.copy(phone = "+351 999 999 999"), pt)

        assertEquals("+351 999 999 999", viewModel.loadAll().single().shop.phone)
        assertEquals(1, viewModel.loadAll().size)
    }

    @Test
    fun `the shops are in order of town and then name, ignoring case, with shops of no town last`() = runBlocking {
        viewModel.add(shop("1", "zebra Records", "Lisboa"), null)
        viewModel.add(shop("2", "Carbono", "Lisboa"), null)
        viewModel.add(shop("3", "Silver Tentacle", "Caldas da Rainha"), null)
        viewModel.add(shop("4", "Nowhere Records", null), null)
        viewModel.add(shop("5", "amoeba", "Lisboa"), null)

        assertEquals(
            listOf("Silver Tentacle", "amoeba", "Carbono", "zebra Records", "Nowhere Records"),
            viewModel.loadAll().map { it.shop.name }
        )
    }

    @Test
    fun `lists the ids of the favourite shops`() = runBlocking {
        viewModel.add(louie, pt)
        viewModel.add(louie.copy(id = "way/7"), pt)

        assertEquals(setOf("node/1", "way/7"), viewModel.favouriteIds())
    }
}
