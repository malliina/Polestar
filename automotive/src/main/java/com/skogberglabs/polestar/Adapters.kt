package com.skogberglabs.polestar

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.Moshi
import com.squareup.moshi.ToJson
import com.squareup.moshi.adapter
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalStdlibApi::class)
object Adapters {
    private val moshi: Moshi = Moshi.Builder()
        .add(PrimitiveAdapter())
        .build()

    val errors: JsonAdapter<Errors> = moshi.adapter()
    val locationUpdates: JsonAdapter<LocationUpdates> = moshi.adapter()
    val message: JsonAdapter<SimpleMessage> = moshi.adapter()
    val userContainer: JsonAdapter<UserContainer> = moshi.adapter()
    val carState: JsonAdapter<CarState> = moshi.adapter()
    val carConf: JsonAdapter<CarConf> = moshi.adapter()
    val tracks: JsonAdapter<Tracks> = moshi.adapter()
}

class PrimitiveAdapter {
    @FromJson
    fun email(s: String): Email = Email(s)

    @ToJson
    fun writeEmail(s: Email): String = s.email

    @FromJson
    fun id(s: String): IdToken = IdToken(s)

    @ToJson
    fun writeId(s: IdToken): String = s.token

    @FromJson
    fun dateTime(s: String): OffsetDateTime = OffsetDateTime.parse(s)

    @ToJson
    fun writeDateTime(s: OffsetDateTime): String = s.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

    @FromJson
    fun power(f: Float): Power = Power(f)

    @ToJson
    fun writePower(p: Power): Float = p.watts

    @FromJson
    fun energy(f: Float): Energy = Energy(f)

    @ToJson
    fun writeEnergy(p: Energy): Float = p.wattHours

    @FromJson
    fun distanceF(f: Float): DistanceF = DistanceF(f)

    @ToJson
    fun writeDistanceF(p: DistanceF): Float = p.meters

    @FromJson
    fun distance(f: Double): Distance = Distance(f)

    @ToJson
    fun writeDistance(p: Distance): Double = p.meters

    @FromJson
    fun temperature(f: Float): Temperature = Temperature(f)

    @ToJson
    fun writeTemperature(p: Temperature): Float = p.celsius

    @FromJson
    fun pressure(f: Float): Pressure = Pressure(f)

    @ToJson
    fun writePressure(p: Pressure): Float = p.pascals

    @FromJson
    fun speed(f: Float): Speed = Speed(f)

    @ToJson
    fun writeSpeed(p: Speed): Float = p.metersPerSecond

    @FromJson
    fun rpm(f: Int): Rpm = Rpm(f)

    @ToJson
    fun writeRpm(p: Rpm): Int = p.rpm

    @FromJson
    fun gear(i: Int): Gear =
        Gear.values().firstOrNull { v -> v.value == i } ?: throw JsonDataException("Invalid gear: '$i'.")

    @ToJson
    fun writeGear(p: Gear): Int = p.value
}

fun <T> JsonAdapter<T>.read(json: String): T {
    return this.fromJson(json)
        ?: throw JsonDataException("Moshi returned null when reading '$json'.")
}
