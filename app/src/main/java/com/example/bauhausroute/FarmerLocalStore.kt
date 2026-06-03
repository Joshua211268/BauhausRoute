package com.example.bauhausroute

enum class UserRole {
    FARMER,
    CLEANER,
    ADMIN
}

data class FarmerProfile(
    val userId: Long = LEGACY_LOCAL_USER_ID,
    val name: String,
    val phone: String,
    val email: String,
    val password: String,
    val address: String,
    val role: UserRole = UserRole.FARMER
)

data class GoogleAccountProfile(
    val subject: String,
    val displayName: String,
    val email: String
)
