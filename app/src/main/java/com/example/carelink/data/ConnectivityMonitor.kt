package com.example.carelink.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities

// NetworkCallback reacts immediately when connectivity changes and avoids polling in the UI.
class ConnectivityMonitor(
    context: Context,
    private val onConnectivityChanged: (Boolean) -> Unit
) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = onConnectivityChanged(true)
        override fun onLost(network: Network) = onConnectivityChanged(isOnline())
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            onConnectivityChanged(capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED))
        }
    }

    fun start() {
        onConnectivityChanged(isOnline())
        manager.registerDefaultNetworkCallback(callback)
    }

    fun stop() {
        runCatching { manager.unregisterNetworkCallback(callback) }
    }

    fun isOnline(): Boolean {
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        // INTERNET alone only means a network is configured. VALIDATED confirms it reached the internet.
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
