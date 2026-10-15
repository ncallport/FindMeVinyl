package com.nickallport.findmevinyl.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickallport.findmevinyl.ui.brand.FindMeVinylLogo
import com.nickallport.findmevinyl.ui.brand.Glyphs

/** The front screen: the logo, then three big buttons. There is no menu, Back does the rest. */
@Composable
fun HomeScreen(
    onArtists: () -> Unit,
    onWants: () -> Unit,
    onShops: () -> Unit,
    modifier: Modifier = Modifier,
    onSettings: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FindMeVinylLogo(modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(modifier = Modifier.height(24.dp))
        HomeButton("ARTISTS", Glyphs.Person, colors.primary, colors.onPrimary, "home_artists", onArtists)
        Spacer(modifier = Modifier.height(16.dp))
        HomeButton("WANTS", Glyphs.Heart, colors.secondary, colors.onSecondary, "home_wants", onWants)
        Spacer(modifier = Modifier.height(16.dp))
        HomeButton("SHOPS", Glyphs.Pin, colors.tertiary, colors.onTertiary, "home_shops", onShops)
        if (onSettings != null) {
            Spacer(modifier = Modifier.height(24.dp))
            // Quieter than the three that matter every day.
            OutlinedButton(onClick = onSettings, modifier = Modifier.testTag("home_settings")) {
                Text("Settings")
            }
        }
    }
}

@Composable
private fun HomeButton(
    label: String,
    icon: ImageVector,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    tag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content),
        modifier = Modifier.fillMaxWidth().height(72.dp).testTag(tag)
    ) {
        Icon(icon, contentDescription = null)
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
    }
}
