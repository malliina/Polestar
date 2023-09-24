package com.skogberglabs.polestar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.skogberglabs.polestar.CarLang
import com.skogberglabs.polestar.Outcome
import com.skogberglabs.polestar.Paddings
import java.time.format.DateTimeFormatter

@Composable
fun CarStatsView(lang: CarLang, vm: CarViewModelInterface) {
    val currentLocation by vm.locationSource.currentLocation.collectAsStateWithLifecycle(null)
    val uploadMessage by vm.uploadMessage.collectAsStateWithLifecycle(Outcome.Idle)
    val carState by vm.carState.collectAsStateWithLifecycle()
    val slang = lang.stats
    Column(Modifier.padding(Paddings.xxl)) {
        currentLocation?.let { loc ->
            SpacedRow {
                val accuracy = loc.accuracyMeters?.let { " ${slang.accuracy.lowercase()} $it ${slang.meters}" } ?: ""
                ProfileText("GPS ${loc.latitude.formatted(5)}, ${loc.longitude.formatted(5)}$accuracy")
            }
            SpacedRow {
                loc.altitudeMeters?.let { altitude ->
                    ProfileText("${slang.altitude} $altitude ${slang.meters}")
                }
                loc.bearing?.let { bearing ->
                    val accuracyText = loc.bearingAccuracyDegrees?.let { " ${slang.accuracy.lowercase()} $it ${slang.degrees}" } ?: ""
                    ProfileText("${slang.bearing} $bearing$accuracyText")
                }
            }
            SpacedRow {
                ProfileText(loc.date.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
                when (val outcome = uploadMessage) {
                    is Outcome.Success -> ProfileText("'${outcome.result.message}'.")
                    is Outcome.Error -> ProfileText(outcome.e.message ?: "${outcome.e}", color = MaterialTheme.colorScheme.error)
                    Outcome.Idle -> Text("")
                    Outcome.Loading -> Text("")
                }
            }
        }
        if (!carState.isEmpty) {
            SpacedRow {
                carState.batteryLevel?.let { energy ->
                    ProfileText("${slang.batteryLevel} ${energy.describeKWh}")
                }
                carState.batteryCapacity?.let { capacity ->
                    ProfileText("${slang.capacity} ${capacity.describeKWh}")
                }
                carState.rangeRemaining?.let { distance ->
                    ProfileText("${slang.range} ${distance.describeKm}")
                }
            }
            SpacedRow {
                carState.speed?.let { speed ->
                    ProfileText("${slang.speed} ${speed.describeKmh}")
                }
                carState.outsideTemperature?.let { temperature ->
                    ProfileText("${slang.outsideTemperature} ${temperature.describeCelsius}")
                }
                carState.nightMode?.let { nightMode ->
                    ProfileText(if (nightMode) slang.nightMode else slang.dayMode)
                }
            }
        }
    }
}