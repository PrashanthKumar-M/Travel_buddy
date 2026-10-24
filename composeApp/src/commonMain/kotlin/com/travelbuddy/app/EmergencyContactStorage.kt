package com.travelbuddy.app

import com.travelbuddy.app.domain.EmergencyContact
import kotlinx.serialization.json.*

object EmergencyContactStorage {
    private const val KEY = "tb_emergency_contacts_v2"

    fun loadContacts(): List<EmergencyContact> {
        val raw = PlatformStorage.getString(KEY) ?: return defaultContacts()
        return try {
            val json = Json { ignoreUnknownKeys = true }
            val element = json.parseToJsonElement(raw)
            val array = element.jsonArray
            val list = array.mapNotNull { item ->
                val obj = item.jsonObject
                val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val relation = obj["relation"]?.jsonPrimitive?.contentOrNull ?: "Contact"
                val phone = obj["phone"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val isEnabled = obj["isEnabled"]?.jsonPrimitive?.booleanOrNull ?: true
                val notifyBySms = obj["notifyBySms"]?.jsonPrimitive?.booleanOrNull ?: true
                val notifyByCall = obj["notifyByCall"]?.jsonPrimitive?.booleanOrNull ?: true
                EmergencyContact(
                    id = id,
                    name = name,
                    relation = relation,
                    phone = phone,
                    isEnabled = isEnabled,
                    notifyBySms = notifyBySms,
                    notifyByCall = notifyByCall
                )
            }
            if (list.isNotEmpty()) list else defaultContacts()
        } catch (_: Exception) {
            defaultContacts()
        }
    }

    fun saveContacts(contacts: List<EmergencyContact>) {
        try {
            val array = buildJsonArray {
                contacts.forEach { c ->
                    addJsonObject {
                        put("id", c.id)
                        put("name", c.name)
                        put("relation", c.relation)
                        put("phone", c.phone)
                        put("isEnabled", c.isEnabled)
                        put("notifyBySms", c.notifyBySms)
                        put("notifyByCall", c.notifyByCall)
                    }
                }
            }
            PlatformStorage.setString(KEY, array.toString())
        } catch (_: Exception) {}
    }

    private fun defaultContacts(): List<EmergencyContact> = emptyList()
}
