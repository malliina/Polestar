package com.skogberglabs.polestar

import com.skogberglabs.polestar.ui.formatted
import com.squareup.moshi.JsonClass
import java.time.OffsetDateTime

data class Power(val watts: Float)
data class Energy(val wattHours: Float) {
    private val kWhRounded get() = (wattHours / 1000).formatted(2)
    val describeKWh: String get() = "$kWhRounded kWh"
}
data class Distance(val meters: Float) {
    private val kmRounded get() = (meters / 1000).formatted(2)
    val describeKm = "$kmRounded km"
}
data class Temperature(val celsius: Float) {
    private val rounded get() = celsius.formatted(2)
    val describeCelsius = "$rounded °C"
}
data class Pressure(val pascals: Float)
data class Speed(val metersPerSecond: Float) {
    private val kmhRounded = (metersPerSecond * 3.6).formatted(2)
    val describeKmh = "$kmhRounded km/h"
}
data class Rpm(val rpm: Int)
enum class Gear(val value: Int) {
    Drive(8), Neutral(1), Park(4), Reverse(2);

    companion object {
        fun find(i: Int): Gear? = values().firstOrNull { it.value == i }
    }
}
val Float.celsius get() = Temperature(this)
val Float.wattHours get() = Energy(this)
val Float.watts get() = Power(this)
val Float.meters get() = Distance(this)
val Float.kilometers get() = Distance(this * 1000)
val Float.pascals get() = Pressure(this)
val Float.kilopascals get() = Pressure(this * 1000)
val Float.metersPerSecond get() = Speed(this)

@JsonClass(generateAdapter = true)
data class CarState(
    val outsideTemperature: Temperature?,
    val batteryLevel: Energy?,
    val batteryCapacity: Energy?,
    val speed: Speed?,
    val rangeRemaining: Distance?,
    val gear: Gear?,
    val nightMode: Boolean?,
    val updated: OffsetDateTime?
) {
    companion object {
        val empty = CarState(null, null, null, null, null, null, null, null)
    }
    val isEmpty: Boolean get() = this == empty
    fun updateTime(): CarState = copy(updated = OffsetDateTime.now())
}

enum class PropertyType {
    FloatProp, IntProp, BoolProp, StringProp
}
enum class DataUnit {
    WattHours, MilliWatts, Celsius, Kilometers, Kilopascals, Meters, MetersPerSecond, Rpm, Other
}
data class VehicleProp(val id: Int, val propertyType: PropertyType = PropertyType.FloatProp, val dataUnit: DataUnit = DataUnit.Other) {
    companion object {
        fun bool(id: Int) = VehicleProp(id, PropertyType.BoolProp)
        fun string(id: Int) = VehicleProp(id, PropertyType.StringProp)
    }
}
