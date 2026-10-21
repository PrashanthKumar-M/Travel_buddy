package com.travelbuddy.app.domain
data class Vehicle(val id:String,val name:String,val registrationNumber:String,val imei:String,val isOnline:Boolean,val latitude:Double,val longitude:Double,val speedKmh:Double,val heading:Double,val ignitionOn:Boolean,val batteryPercent:Int,val lastUpdated:Long)
data class VehicleLocation(val vehicleId:String,val latitude:Double,val longitude:Double,val speedKmh:Double,val heading:Double,val ignitionOn:Boolean,val batteryPercent:Int,val timestamp:Long)
