package com.project.dimediaryapp.util

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirestoreUtil(private val context: Context) {
    private val TAG = "FirestoreUtil"
    private val db = Firebase.firestore

    interface FirestoreCallback {
        fun onComplete(success: Boolean, documentId: String? = null)
    }

    fun createOrUpdateDocument(collection: String, documentId: String?, data: Map<String, Any>, callback: FirestoreCallback? = null) {
        if (documentId == null) {
            // Use add to let Firestore generate a unique ID
            db.collection(collection)
                .add(data)
                .addOnSuccessListener { documentReference ->
                    callback?.onComplete(true, documentReference.id)
                }
                .addOnFailureListener { e ->
                    callback?.onComplete(false, null)
                }
        } else {
            // Use set to specify the document ID
            db.collection(collection).document(documentId)
                .set(data)
                .addOnSuccessListener {
                    callback?.onComplete(true, documentId)
                }
                .addOnFailureListener { e ->
                    callback?.onComplete(false, null)
                }
        }
    }

    fun getDocumentIdByUserId(
        collectionPath: String,
        userId: String,
        onSuccess: (String?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        db.collection(collectionPath)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot != null && !querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]
                    Log.d(TAG, "Document found with ID: ${document.id}")
                    PreferenceHelper.saveUserId(context, document.id) // Save document ID
                    onSuccess(document.id)
                } else {
                    Log.d(TAG, "No document found with userId: $userId")
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting document by userId", e)
                onFailure(e)
            }
    }

    fun readDocument(
        collectionPath: String,
        documentId: String,
        onSuccess: (Map<String, Any>?) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val documentRef = db.collection(collectionPath).document(documentId)

        documentRef.get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    Log.d(TAG, "DocumentSnapshot data: ${document.data}")
                    onSuccess(document.data)
                } else {
                    Log.d(TAG, "No such document")
                    onSuccess(null)
                }
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error getting document", e)
                onFailure(e)
            }
    }

    fun updateDocument(
        collectionPath: String,
        documentId: String,
        data: Map<String, Any>,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val documentRef = db.collection(collectionPath).document(documentId)

        documentRef.update(data)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully updated!")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error updating document", e)
                onFailure(e)
            }
    }

    fun deleteDocument(
        collectionPath: String,
        documentId: String,
        onSuccess: () -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val documentRef = db.collection(collectionPath).document(documentId)

        documentRef.delete()
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot successfully deleted!")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error deleting document", e)
                onFailure(e)
            }
    }
}
