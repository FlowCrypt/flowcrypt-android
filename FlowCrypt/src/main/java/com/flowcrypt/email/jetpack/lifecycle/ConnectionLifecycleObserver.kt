/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.jetpack.lifecycle

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData

/**
 * @author Denys Bondarenko
 */
class ConnectionLifecycleObserver(context: Context?) : DefaultLifecycleObserver {
  val connectionLiveData: MutableLiveData<Boolean> = MutableLiveData(false)

  private val connectivityManager =
    context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
  private val networkRequest: NetworkRequest = NetworkRequest.Builder()
    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
    .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
    .addTransportType(NetworkCapabilities.TRANSPORT_VPN)
    .addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
    .build()

  private val networkCallback = object : ConnectivityManager.NetworkCallback() {
    override fun onAvailable(network: Network) {
      super.onAvailable(network)
      if (connectionLiveData.value != true) {
        connectionLiveData.postValue(true)
      }
    }

    override fun onLost(network: Network) {
      super.onLost(network)
      connectionLiveData.postValue(false)
    }
  }

  override fun onCreate(owner: LifecycleOwner) {
    super.onCreate(owner)
    connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
  }

  override fun onDestroy(owner: LifecycleOwner) {
    super.onDestroy(owner)
    connectivityManager?.unregisterNetworkCallback(networkCallback)
  }
}
