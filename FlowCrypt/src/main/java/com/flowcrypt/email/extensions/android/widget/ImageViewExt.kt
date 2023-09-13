/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.android.widget

import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.TransitionOptions
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions

/**
 * @author Denys Bondarenko
 */
fun ImageView?.useGlideToApplyImageFromSource(
  source: Any,
  placeholderId: Int? = null,
  transitionOptions: TransitionOptions<*, in Drawable>? = null,
  diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC,
  skipMemoryCache: Boolean = false,
  applyCircleTransformation: Boolean = false,
) {
  this ?: return

  var requestOptions = RequestOptions()
    .skipMemoryCache(skipMemoryCache)
    .diskCacheStrategy(diskCacheStrategy)

  requestOptions = if (applyCircleTransformation) {
    requestOptions.transform(MultiTransformation(CenterCrop(), CircleCrop()))
  } else {
    requestOptions.transform(CenterCrop())
  }

  placeholderId?.let {
    //https://bumptech.github.io/glide/doc/placeholders.html#are-transformations-applied-to-placeholders
    requestOptions = requestOptions.placeholder(it).error(it)
  }

  var requestBuilder = Glide.with(context)
    .load(source)
    .apply(requestOptions)
  transitionOptions?.let {
    requestBuilder = requestBuilder.transition(it)
  }
  requestBuilder.into(this)
}