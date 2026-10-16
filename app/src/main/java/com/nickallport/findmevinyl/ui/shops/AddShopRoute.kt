package com.nickallport.findmevinyl.ui.shops

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.nickallport.findmevinyl.nominatim.Coordinates
import com.nickallport.findmevinyl.shops.FoundPosition
import com.nickallport.findmevinyl.shops.SharedPlace
import com.nickallport.findmevinyl.shops.SharedPlaceParser
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopAddress
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Adding a shop by hand. [sharedText] is what Google Maps shared into the app, read as soon as the page opens. A short
 * link is followed with [resolveLink] only when the text does not already have the name and address. [findPosition] looks
 * an address up, [currentPosition] is where the phone is (null when it cannot say), and [onSave] keeps the shop.
 */
@Composable
fun AddShopRoute(
    sharedText: String?,
    resolveLink: suspend (String) -> String?,
    findPosition: suspend (String) -> FoundPosition?,
    currentPosition: suspend () -> Coordinates?,
    onSave: suspend (Shop) -> Unit,
    modifier: Modifier = Modifier,
    newId: () -> String = { Shop.CUSTOM_PREFIX + UUID.randomUUID() }
) {
    var form by rememberSaveable(stateSaver = AddShopFormSaver) { mutableStateOf(AddShopForm()) }
    var busy by remember { mutableStateOf(false) }
    var sharedTextRead by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    suspend fun read(text: String) {
        var place = SharedPlaceParser.parse(text)
        var status: String? = null

        val link = place.link
        val needsLookup = place.position == null && (place.name == null || place.address == null)
        if (link != null && needsLookup && SharedPlaceParser.isMapsLink(link)) {
            val followed = resolveLink(link)?.let { SharedPlaceParser.fromPlaceUrl(it) }
            if (followed == null) {
                status = "Couldn't read that link. Type the name and address, or paste a position."
            } else {
                place = SharedPlace(
                    name = place.name ?: followed.name,
                    address = place.address ?: followed.address,
                    link = link,
                    position = place.position ?: followed.position
                )
            }
        }
        if (place.name == null && place.address == null && place.position == null && status == null) {
            status = "Couldn't find a shop in that. Paste what Google Maps shared, a link, or a position."
        }

        form = form.copy(
            name = place.name ?: form.name,
            address = place.address ?: form.address,
            status = status
        )

        val position = place.position
        val address = place.address
        when {
            position != null -> form = form.copy(
                position = position, positionNote = "exact, as pasted", approximate = false, status = status
            )
            address != null -> {
                val found = findPosition(address)
                form = when {
                    found == null -> form.copy(
                        status = "Couldn't find that address on the map. Paste a position, or use your position."
                    )
                    found.approximate -> form.copy(
                        position = found.position, positionNote = "approximate: ${found.place}", approximate = true,
                        status = "Couldn't find the street, so the position is that of ${found.place}, and the map will " +
                            "search for the shop by name. Paste a position, or use your position, for a better one."
                    )
                    else -> form.copy(
                        position = found.position, positionNote = "found from the address", approximate = false,
                        status = "Found the street on the map. The position may be a little off, because OpenStreetMap often has the street " +
                            "and not the house. Paste a position for the exact spot."
                    )
                }
            }
        }
    }

    fun runRead(text: String) {
        scope.launch {
            busy = true
            try {
                read(text)
            } catch (e: CancellationException) {
                throw e
            } finally {
                busy = false
            }
        }
    }

    LaunchedEffect(sharedText) {
        if (!sharedTextRead && !sharedText.isNullOrBlank()) {
            sharedTextRead = true
            form = form.copy(paste = sharedText)
            busy = true
            try {
                read(sharedText)
            } finally {
                busy = false
            }
        }
    }

    AddShopScreen(
        form = form,
        busy = busy,
        onFormChange = { form = it },
        onRead = { runRead(form.paste) },
        onUsePosition = {
            scope.launch {
                busy = true
                try {
                    val here = currentPosition()
                    form = if (here == null) {
                        form.copy(status = "Couldn't get your position. Check that location is turned on and allowed.")
                    } else {
                        form.copy(position = here, positionNote = "your position", approximate = false, status = null)
                    }
                } finally {
                    busy = false
                }
            }
        },
        onSave = {
            val position = form.position ?: return@AddShopScreen
            val name = form.name.trim()
            val address = form.address.trim().takeIf { it.isNotEmpty() }
            scope.launch {
                busy = true
                try {
                    onSave(
                        Shop(
                            id = newId(),
                            name = name,
                            category = form.kind,
                            latitude = position.latitude,
                            longitude = position.longitude,
                            openingHours = form.hours.trim().takeIf { it.isNotEmpty() },
                            address = address?.let { ShopAddress.fromText(it) },
                            phone = form.phone.trim().takeIf { it.isNotEmpty() },
                            website = form.website.trim().takeIf { it.isNotEmpty() },
                            approximatePosition = form.approximate,
                            mapQuery = listOfNotNull(name, address).joinToString(", ")
                        )
                    )
                } finally {
                    busy = false
                }
            }
        },
        modifier = modifier
    )
}
