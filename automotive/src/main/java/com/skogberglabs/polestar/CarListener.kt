package com.skogberglabs.polestar

import android.car.Car
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import timber.log.Timber

class CarListener(private val context: Context) {
    companion object {
        val permissions =
            listOf(
                Car.PERMISSION_SPEED,
                Car.PERMISSION_ENERGY,
                Car.PERMISSION_EXTERIOR_ENVIRONMENT,
            )
    }

    val car: Car = Car.createCar(context)
    private val carState = MutableStateFlow(CarState.empty)
    val carInfo: StateFlow<CarState> = carState

    private val carPropertyManager = car.getCarManager(Car.PROPERTY_SERVICE) as CarPropertyManager
    private val callback =
        object : CarPropertyManager.CarPropertyEventCallback {
            override fun onChangeEvent(v: CarPropertyValue<*>) {
                if (v.status == CarPropertyValue.STATUS_AVAILABLE) {
//                Timber.i("Property ${v.propertyId} changed to ${v.value}.")
                    carState.update {
                        val updated =
                            when (v.propertyId) {
                                VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE ->
                                    it.copy(outsideTemperature = (v.value as Float).celsius)
                                VehiclePropertyIds.EV_BATTERY_LEVEL ->
                                    it.copy(batteryLevel = (v.value as Float).wattHours)
                                VehiclePropertyIds.PERF_VEHICLE_SPEED ->
                                    it.copy(speed = (v.value as Float).metersPerSecond)
                                VehiclePropertyIds.RANGE_REMAINING ->
                                    it.copy(rangeRemaining = (v.value as Float).meters)
                                VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY ->
                                    it.copy(batteryCapacity = (v.value as Float).wattHours)
                                VehiclePropertyIds.CURRENT_GEAR ->
                                    Gear.find(v.value as Int)?.let { gear ->
                                        it.copy(gear = gear).updateTime()
                                    } ?: it
                                VehiclePropertyIds.NIGHT_MODE ->
                                    it.copy(nightMode = v.value as Boolean)
                                else -> {
                                    Timber.i("Property ${v.propertyId} changed, but ignoring.")
                                    it
                                }
                            }
                        updated.updateTime()
                    }
                } else {
                    Timber.i("Property ${v.propertyId} in status ${v.status}.")
                }
            }

            override fun onErrorEvent(
                propId: Int,
                zone: Int,
            ) {
                Timber.i("Car property error. Property $propId zone $zone.")
            }
        }
    private val vehicleProps =
        listOf(
            VehiclePropertyIds.ENV_OUTSIDE_TEMPERATURE,
            VehiclePropertyIds.EV_BATTERY_LEVEL,
            VehiclePropertyIds.PERF_VEHICLE_SPEED,
            VehiclePropertyIds.RANGE_REMAINING,
            VehiclePropertyIds.NIGHT_MODE,
//        VehiclePropertyIds.CURRENT_GEAR
//        VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY,
//        VehiclePropertyIds.INFO_MAKE,
//        VehiclePropertyIds.INFO_MODEL,
//        VehiclePropertyIds.INFO_MODEL_YEAR,
        )
    private val vehiclePropsUnused =
        listOf(
            VehicleProp(VehiclePropertyIds.HVAC_TEMPERATURE_CURRENT, dataUnit = DataUnit.Celsius),
            VehicleProp(VehiclePropertyIds.PERF_ODOMETER, dataUnit = DataUnit.Kilometers),
            VehicleProp(VehiclePropertyIds.EV_BATTERY_DISPLAY_UNITS, PropertyType.IntProp),
            VehicleProp(VehiclePropertyIds.EV_BATTERY_INSTANTANEOUS_CHARGE_RATE, dataUnit = DataUnit.MilliWatts),
            VehicleProp.bool(VehiclePropertyIds.EV_CHARGE_PORT_CONNECTED),
            VehicleProp.bool(VehiclePropertyIds.EV_CHARGE_PORT_OPEN),
            VehicleProp(VehiclePropertyIds.HVAC_TEMPERATURE_CURRENT, dataUnit = DataUnit.Celsius),
            VehicleProp.bool(VehiclePropertyIds.NIGHT_MODE),
            VehicleProp.bool(VehiclePropertyIds.SEAT_BELT_BUCKLED),
            VehicleProp(VehiclePropertyIds.TIRE_PRESSURE, dataUnit = DataUnit.Kilopascals),
            VehicleProp(VehiclePropertyIds.HVAC_ACTUAL_FAN_SPEED_RPM, dataUnit = DataUnit.Rpm),
            VehicleProp.bool(VehiclePropertyIds.HVAC_AC_ON),
            VehicleProp.bool(VehiclePropertyIds.HVAC_AUTO_ON),
            VehicleProp.bool(VehiclePropertyIds.HVAC_AUTO_RECIRC_ON),
            VehicleProp.bool(VehiclePropertyIds.HVAC_DEFROSTER),
            VehicleProp.bool(VehiclePropertyIds.HVAC_DUAL_ON),
            VehicleProp.bool(VehiclePropertyIds.HVAC_MAX_AC_ON),
            VehicleProp.bool(VehiclePropertyIds.HVAC_MAX_DEFROST_ON),
            VehicleProp.bool(VehiclePropertyIds.HVAC_POWER_ON),
            VehicleProp.bool(VehiclePropertyIds.HVAC_RECIRC_ON),
            VehicleProp.string(VehiclePropertyIds.INFO_VIN),
            //        VehicleProp(VehiclePropertyIds.HVAC_TEMPERATURE_SET),
//        VehicleProp(VehiclePropertyIds.EPOCH_TIME),
//        VehicleProp(VehiclePropertyIds.HVAC_SEAT_TEMPERATURE),
//        VehicleProp(VehiclePropertyIds.HVAC_STEERING_WHEEL_HEAT),
//        VehicleProp(VehiclePropertyIds.SEAT_OCCUPANCY),
//        VehicleProp(VehiclePropertyIds.TURN_SIGNAL_STATE),
//        VehicleProp(VehiclePropertyIds.HVAC_FAN_DIRECTION),
//        VehicleProp(VehiclePropertyIds.HVAC_FAN_SPEED)
        )

    fun connect() {
        val props = carPropertyManager.propertyList
        val message =
            if (props.isEmpty()) {
                "No car props are available."
            } else {
                val str = props.map { it.propertyId }.joinToString(separator = ", ")
                "The following props are available: $str"
            }
        Timber.i(message)
        val registrations =
            vehicleProps.map { prop ->
                carPropertyManager.registerCallback(callback, prop, CarPropertyManager.SENSOR_RATE_ONCHANGE)
            }
    }

    fun disconnect() = car.disconnect()
}
