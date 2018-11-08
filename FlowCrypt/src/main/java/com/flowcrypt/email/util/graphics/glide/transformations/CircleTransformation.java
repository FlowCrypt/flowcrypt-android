/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.util.graphics.glide.transformations;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;

import java.security.MessageDigest;

import androidx.annotation.NonNull;

/**
 * The circle transformation for the Glide.
 * For details see https://stackoverflow
 * .com/questions/25278821/how-to-round-an-image-with-glide-library/25806229#25806229
 *
 * @author Denis Bondarenko
 * Date: 17.07.2017
 * Time: 15:59
 * E-mail: DenBond7@gmail.com
 */

public class CircleTransformation extends BitmapTransformation {

  @Override
  public void updateDiskCacheKey(MessageDigest messageDigest) {

  }

  @Override
  protected Bitmap transform(@NonNull BitmapPool bitmapPool, @NonNull Bitmap bitmap,
                             int i, int i1) {
    return circleCrop(bitmapPool, bitmap);
  }

  public String getId() {
    return getClass().getName();
  }

  private static Bitmap circleCrop(BitmapPool pool, Bitmap source) {
    if (source == null) return null;

    int size = Math.min(source.getWidth(), source.getHeight());
    int x = (source.getWidth() - size) / 2;
    int y = (source.getHeight() - size) / 2;

    Bitmap squared = Bitmap.createBitmap(source, x, y, size, size);
    Bitmap result = pool.get(size, size, Bitmap.Config.ARGB_8888);

    Canvas canvas = new Canvas(result);
    Paint paint = new Paint();
    paint.setShader(new BitmapShader(squared, BitmapShader.TileMode.CLAMP, BitmapShader
        .TileMode.CLAMP));
    paint.setAntiAlias(true);
    float r = size / 2f;
    canvas.drawCircle(r, r, r, paint);
    return result;
  }
}
