package com.nickallport.findmevinyl.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A rounded card for a row in a list: shops, albums and the groups on the want list. Tappable when
 * given [onClick]. The colour is the theme's surface by default, and can be set for a row that
 * stands out, such as an album you own.
 */
@Composable
fun ListCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    container: Color = MaterialTheme.colorScheme.surface,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
    val shape = RoundedCornerShape(16.dp)
    val inner: @Composable () -> Unit = { Column(content = content) }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = cardModifier,
            shape = shape,
            color = container,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            content = inner
        )
    } else {
        Surface(
            modifier = cardModifier,
            shape = shape,
            color = container,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp,
            content = inner
        )
    }
}
