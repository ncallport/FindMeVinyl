package com.nickallport.findmevinyl.ui.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nickallport.findmevinyl.artist.ArtistSuggestion
import com.nickallport.findmevinyl.artist.ArtistSuggestions
import com.nickallport.findmevinyl.favourites.ArtistGroup
import com.nickallport.findmevinyl.favourites.GroupMember
import com.nickallport.findmevinyl.favourites.GroupResult
import com.nickallport.findmevinyl.ui.brand.Glyphs
import com.nickallport.findmevinyl.ui.favourites.FavouriteArtistUiModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long the typing has to pause before MusicBrainz is asked for artists to add, as on the Artists page. */
internal const val JOIN_SUGGESTION_WAIT_MILLIS = 700L

// The members as one string, which a rotation can keep: artists separated by 0x1E, id and name by 0x1F.
private const val ITEM = '\u001E'
private const val FIELD = '\u001F'

private fun encode(members: List<GroupMember>) = members.joinToString(ITEM.toString()) { "${it.artistId}$FIELD${it.name}" }

private fun decode(text: String): List<GroupMember> =
    if (text.isEmpty()) emptyList() else text.split(ITEM).mapNotNull {
        val parts = it.split(FIELD, limit = 2)
        if (parts.size == 2) GroupMember(parts[0], parts[1]) else null
    }

/**
 * Joining artists into one, under a name of the user's own, or changing a group the artist is already in.
 * It starts with [startArtist] as the only member. Other artists are found by typing: the user's favourites
 * that match at once, and then MusicBrainz's, as on the Artists page. The first member's picture is the one
 * the group uses, so a member can be made first. [save] is given the id of the group being changed, or null
 * for a new one, and says whether it worked. [ungroup] splits a group up. Both [onDone] after they worked.
 */
@Composable
fun JoinArtistsRoute(
    startArtist: GroupMember,
    loadGroup: suspend (artistId: String) -> ArtistGroup?,
    loadFavourites: suspend () -> List<FavouriteArtistUiModel>,
    suggestArtists: suspend (String) -> List<ArtistSuggestion>,
    save: suspend (groupId: String?, name: String, members: List<GroupMember>) -> GroupResult,
    ungroup: suspend (groupId: String) -> Unit,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    var existing by remember { mutableStateOf<ArtistGroup?>(null) }
    var ready by rememberSaveable { mutableStateOf(false) }
    var name by rememberSaveable { mutableStateOf("") }
    var membersText by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var suggestions by remember { mutableStateOf<List<ArtistSuggestion>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        existing = loadGroup(startArtist.artistId)
        if (!ready) {
            name = existing?.name ?: startArtist.name
            membersText = encode(existing?.members ?: listOf(startArtist))
            ready = true
        }
    }

    val members = decode(membersText)

    // Artists to add: the favourites that match at once, MusicBrainz's once typing pauses. Neither lists an
    // artist already chosen, and groups are not offered, since a group cannot be put in a group.
    LaunchedEffect(query, membersText) {
        if (query.isBlank()) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        val chosen = decode(membersText).map { it.artistId }.toSet()
        val local = ArtistSuggestions.favouritesMatching(query, loadFavourites().filter { !it.isGroup && it.id !in chosen })
        suggestions = local
        delay(JOIN_SUGGESTION_WAIT_MILLIS)
        suggestions = ArtistSuggestions.merge(local, suggestArtists(query)).filter { it.id !in chosen }
    }

    Column(modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text(
            text = if (existing != null) "Edit group" else "Join artists",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("join_title")
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name for the group") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("join_name")
        )
        Spacer(Modifier.height(16.dp))

        Text(text = "Artists in the group", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Text(
            text = "The first one's picture is used for the group, unless you choose a photo for the group.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        members.forEachIndexed { index, member ->
            Row(
                modifier = Modifier.fillMaxWidth().testTag("join_member_${member.artistId}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = member.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                if (index > 0) {
                    IconButton(
                        onClick = { membersText = encode(listOf(member) + members.filter { it != member }) },
                        modifier = Modifier.size(40.dp).testTag("join_member_first_${member.artistId}")
                    ) {
                        Icon(Glyphs.ArrowUp, contentDescription = "Make ${member.name} the first", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                TextButton(
                    onClick = { membersText = encode(members.filter { it != member }) },
                    modifier = Modifier.testTag("join_member_remove_${member.artistId}")
                ) { Text("Remove") }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Add another artist") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("join_search")
        )
        suggestions.forEachIndexed { index, suggestion ->
            Column(
                modifier = Modifier
                    .testTag("join_suggestion_$index")
                    .fillMaxWidth()
                    .clickable {
                        membersText = encode(members + GroupMember(suggestion.id, suggestion.name))
                        query = ""
                        suggestions = emptyList()
                        error = null
                    }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
            ) {
                Text(text = suggestion.name, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = suggestion.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        error?.let {
            Spacer(Modifier.height(12.dp))
            Text(text = it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("join_error"))
        }

        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = members.size >= 2,
                onClick = {
                    scope.launch {
                        when (val result = save(existing?.id, name, members)) {
                            is GroupResult.Saved -> onDone()
                            GroupResult.TooFewMembers -> error = "Pick at least two artists"
                            is GroupResult.AlreadyInGroup -> error = "${result.artistName} is already in the group ${result.groupName}"
                        }
                    }
                },
                modifier = Modifier.testTag("join_save")
            ) { Text("Save") }
            OutlinedButton(onClick = onCancel, modifier = Modifier.testTag("join_cancel")) { Text("Cancel") }
        }
        existing?.let { group ->
            Spacer(Modifier.height(12.dp))
            TextButton(
                onClick = { scope.launch { ungroup(group.id); onDone() } },
                modifier = Modifier.testTag("join_ungroup")
            ) { Text("Split this group up") }
        }
    }
}
