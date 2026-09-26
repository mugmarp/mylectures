package com.mustime.core.network

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

object FirebaseFactory {
    val auth: FirebaseAuth by lazy { Firebase.auth }
    val db: FirebaseFirestore by lazy { Firebase.firestore }
    val uid: String? get() = auth.currentUser?.uid
}
