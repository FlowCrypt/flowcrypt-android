/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.loader.content.Loader
import com.flowcrypt.email.R
import com.flowcrypt.email.model.results.LoaderResult
import com.flowcrypt.email.node.Node
import com.flowcrypt.email.util.idling.NodeIdlingResource

/**
 * The base dialog fragment.
 *
 * @author Denis Bondarenko
 * Date: 01.08.2017
 * Time: 10:04
 * E-mail: DenBond7@gmail.com
 */

open class BaseDialogFragment : DialogFragment() {
  @get:VisibleForTesting
  val nodeIdlingResource: NodeIdlingResource = NodeIdlingResource()

  protected val isNodeReady: Boolean
    get() = Node.getInstance(activity!!.application).liveData.value ?: false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    registerNodeIdlingResources()
  }

  fun handleLoaderResult(loader: Loader<*>, loaderResult: LoaderResult?) {
    if (loaderResult != null) {
      when {
        loaderResult.result != null -> onSuccess(loader.id, loaderResult.result)
        loaderResult.exception != null -> onError(loader.id, loaderResult.exception)
        else -> showToast(getString(R.string.unknown_error))
      }
    } else {
      showToast(getString(R.string.unknown_error))
    }
  }

  fun onError(loaderId: Int, e: Exception?) {

  }

  fun onSuccess(loaderId: Int, result: Any?) {

  }

  fun showToast(string: String) {
    Toast.makeText(context, string, Toast.LENGTH_SHORT).show()
  }

  protected open fun onNodeStateChanged(newState: Boolean?) {

  }

  private fun registerNodeIdlingResources() {
    Node.getInstance(activity!!.application).liveData.observe(this, Observer { aBoolean ->
      nodeIdlingResource.setIdleState(aBoolean!!)
      onNodeStateChanged(aBoolean)
    })
  }
}
