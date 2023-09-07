/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.extensions.android.widget

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import java.io.File

/**
 * @author Denys Bondarenko
 */
fun ImageView?.useGlideToApplyImageFromSource(
  source: Any,
  placeholderId: Int? = null,
  diskCacheStrategy: DiskCacheStrategy = DiskCacheStrategy.AUTOMATIC,
  skipMemoryCache: Boolean = false,
  applyCircleTransformation: Boolean = false,
) {
  this ?: return

  val castedSource = when (source) {
    is Bitmap -> source
    is Drawable -> source
    is String -> source
    is Uri -> source
    is File -> source
    is Int -> source
    else -> source
  }

  var requestBuilder = Glide.with(this.context)
    .load(castedSource)
    .skipMemoryCache(skipMemoryCache)
    .diskCacheStrategy(diskCacheStrategy)

  requestBuilder = if (placeholderId == null) {
    if (applyCircleTransformation) {
      requestBuilder.apply(
        RequestOptions().transform(
          MultiTransformation(
            CenterCrop(),
            CircleCrop()
          )
        )
      )
    } else {
      requestBuilder.apply(RequestOptions().transform(CenterCrop()))
    }
  } else {
    if (applyCircleTransformation) {
      requestBuilder
        .apply(
          RequestOptions().transform(MultiTransformation(CenterCrop(), CircleCrop()))
            .placeholder(placeholderId)
            .error(placeholderId)
        ).apply(RequestOptions().transform(MultiTransformation(CenterCrop(), CircleCrop())))
    } else {
      requestBuilder
        .apply(
          RequestOptions().transform(CenterCrop()).placeholder(placeholderId).error(placeholderId)
        ).apply(RequestOptions().transform(CenterCrop()))
    }
  }

  requestBuilder.into(this)
}