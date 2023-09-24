package com.skogberglabs.polestar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skogberglabs.polestar.Paddings

@Composable
fun TitleText(title: String) {
    Text(title, fontSize = 48.sp)
}

@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        modifier,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.titleLarge,
        fontSize = 28.sp
    )
}

@Composable
fun ReadableText(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        modifier,
        style = MaterialTheme.typography.titleLarge,
        fontSize = 28.sp
    )
}

@Composable
fun CarProgressBar(modifier: Modifier = Modifier) {
    Row(Modifier.fillMaxWidth().padding(Paddings.xxl).then(modifier), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun CarDivider() {
    Divider(Modifier.padding(vertical = Paddings.large))
}

@Composable
fun SpacedRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        content = content
    )
}

@Composable
fun CarIconButton(onClick: () -> Unit, image: ImageVector, contentDescription: String) {
    IconButton(onClick = onClick, modifier = Modifier.size(80.dp)) {
        Icon(
            imageVector = image,
            contentDescription = contentDescription,
            Modifier.fillMaxSize()
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CarTopAppBar(title: String, navigationIcon: @Composable () -> Unit = {}, actions: @Composable RowScope.() -> Unit = {}) {
    CenterAlignedTopAppBar(
        title = { TitleText(title) },
        modifier = Modifier.padding(Paddings.normal),
        actions = actions,
        navigationIcon = navigationIcon
    )
}
