package com.example.artdecode

import android.app.Application
import android.util.Log
import com.example.artdecode.data.repository.ArtworkRepositoryImpl

class ArtDecode : Application() {
    lateinit var artworkRepository: ArtworkRepositoryImpl
        private set

    override fun onCreate() {
        super.onCreate()
        artworkRepository = ArtworkRepositoryImpl(applicationContext)
        Log.d("ArtDecodeApp", "ArtworkRepositoryImpl singleton initialized.")
    }
}