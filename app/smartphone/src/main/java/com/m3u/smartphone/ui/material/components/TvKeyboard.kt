package com.m3u.smartphone.ui.material.components

import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

private val KEYS = listOf(
    "A","B","C","D","E","F","G",
    "H","I","J","K","L","M","N",
    "O","P","Q","R","S","T","U",
    "V","W","X","Y","Z","1","2",
    "3","4","5","6","7","8","9",
    "0","⌫","␣","⏎"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TvKeyboard(
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    modifier: Modifier = Modifier
) {
    FlowRow(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = 7
    ) {
        KEYS.forEach { label ->
            TvKey(
                label = label,
                onClick = {
                    when (label) {
                        "⌫" -> onBackspace()
                        "␣" -> onSpace()
                        "⏎" -> { /* no-op or submit */ }
                        else -> onChar(label.first().lowercaseChar())
                    }
                }
            )
        }
    }
}

@Composable
private fun TvKey(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Surface(
        color = if (isFocused) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = shape,
        modifier = modifier
            .size(44.dp)
            .then(
                if (isFocused) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                    onClick()
                    true
                } else false
            }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isFocused) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
