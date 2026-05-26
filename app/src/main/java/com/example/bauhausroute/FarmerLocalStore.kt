package com.example.bauhausroute

import android.content.Context

enum class UserRole {
    FARMER,
    CLEANER,
    ADMIN
}

data class FarmerProfile(
    val name: String,
    val phone: String,
    val email: String,
    val password: String,
    val address: String,
    val role: UserRole = UserRole.FARMER
)

class FarmerLocalStore(context: Context) {
    private val preferences = context.getSharedPreferences("farmer_local_store", Context.MODE_PRIVATE)

    fun register(profile: FarmerProfile) {
        val normalizedEmail = profile.email.trim().lowercase()
        val normalizedPhone = profile.phone.trim()
        val id = normalizedEmail.ifBlank { normalizedPhone }
        val farmers = preferences.getStringSet(KEY_FARMERS, emptySet()).orEmpty().toMutableSet()

        farmers += id
        preferences.edit()
            .putStringSet(KEY_FARMERS, farmers)
            .putString("$id.name", profile.name.trim())
            .putString("$id.phone", normalizedPhone)
            .putString("$id.email", normalizedEmail)
            .putString("$id.password", profile.password)
            .putString("$id.address", profile.address.trim())
            .putString("$id.role", profile.role.name)
            .apply()
    }

    fun login(account: String, password: String): FarmerProfile? {
        val normalizedAccount = account.trim().lowercase()
        val farmers = preferences.getStringSet(KEY_FARMERS, emptySet()).orEmpty()
        val matchedId = farmers.firstOrNull { id ->
            id == normalizedAccount ||
                preferences.getString("$id.phone", "").orEmpty() == account.trim() ||
                preferences.getString("$id.email", "").orEmpty() == normalizedAccount
        } ?: return null

        val savedPassword = preferences.getString("$matchedId.password", "").orEmpty()
        if (savedPassword != password) return null

        return FarmerProfile(
            name = preferences.getString("$matchedId.name", "").orEmpty(),
            phone = preferences.getString("$matchedId.phone", "").orEmpty(),
            email = preferences.getString("$matchedId.email", "").orEmpty(),
            password = savedPassword,
            address = preferences.getString("$matchedId.address", "").orEmpty(),
            role = preferences.getString("$matchedId.role", UserRole.FARMER.name)
                .toUserRole()
        )
    }

    companion object {
        private const val KEY_FARMERS = "farmers"
    }
}

private fun String?.toUserRole(): UserRole {
    return runCatching {
        UserRole.valueOf(this ?: UserRole.FARMER.name)
    }.getOrDefault(UserRole.FARMER)
}
