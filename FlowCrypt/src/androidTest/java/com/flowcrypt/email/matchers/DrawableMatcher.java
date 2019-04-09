/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.matchers;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * See details here https://github.com/dbottillo/Blog/blob/espresso_match_imageview/app/src/androidTest/java/com
 * /danielebottillo/blog/config/DrawableMatcher.java
 *
 * @author Denis Bondarenko
 * Date: 3/15/19
 * Time: 5:17 PM
 * E-mail: DenBond7@gmail.com
 */
public class DrawableMatcher extends TypeSafeMatcher<View> {

  public static final int EMPTY = -1;
  public static final int ANY = -2;
  private final int expectedId;
  private String resourceName;

  DrawableMatcher(int expectedId) {
    super(View.class);
    this.expectedId = expectedId;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("with drawable from resource id: ");
    description.appendValue(expectedId);
    if (resourceName != null) {
      description.appendText("[");
      description.appendText(resourceName);
      description.appendText("]");
    }
  }

  @Override
  protected boolean matchesSafely(View target) {
    if (!(target instanceof ImageView)) {
      return false;
    }
    ImageView imageView = (ImageView) target;
    if (expectedId == EMPTY) {
      return imageView.getDrawable() == null;
    }
    if (expectedId == ANY) {
      return imageView.getDrawable() != null;
    }
    Resources resources = target.getContext().getResources();
    Drawable expectedDrawable = resources.getDrawable(expectedId, target.getContext().getTheme());
    resourceName = resources.getResourceEntryName(expectedId);

    if (expectedDrawable == null) {
      return false;
    }

    Bitmap bitmap = getBitmap(imageView.getDrawable());
    Bitmap otherBitmap = getBitmap(expectedDrawable);
    return bitmap.sameAs(otherBitmap);
  }

  private Bitmap getBitmap(Drawable drawable) {
    Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
        Bitmap.Config.ARGB_8888);
    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);
    return bitmap;
  }
}
