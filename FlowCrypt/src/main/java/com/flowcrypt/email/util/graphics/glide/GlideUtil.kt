/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util.graphics.glide

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.MultiTransformation
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import java.io.File

/**
 * @author Denys Bondarenko
 */
object GlideUtil {
  fun setImage(source: Any, placeholderId: Int? = null, imageView: ImageView?) {
    imageView ?: return

    val castedSource = when (source) {
      is Bitmap -> source
      is Drawable -> source
      is String -> source
      is Uri -> source
      is File -> source
      is Int -> source
      else -> source
    }
    if (placeholderId == null) {
      Glide.with(imageView.context)
        .load(castedSource)
        .apply(RequestOptions().transform(CenterCrop()))
        .into(imageView)
    } else {
      Glide.with(imageView.context)
        .load(castedSource)
        .apply(
          RequestOptions().transform(CenterCrop()).placeholder(placeholderId).error(placeholderId)
        )
        .apply(RequestOptions().transform(CenterCrop()))
        .into(imageView)
    }
  }

  fun setCircleImage(source: Any, placeholderId: Int? = null, imageView: ImageView?) {
    imageView ?: return

    val castedSource = when (source) {
      is Bitmap -> source
      is Drawable -> source
      is String -> source
      is Uri -> source
      is File -> source
      is Int -> source
      else -> source
    }

    if (placeholderId == null) {
      Glide.with(imageView.context)
        .load(castedSource)
        .apply(RequestOptions().transform(MultiTransformation(CenterCrop(), CircleCrop())))
        .into(imageView)
    } else {
      Glide.with(imageView.context)
        .load(castedSource)
        .apply(
          RequestOptions().transform(MultiTransformation(CenterCrop(), CircleCrop()))
            .placeholder(placeholderId).error(placeholderId)
        )
        .apply(RequestOptions().transform(MultiTransformation(CenterCrop(), CircleCrop())))
        .into(imageView)
    }
  }
}