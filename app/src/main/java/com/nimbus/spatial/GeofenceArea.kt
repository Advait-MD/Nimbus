package com.nimbus.spatial

data class GeofenceArea(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radius: Float = 0f,
    val isEnabled: Boolean = true,
    val ownerId: String = "" // ✅ Add this if not already present
)
