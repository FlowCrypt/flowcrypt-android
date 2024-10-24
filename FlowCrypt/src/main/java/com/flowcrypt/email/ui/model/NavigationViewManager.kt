/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.ui.model

import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.NavHeaderBinding
import com.flowcrypt.email.extensions.android.widget.useGlideToApplyImageFromSource
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.visibleOrGone

class NavigationViewManager(
  activity: Activity,
  private val navHeaderActionsListener: NavHeaderActionsListener
) {
  var navHeaderBinding: NavHeaderBinding? = null
  val accountManagementLayout: LinearLayout = LinearLayout(activity).apply {
    orientation = LinearLayout.VERTICAL
    layoutParams = ViewGroup.LayoutParams(
      ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    )
    gone()
  }

  /**
   * Init the user profile in the top of the navigation view.
   *
   * @param headerView The view which contains user profile views.
   */
  fun initUserProfileView(headerView: View, accountEntity: AccountEntity) {
    navHeaderBinding = NavHeaderBinding.bind(headerView)

    if (accountEntity.displayName.isNullOrEmpty()) {
      navHeaderBinding?.textViewActiveUserDisplayName?.gone()
    } else {
      navHeaderBinding?.textViewActiveUserDisplayName?.text = accountEntity.displayName
    }
    navHeaderBinding?.textViewActiveUserEmail?.text = accountEntity.email

    navHeaderBinding?.imageViewActiveUserPhoto?.useGlideToApplyImageFromSource(
      source = accountEntity.avatarResource,
      placeholderId = R.drawable.ic_account_default_photo,
      applyCircleTransformation = true
    )

    navHeaderBinding?.layoutUserDetails?.setOnClickListener(object :
      View.OnClickListener {
      private var isExpanded: Boolean = false

      override fun onClick(v: View) {
        navHeaderBinding?.imageViewExpandAccountManagement?.setImageResource(
          if (isExpanded) R.drawable.ic_arrow_drop_down else R.drawable.ic_arrow_drop_up
        )
        accountManagementLayout.visibleOrGone(!isExpanded)
        navHeaderActionsListener.onAccountsMenuExpanded(isExpanded)
        isExpanded = !isExpanded
      }
    })
  }

  /**
   * Generate view which contains information about added accounts and using him we can add a new one.
   *
   * @return The generated view.
   */
  fun genAccountsLayout(context: Context, accounts: List<AccountEntity>): ViewGroup {
    accountManagementLayout.removeAllViews()
    for (account in accounts) {
      accountManagementLayout.addView(generateAccountItemView(context, account))
    }
    accountManagementLayout.addView(
      LayoutInflater.from(context).inflate(
        R.layout.add_account, accountManagementLayout, false
      ).apply {
        setOnClickListener {
          navHeaderBinding?.layoutUserDetails?.performClick()
          navHeaderActionsListener.onAddAccountClick()
        }
      })
    return accountManagementLayout
  }

  private fun generateAccountItemView(context: Context, account: AccountEntity): View {
    val view = LayoutInflater.from(context)
      .inflate(R.layout.nav_menu_account_item, accountManagementLayout, false)
    view.tag = account

    val imageViewActiveUserPhoto = view.findViewById<ImageView>(R.id.imageViewActiveUserPhoto)
    val textViewName = view.findViewById<TextView>(R.id.textViewUserDisplayName)
    val textViewEmail = view.findViewById<TextView>(R.id.textViewUserEmail)

    if (account.displayName.isNullOrEmpty()) {
      textViewName.visibility = View.GONE
    } else {
      textViewName.text = account.displayName
    }
    textViewEmail.text = account.email

    imageViewActiveUserPhoto.useGlideToApplyImageFromSource(
      source = account.avatarResource,
      placeholderId = R.drawable.ic_account_default_photo,
      applyCircleTransformation = true
    )

    view.setOnClickListener {
      navHeaderBinding?.layoutUserDetails?.performClick()
      navHeaderActionsListener.onSwitchAccountClick(account)
    }

    return view
  }

  interface NavHeaderActionsListener {
    fun onAccountsMenuExpanded(isExpanded: Boolean)
    fun onAddAccountClick()
    fun onSwitchAccountClick(accountEntity: AccountEntity)
  }
}
