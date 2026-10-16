package com.nickallport.findmevinyl.ui.shops

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.nominatim.Coordinates
import com.nickallport.findmevinyl.shops.FriendlyHours
import com.nickallport.findmevinyl.shops.Shop
import com.nickallport.findmevinyl.shops.ShopCategory
import java.util.Locale

/** What has been typed or found on the page for adding a shop. */
data class AddShopForm(
    val paste: String = "",
    val name: String = "",
    val kind: ShopCategory = ShopCategory.RECORD_SHOP,
    val address: String = "",
    val phone: String = "",
    val website: String = "",
    val hours: String = "",
    val position: Coordinates? = null,
    /** How the position was found, in words: "found from the address", "your position", and so on. */
    val positionNote: String = "",
    /** The position is only the town's. */
    val approximate: Boolean = false,
    val status: String? = null
) {
    val canSave: Boolean get() = name.isNotBlank() && position != null

    companion object {
        /** The form for changing a shop that was added by hand, filled in with what it has. */
        fun from(shop: Shop) = AddShopForm(
            name = shop.name,
            kind = shop.category,
            address = shop.address?.toText().orEmpty(),
            phone = shop.phone.orEmpty(),
            website = shop.website.orEmpty(),
            hours = shop.openingHours.orEmpty(),
            position = Coordinates(shop.latitude, shop.longitude),
            positionNote = if (shop.approximatePosition) "approximate, as saved" else "as saved",
            approximate = shop.approximatePosition
        )
    }
}

/** Keeps the form over a rotation, as a list of strings. */
val AddShopFormSaver: Saver<AddShopForm, List<String>> = Saver(
    save = {
        listOf(
            it.paste, it.name, it.kind.name, it.address, it.phone, it.website, it.hours,
            it.position?.latitude?.toString().orEmpty(), it.position?.longitude?.toString().orEmpty(),
            it.positionNote, if (it.approximate) "1" else "0", it.status.orEmpty()
        )
    },
    restore = { v ->
        val lat = v[7].toDoubleOrNull()
        val lon = v[8].toDoubleOrNull()
        AddShopForm(
            paste = v[0], name = v[1], kind = runCatching { ShopCategory.valueOf(v[2]) }.getOrDefault(ShopCategory.RECORD_SHOP),
            address = v[3], phone = v[4], website = v[5], hours = v[6],
            position = if (lat != null && lon != null) Coordinates(lat, lon) else null,
            positionNote = v[9], approximate = v[10] == "1", status = v[11].ifEmpty { null }
        )
    }
)

/**
 * The page for adding a shop that OpenStreetMap does not have. Text shared from Google Maps, a link or a pair of
 * coordinates is pasted at the top and read ([onRead]), which fills in the name and address and finds a position. The
 * rest can be typed. A shop needs a name and a position. The position comes from the address, from coordinates, or
 * from where the phone is ([onUsePosition]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddShopScreen(
    form: AddShopForm,
    busy: Boolean,
    isEditing: Boolean,
    onFormChange: (AddShopForm) -> Unit,
    onRead: () -> Unit,
    onUsePosition: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (isEditing) "Edit shop" else "Add a shop",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("add_shop_title")
        )
        Text(
            text = "In Google Maps, open the shop, tap Share and choose Find Me Vinyl. Or paste what Maps gives you " +
                "below: the shared text, a link, or a position such as 39.4036, -9.1349.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = form.paste,
            onValueChange = { onFormChange(form.copy(paste = it)) },
            label = { Text("From Google Maps") },
            minLines = 2,
            maxLines = 5,
            modifier = Modifier.fillMaxWidth().testTag("add_shop_paste")
        )
        FilledTonalButton(
            onClick = onRead,
            enabled = !busy && form.paste.isNotBlank(),
            modifier = Modifier.testTag("add_shop_read")
        ) { Text("Read it") }
        form.status?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.testTag("add_shop_status")
            )
        }

        Spacer(Modifier.height(4.dp))
        Field("Name", form.name, "add_shop_name", capitalise = true) { onFormChange(form.copy(name = it)) }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KindChip("Record shop", "add_shop_kind_record", form.kind == ShopCategory.RECORD_SHOP) {
                onFormChange(form.copy(kind = ShopCategory.RECORD_SHOP))
            }
            KindChip("Second-hand shop", "add_shop_kind_second_hand", form.kind == ShopCategory.SECOND_HAND_SHOP) {
                onFormChange(form.copy(kind = ShopCategory.SECOND_HAND_SHOP))
            }
            KindChip("Charity shop", "add_shop_kind_charity", form.kind == ShopCategory.CHARITY_SHOP) {
                onFormChange(form.copy(kind = ShopCategory.CHARITY_SHOP))
            }
        }
        Field("Address", form.address, "add_shop_address", capitalise = true) { onFormChange(form.copy(address = it)) }
        Field("Phone (optional)", form.phone, "add_shop_phone", keyboard = KeyboardType.Phone) { onFormChange(form.copy(phone = it)) }
        Field("Website (optional)", form.website, "add_shop_website", keyboard = KeyboardType.Uri) { onFormChange(form.copy(website = it)) }
        Field("Opening hours (optional), such as Mon-Sat 10-7", form.hours, "add_shop_hours") { onFormChange(form.copy(hours = it)) }
        if (form.hours.isNotBlank()) {
            val understood = FriendlyHours.toOsm(form.hours)
            if (understood == null) {
                Text(
                    text = "I could not read these hours, so they will be shown as written and the shop will not say " +
                        "whether it is open now. Try something like Mon-Sat 10-7; Sun closed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("add_shop_hours_note")
                )
            } else if (understood != form.hours.trim()) {
                Text(
                    text = "Understood as: $understood",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("add_shop_hours_preview")
                )
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(text = "Position", style = MaterialTheme.typography.titleMedium)
        Text(
            text = form.position?.let { positionText(it, form.positionNote) } ?: "No position yet",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.testTag("add_shop_position")
        )
        OutlinedButton(onClick = onUsePosition, enabled = !busy, modifier = Modifier.testTag("add_shop_use_position")) {
            Text("Use my position")
        }

        Spacer(Modifier.height(8.dp))
        Button(onClick = onSave, enabled = !busy && form.canSave, modifier = Modifier.testTag("add_shop_save")) {
            Text(if (isEditing) "Save changes" else "Save as a favourite")
        }
    }
}

private fun positionText(position: Coordinates, note: String): String {
    val numbers = String.format(Locale.US, "%.4f, %.4f", position.latitude, position.longitude)
    return if (note.isBlank()) numbers else "$numbers ($note)"
}

@Composable
private fun Field(
    label: String,
    value: String,
    tag: String,
    keyboard: KeyboardType = KeyboardType.Text,
    capitalise: Boolean = false,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboard,
            capitalization = if (capitalise) KeyboardCapitalization.Words else KeyboardCapitalization.None
        ),
        modifier = Modifier.fillMaxWidth().testTag(tag)
    )
}

@Composable
private fun KindChip(label: String, tag: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, modifier = Modifier.testTag(tag))
}
