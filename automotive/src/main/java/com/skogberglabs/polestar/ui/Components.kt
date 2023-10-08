package com.skogberglabs.polestar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.skogberglabs.polestar.Paddings

@Composable
fun SpacedRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@Composable fun ProfileText(text: String, modifier: Modifier = Modifier, color: Color = Color.Unspecified) =
    Text(
        text,
        modifier.padding(Paddings.xs),
        color = color,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Start
    )

fun Double.formatted(n: Int): String = String.format("%.${n}f", this)
fun Float.formatted(n: Int): String = String.format("%.${n}f", this)
