/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util

import android.content.Context
import android.graphics.Bitmap
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.module.AppGlideModule
import com.flowcrypt.email.util.graphics.glide.AvatarModelLoader

/**
 * [AppGlideModule] implementation of the application.
 * See http://sjudd.github.io/glide/doc/generatedapi.html for more details.
 *
 * @author Denys Bondarenko
 */
@GlideModule
class FlowcryptAppGlideModule : AppGlideModule() {
  override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
    super.registerComponents(context, glide, registry)
    registry.prepend(
      String::class.java,
      Bitmap::class.java,
      AvatarModelLoader.AvatarModelLoaderFactory(context)
    )
  }
}
