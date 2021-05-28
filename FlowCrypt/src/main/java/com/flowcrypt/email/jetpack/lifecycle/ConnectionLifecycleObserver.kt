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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent

/**
 * @author Denis Bondarenko
 *         Date: 1/14/20
 *         Time: 4:10 PM
 *         E-mail: DenBond7@gmail.com
 */
class ConnectionLifecycleObserver(context: Context?) : LifecycleObserver {
  val connectionLiveData: MutableLiveData<Boolean> = MutableLiveData(false)

  private val connectivityManager =
    context?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
  private val networkRequest: NetworkRequest = NetworkRequest.Builder()
    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
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

  @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
  fun onCreate() {
    connectivityManager?.registerNetworkCallback(networkRequest, networkCallback)
  }

  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  fun onDestroy() {
    connectivityManager?.unregisterNetworkCallback(networkCallback)
  }
}