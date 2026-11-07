package com.example.carelink.data

internal fun showOfflineWarning(
    networkAvailable: Boolean,
    isAuthenticated: Boolean,
    firebaseAvailable: Boolean?
): Boolean = !networkAvailable || (isAuthenticated && firebaseAvailable == false)
