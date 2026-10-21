package com.travelbuddy.app.domain
data class EmergencyContact(val id:String,val name:String,val relation:String,val phone:String,val notifyBySms:Boolean=true,val notifyByCall:Boolean=true)
data class SosRequest(val tripId:String,val latitude:Double,val longitude:Double,val accuracyMeters:Double?,val timestamp:Long,val message:String)
data class SosResponse(val alertId:String,val status:String,val notifiedContacts:Int,val createdAt:Long)
