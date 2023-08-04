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
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.flowcrypt.email.R
import com.flowcrypt.email.database.entity.AccountEntity
import com.flowcrypt.email.databinding.NavHeaderBinding
import com.flowcrypt.email.extensions.gone
import com.flowcrypt.email.extensions.visibleOrGone
import com.flowcrypt.email.util.graphics.glide.transformations.CircleTransformation

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
   * @param view The view which contains user profile views.
   */
  fun initUserProfileView(context: Context, headerView: View, accountEntity: AccountEntity) {
    navHeaderBinding = NavHeaderBinding.bind(headerView)

    if (accountEntity.displayName.isNullOrEmpty()) {
      navHeaderBinding?.textViewActiveUserDisplayName?.gone()
    } else {
      navHeaderBinding?.textViewActiveUserDisplayName?.text = accountEntity.displayName
    }
    navHeaderBinding?.textViewActiveUserEmail?.text = accountEntity.email

    val resource = if (accountEntity.photoUrl?.isNotEmpty() == true) {
      accountEntity.photoUrl
    } else {
      R.mipmap.ic_account_default_photo
    }
    navHeaderBinding?.imageViewActiveUserPhoto?.let {
      Glide.with(context)
        .load(resource)
        .apply(
          RequestOptions()
            .centerCrop()
            .transform(CircleTransformation())
            .error(R.mipmap.ic_account_default_photo)
        )
        .into(it)
    }

    navHeaderBinding?.layoutUserDetails?.setOnClickListener(object :
      View.OnClickListener {
      private var isExpanded: Boolean = false

      override fun onClick(v: View) {
        navHeaderBinding?.imageViewExpandAccountManagement?.setImageResource(
          if (isExpanded) R.mipmap.ic_arrow_drop_down else R.mipmap.ic_arrow_drop_up
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

    if (account.photoUrl?.isNotEmpty() == true) {
      Glide.with(context)
        .load(account.photoUrl)
        .apply(
          RequestOptions()
            .centerCrop()
            .transform(CircleTransformation())
            .error(R.mipmap.ic_account_default_photo)
        )
        .into(imageViewActiveUserPhoto)
    }

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
