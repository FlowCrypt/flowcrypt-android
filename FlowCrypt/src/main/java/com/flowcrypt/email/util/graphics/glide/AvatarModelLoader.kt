/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: denbond7
 */

package com.flowcrypt.email.util.graphics.glide

import android.content.Context
import android.graphics.Bitmap
import android.util.TypedValue
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.flowcrypt.email.R
import com.flowcrypt.email.util.AvatarGenerator


/**
 * ref https://bumptech.github.io/glide/tut/custom-modelloader.html
 *
 * @author Denys Bondarenko
 */
class AvatarModelLoader(private val fontSize: Float) : ModelLoader<String, Bitmap> {
  override fun buildLoadData(
    model: String,
    width: Int,
    height: Int,
    options: Options
  ): ModelLoader.LoadData<Bitmap> {
    return ModelLoader.LoadData(ObjectKey(model), AvatarDataFetcher(model, width, height, fontSize))
  }

  override fun handles(model: String): Boolean {
    return model.startsWith(SCHEMA_AVATAR, true)
  }

  class AvatarModelLoaderFactory(private val context: Context) :
    ModelLoaderFactory<String, Bitmap> {
    override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<String, Bitmap> {
      val fontSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_PX,
        context.resources.getDimension(R.dimen.default_text_size_very_big),
        context.resources.displayMetrics
      )
      return AvatarModelLoader(fontSize)
    }

    override fun teardown() {}
  }

  class AvatarDataFetcher(
    private val key: String,
    private val width: Int,
    private val height: Int,
    private val fontSize: Float
  ) :
    DataFetcher<Bitmap> {
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in Bitmap>) {
      val bitmap = AvatarGenerator.generate(
        text = key.replaceFirst(SCHEMA_AVATAR, ""),
        bitmapWidth = width,
        bitmapHeight = height,
        fontSize = fontSize
      )

      callback.onDataReady(bitmap)
    }

    //Intentionally empty only because we're not opening an InputStream or another I/O resource!
    override fun cleanup() {}

    // There is no implementation as this operation is fast.
    override fun cancel() {}

    override fun getDataClass(): Class<Bitmap> = Bitmap::class.java
    override fun getDataSource(): DataSource = DataSource.LOCAL
  }

  companion object {
    const val SCHEMA_AVATAR = "avatar:"
  }
}