package com.travelbuddy.app.data
import com.travelbuddy.app.domain.EmergencyContact
import com.travelbuddy.app.domain.SosRequest
import com.travelbuddy.app.domain.SosResponse
interface SafetyRepository{
 suspend fun getEmergencyContacts():Result<List<EmergencyContact>>
 suspend fun saveEmergencyContacts(contacts:List<EmergencyContact>):Result<List<EmergencyContact>>
 suspend fun sendSos(request:SosRequest):Result<SosResponse>
}
