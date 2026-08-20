package com.medvault.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.medvault.data.model.DpdpConsents
import com.medvault.data.model.User
import com.medvault.data.model.UserProfile
import com.medvault.data.model.UserRole
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser get() = auth.currentUser
    val isLoggedIn get() = currentUser != null

    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("User is null")

            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()

            val user = if (userDoc.exists()) {
                User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    role = userDoc.getString("role")?.let { UserRole.valueOf(it) } ?: UserRole.PATIENT,
                    isOnboarded = userDoc.getBoolean("isOnboarded") ?: true,
                    dpdpConsents = DpdpConsents(
                        storeRecords = userDoc.getBoolean("dpdpConsents.storeRecords") ?: false,
                        shareWithDoctor = userDoc.getBoolean("dpdpConsents.shareWithDoctor") ?: false,
                        aiProcessing = userDoc.getBoolean("dpdpConsents.aiProcessing") ?: false,
                        bloodNetwork = userDoc.getBoolean("dpdpConsents.bloodNetwork") ?: false,
                        emergencyContact = userDoc.getBoolean("dpdpConsents.emergencyContact") ?: false
                    )
                )
            } else {
                val newUser = User(
                    uid = firebaseUser.uid,
                    email = firebaseUser.email ?: "",
                    displayName = firebaseUser.displayName ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    role = UserRole.PATIENT,
                    isOnboarded = true
                )
                firestore.collection("users").document(firebaseUser.uid).set(
                    mapOf(
                        "email" to newUser.email,
                        "displayName" to newUser.displayName,
                        "photoUrl" to newUser.photoUrl,
                        "role" to "PATIENT",
                        "isOnboarded" to true,
                        "createdAt" to System.currentTimeMillis()
                    )
                ).await()
                newUser
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateRole(role: UserRole): Result<Unit> {
        return try {
            val uid = currentUser?.uid ?: throw Exception("Not logged in")
            firestore.collection("users").document(uid).update("role", role.name).await()

            // Call Cloud Function to set custom claim
            // For now, just update locally
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateConsents(consents: DpdpConsents): Result<Unit> {
        return try {
            val uid = currentUser?.uid ?: throw Exception("Not logged in")
            firestore.collection("users").document(uid).update(
                mapOf(
                    "dpdpConsents.storeRecords" to consents.storeRecords,
                    "dpdpConsents.shareWithDoctor" to consents.shareWithDoctor,
                    "dpdpConsents.aiProcessing" to consents.aiProcessing,
                    "dpdpConsents.bloodNetwork" to consents.bloodNetwork,
                    "dpdpConsents.emergencyContact" to consents.emergencyContact,
                    "dpdpConsents.dpdpVersion" to consents.dpdpVersion,
                    "isOnboarded" to true
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun signInWithEmail(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User is null")
            getUserFromFirestore(firebaseUser.uid, firebaseUser.email ?: "", firebaseUser.displayName ?: "")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String,
        phone: String,
        bloodGroup: String,
        city: String
    ): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("User is null")

            // Update display name
            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                .setDisplayName(displayName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // Create user document (app only onboards patients)
            firestore.collection("users").document(firebaseUser.uid).set(
                mapOf(
                    "email" to email,
                    "name" to displayName,
                    "displayName" to displayName,
                    "phone" to phone,
                    "bloodGroup" to bloodGroup,
                    "city" to city,
                    "role" to "PATIENT",
                    "isOnboarded" to true,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()

            Result.success(
                User(
                    uid = firebaseUser.uid,
                    email = email,
                    displayName = displayName,
                    role = UserRole.PATIENT,
                    isOnboarded = true
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getUserFromFirestore(uid: String, email: String, displayName: String): Result<User> {
        val userDoc = firestore.collection("users").document(uid).get().await()

        return if (userDoc.exists()) {
            val storedName = userDoc.getString("name")
            if (storedName.isNullOrBlank() && displayName.isNotBlank()) {
                try {
                    firestore.collection("users").document(uid).update("name", displayName).await()
                } catch (_: Exception) { }
            }
            Result.success(
                User(
                    uid = uid,
                    email = email,
                    displayName = storedName ?: displayName,
                    photoUrl = userDoc.getString("photoUrl") ?: "",
                    role = userDoc.getString("role")?.let { UserRole.valueOf(it) } ?: UserRole.PATIENT,
                    isOnboarded = userDoc.getBoolean("isOnboarded") ?: true,
                    dpdpConsents = DpdpConsents(
                        storeRecords = userDoc.getBoolean("dpdpConsents.storeRecords") ?: false,
                        shareWithDoctor = userDoc.getBoolean("dpdpConsents.shareWithDoctor") ?: false,
                        aiProcessing = userDoc.getBoolean("dpdpConsents.aiProcessing") ?: false,
                        bloodNetwork = userDoc.getBoolean("dpdpConsents.bloodNetwork") ?: false,
                        emergencyContact = userDoc.getBoolean("dpdpConsents.emergencyContact") ?: false
                    )
                )
            )
        } else {
            // New user - create document (app only onboards patients)
            firestore.collection("users").document(uid).set(
                mapOf(
                    "email" to email,
                    "displayName" to displayName,
                    "role" to "PATIENT",
                    "isOnboarded" to true,
                    "createdAt" to System.currentTimeMillis()
                )
            ).await()
            Result.success(
                User(
                    uid = uid,
                    email = email,
                    displayName = displayName,
                    role = UserRole.PATIENT,
                    isOnboarded = true
                )
            )
        }
    }

    suspend fun getCurrentUser(): Result<User> {
        return try {
            val firebaseUser = auth.currentUser
                ?: throw Exception("No logged-in user")
            getUserFromFirestore(
                firebaseUser.uid,
                firebaseUser.email ?: "",
                firebaseUser.displayName ?: ""
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return try {
            val uid = currentUser?.uid ?: throw Exception("Not logged in")
            firestore.collection("users").document(uid).delete().await()
            currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
