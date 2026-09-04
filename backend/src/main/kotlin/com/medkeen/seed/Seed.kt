package com.medkeen.seed

import com.medkeen.auth.AuthService
import com.medkeen.db.FirestoreAdapter
import kotlinx.coroutines.runBlocking

object Seed {
    private const val PASS = "Password123!"

    private data class Demo(
        val email: String,
        val role: String,
        val name: String,
        val extra: Map<String, Any?>,
    )

    private val demos = listOf(
        Demo(
            "dr.alice@medkeen.dev", "DOCTOR", "Dr. Alice Mehta",
            mapOf(
                "phone" to "+91 90000 10001", "city" to "Mumbai",
                "specialization" to "Cardiology", "hospitalName" to "City Heart Institute",
                "hospitalAddress" to "Bandra, Mumbai", "licenseNumber" to "MCI-1001",
                "dob" to "1980-05-12", "bloodGroup" to "O+",
            ),
        ),
        Demo(
            "dr.bob@medkeen.dev", "DOCTOR", "Dr. Bob Nair",
            mapOf(
                "phone" to "+91 90000 10002", "city" to "Delhi",
                "specialization" to "General Medicine", "hospitalName" to "Apollo Clinic",
                "hospitalAddress" to "MG Road, Delhi", "licenseNumber" to "MCI-1002",
                "dob" to "1978-09-30", "bloodGroup" to "A+",
            ),
        ),
        Demo(
            "bb.redcross@medkeen.dev", "BLOOD_BANK", "Red Cross Blood Bank",
            mapOf(
                "phone" to "+91 90000 20001", "city" to "Guntur",
                "bankName" to "Red Cross", "bankAddress" to "MG Road, Guntur",
                "bloodBankLicense" to "BB-2001",
            ),
        ),
        Demo(
            "bb.lifeline@medkeen.dev", "BLOOD_BANK", "LifeLine Blood Bank",
            mapOf(
                "phone" to "+91 90000 20002", "city" to "Hyderabad",
                "bankName" to "LifeLine", "bankAddress" to "Banjara Hills, Hyderabad",
                "bloodBankLicense" to "BB-2002",
            ),
        ),
        Demo(
            "bb.care@medkeen.dev", "BLOOD_BANK", "Care Blood Center",
            mapOf(
                "phone" to "+91 90000 20003", "city" to "Bangalore",
                "bankName" to "Care", "bankAddress" to "Indiranagar, Bangalore",
                "bloodBankLicense" to "BB-2003",
            ),
        ),
        Demo(
            "patient@medkeen.dev", "PATIENT", "Test Patient",
            mapOf(
                "phone" to "+91 90000 30001", "city" to "Mumbai",
                "dob" to "1995-02-10", "bloodGroup" to "B+",
            ),
        ),
    )

    fun run() = runBlocking {
        val now = System.currentTimeMillis()
        val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
        for (demo in demos) {
            val existing = FirestoreAdapter.queryRaw("users", listOf("email" to demo.email), limit = 1)
            if (existing.isNotEmpty()) continue

            val uid = FirestoreAdapter.newId()
            val profile = mutableMapOf<String, Any?>(
                "uid" to uid,
                "email" to demo.email,
                "passwordHash" to AuthService.hashPassword(PASS),
                "role" to demo.role,
                "name" to demo.name,
                "createdAt" to now,
                "updatedAt" to now,
                "consents" to mapOf(
                    "dataStorage" to true,
                    "labResults" to true,
                    "bloodDonation" to true,
                ),
            )
            profile.putAll(demo.extra)
            FirestoreAdapter.setRaw("users", uid, profile)

            if (demo.role == "BLOOD_BANK") {
                FirestoreAdapter.setRaw(
                    "blood_inventory", uid,
                    mapOf(
                        "uid" to uid,
                        "bloodGroupUnits" to bloodGroups.associateWith { 0 },
                        "lastUpdated" to now,
                    ),
                )
            }
            println("[Seed] created ${demo.email} ($uid)")
        }
        println("[Seed] demo accounts ensured")
    }
}
