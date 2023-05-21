package com.skogberglabs.polestar

import androidx.car.app.hardware.common.CarUnit.CarDistanceUnit
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun TitleText(title: String) {
    Text(title, fontSize = 48.sp)
}

@Composable
fun ErrorText(message: String, modifier: Modifier = Modifier) {
    Text(
        message,
        modifier.padding(Paddings.large),
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
