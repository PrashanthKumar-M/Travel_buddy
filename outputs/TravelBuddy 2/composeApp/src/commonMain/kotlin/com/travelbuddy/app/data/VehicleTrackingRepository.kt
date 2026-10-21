package com.travelbuddy.app.data
import com.travelbuddy.app.domain.Vehicle
import com.travelbuddy.app.domain.VehicleLocation
interface VehicleTrackingRepository{
 suspend fun getVehicle(vehicleId:String):Result<Vehicle>
 suspend fun getVehicleLocation(vehicleId:String):Result<VehicleLocation>
 suspend fun getTripVehicles(tripId:String):Result<List<Vehicle>>
}
