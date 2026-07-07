package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val email: String,
    val name: String,
    val phone: String,
    val role: String, // "CLIENT", "CAPSTER", "ADMIN"
    val avatarUrl: String = "",
    val membershipTier: String = "Bronze",
    val queueNumber: String? = null
) : Serializable

@Entity(tableName = "capsters")
data class CapsterEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val specialties: String,
    val experience: String,
    val rating: Float,
    val reviewsCount: Int,
    val status: String, // "Available", "Busy", "Off"
    val supportsHomeService: Boolean,
    val imageUrl: String = ""
) : Serializable

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val category: String, // "Hair Pomade", "Hair Styling", "Hair Care", "Shaving"
    val price: Double,
    val description: String,
    val stock: Int,
    val rating: Float,
    val imageUrl: String = ""
) : Serializable

@Entity(tableName = "reservations")
data class ReservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userEmail: String,
    val userName: String,
    val userPhone: String,
    val capsterId: Int,
    val capsterName: String,
    val serviceName: String,
    val servicePrice: Double,
    val serviceType: String, // "STORE" (In-Store), "HOME" (Home Service)
    val homeAddress: String? = null,
    val date: String,
    val time: String,
    val status: String, // "PENDING", "APPROVED", "ON_THE_WAY", "IN_PROGRESS", "COMPLETED", "REJECTED"
    val queueNo: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "waterfall_stages")
data class WaterfallStageEntity(
    @PrimaryKey val stageId: Int,
    val name: String,
    val description: String,
    val percentage: Int,
    val status: String // "COMPLETED", "IN_PROGRESS", "PENDING"
) : Serializable
