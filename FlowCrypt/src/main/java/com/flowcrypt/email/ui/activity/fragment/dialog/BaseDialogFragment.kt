/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
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
    get() = Node.getInstance(requireActivity().application).liveData.value ?: false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    registerNodeIdlingResources()
  }

  fun showToast(string: String) {
    Toast.makeText(context, string, Toast.LENGTH_SHORT).show()
  }

  protected open fun onNodeStateChanged(newState: Boolean?) {

  }

  private fun registerNodeIdlingResources() {
    Node.getInstance(requireActivity().application).liveData.observe(this, Observer { aBoolean ->
      nodeIdlingResource.setIdleState(aBoolean!!)
      onNodeStateChanged(aBoolean)
    })
  }
}
