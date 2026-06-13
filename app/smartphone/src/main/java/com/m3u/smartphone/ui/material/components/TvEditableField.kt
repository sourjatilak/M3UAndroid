package com.m3u.smartphone.ui.material.components

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val TV_FOCUS_COLOR = Color(0xFFFFD600)

/**
 * A text field designed for TV/D-pad input. Shows the current text as a
 * focusable row; when activated (center-press), reveals an inline grid
 * keyboard. Focus stays on the keyboard keys between presses.
 * On phone/tablet, delegates to [EditableField].
 */
@Composable
fun TvEditableField(
    text: String,
    placeholder: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val isTv = (context.resources.configuration.uiMode and
            Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION

    if (!isTv) {
        EditableField(
            text = text,
            placeholder = placeholder,
            onValueChange = onValueChange,
            modifier = modifier
        )
        return
    }

    var showKeyboard by remember { mutableStateOf(false) }
    var fieldFocused by remember { mutableStateOf(false) }

    BackHandler(showKeyboard) { showKeyboard = false }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = if (fieldFocused || showKeyboard) 3.dp else 1.dp,
                    color = if (fieldFocused || showKeyboard) TV_FOCUS_COLOR
                    else MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .focusable()
                .onFocusChanged {
                    fieldFocused = it.isFocused
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionCenter) {
                        showKeyboard = !showKeyboard
                        true
                    } else false
                }
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Text(
                text = text.ifEmpty { placeholder },
                style = MaterialTheme.typography.bodyLarge,
                color = if (text.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }

        if (showKeyboard) {
            InlineKeyboard(
                onChar = { ch -> onValueChange(text + ch) },
                onBackspace = { if (text.isNotEmpty()) onValueChange(text.dropLast(1)) },
                onSpace = { onValueChange("$text ") },
                onDone = { showKeyboard = false },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private val KEYS = listOf(
    "A","B","C","D","E","F","G",
    "H","I","J","K","L","M","N",
    "O","P","Q","R","S","T","U",
    "V","W","X","Y","Z","1","2",
    "3","4","5","6","7","8","9",
    "0",".",":","/"," ","⌫","✓"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineKeyboard(
    onChar: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSpace: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val firstKeyFocusRequester = remember { FocusRequester() }

    FlowRow(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        maxItemsInEachRow = 7
    ) {
        KEYS.forEachIndexed { index, label ->
            InlineKey(
                label = if (label == " ") "␣" else label,
                focusRequester = if (index == 0) firstKeyFocusRequester else null,
                onClick = {
                    when (label) {
                        "⌫" -> onBackspace()
                        "✓" -> onDone()
                        " " -> onSpace()
                        else -> onChar(label.first().lowercaseChar())
                    }
                }
            )
        }
    }
}

@Composable
private fun InlineKey(
    label: String,
    onClick: () -> Unit,
    focusRequester: FocusRequester? = null,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(6.dp)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(46.dp)
            .clip(shape)
            .background(
                if (isFocused) TV_FOCUS_COLOR
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .then(
                if (isFocused) Modifier.border(3.dp, TV_FOCUS_COLOR, shape)
                else Modifier
            )
            .then(
                if (focusRequester != null) Modifier.focusRequester(focusRequester)
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
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isFocused) Color.Black else MaterialTheme.colorScheme.onSurface
        )
    }
}
