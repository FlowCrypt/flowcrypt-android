/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.SuggestionSpan;
import android.util.AttributeSet;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;

import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;

import java.util.Arrays;
import java.util.List;

import androidx.annotation.NonNull;

/**
 * The custom realization of {@link NachoTextView}.
 *
 * @author DenBond7
 * Date: 19.05.2017
 * Time: 8:52
 * E-mail: DenBond7@gmail.com
 */

public class PgpContactsNachoTextView extends NachoTextView {
  private GestureDetector gestureDetector;
  private OnChipLongClickListener listener;
  private ChipLongClickOnGestureListener gestureListener;

  public PgpContactsNachoTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
    this.gestureListener = new ChipLongClickOnGestureListener();
    this.gestureDetector = new GestureDetector(getContext(), gestureListener);
    setCustomSelectionActionModeCallback(new CustomActionModeCallback());
  }

  /**
   * This method prevents add a duplicate email from the dropdown to TextView.
   */
  @Override
  public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
    CharSequence text = this.getFilter().convertResultToString(this.getAdapter().getItem(position));

    if (!getText().toString().contains(text)) {
      super.onItemClick(adapterView, view, position, id);
    }

  }

  @Override
  public String toString() {
    //Todo In this code I received a crash. Need to fix it.
    try {
      return super.toString();
    } catch (Exception e) {
      e.printStackTrace();
      ExceptionUtil.handleError(e);
    }
    return getText().toString();
  }

  @Override
  public boolean onTouchEvent(@NonNull MotionEvent event) {
    gestureDetector.onTouchEvent(event);
    return super.onTouchEvent(event);
  }

  @Override
  public boolean performClick() {
    return super.performClick();
  }

  @Override
  public boolean onTextContextMenuItem(int id) {
    int start = getSelectionStart();
    int end = getSelectionEnd();

    switch (id) {
      case android.R.id.cut:
        setClipboardData(ClipData.newPlainText(null, removeSuggestionSpans(getTextWithPlainTextSpans(start, end))));
        getText().delete(getSelectionStart(), getSelectionEnd());
        return true;

      case android.R.id.copy:
        setClipboardData(ClipData.newPlainText(null, removeSuggestionSpans(getTextWithPlainTextSpans(start, end))));
        return true;

      case android.R.id.paste:
        StringBuilder stringBuilder = new StringBuilder();
        ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
          ClipData clip = clipboardManager.getPrimaryClip();
          if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
              stringBuilder.append(clip.getItemAt(i).coerceToStyledText(getContext()));
            }
          }

          List<String> emails = getChipValues();
          if (emails.contains(stringBuilder.toString())) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, " "));
          }
        }

        return super.onTextContextMenuItem(id);

      default:
        return super.onTextContextMenuItem(id);
    }
  }

  public void setListener(OnChipLongClickListener listener) {
    this.listener = listener;
  }

  private void setClipboardData(ClipData clip) {
    ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
    if (clipboard != null) {
      clipboard.setPrimaryClip(clip);
    }
  }

  /**
   * Get a formatted text of a selection.
   *
   * @param start The begin position of the selected text.
   * @param end   The end position of the selected text.
   * @return A formatted text.
   */
  private CharSequence getTextWithPlainTextSpans(int start, int end) {
    Editable editable = getText();

    if (getChipTokenizer() != null) {
      StringBuilder stringBuilder = new StringBuilder();

      List<Chip> chips = Arrays.asList(getChipTokenizer().findAllChips(start, end, editable));
      for (int i = 0; i < chips.size(); i++) {
        Chip chip = chips.get(i);
        stringBuilder.append(chip.getText());
        if (i != chips.size() - 1) {
          stringBuilder.append(SingleCharacterSpanChipTokenizer.CHIP_SEPARATOR_WHITESPACE);
        }
      }

      return stringBuilder.toString();
    }
    return editable.subSequence(start, end).toString();
  }

  private CharSequence removeSuggestionSpans(CharSequence text) {
    if (text instanceof Spanned) {
      Spannable spannable;
      if (text instanceof Spannable) {
        spannable = (Spannable) text;
      } else {
        spannable = new SpannableString(text);
        text = spannable;
      }

      SuggestionSpan[] spans = spannable.getSpans(0, text.length(), SuggestionSpan.class);
      for (SuggestionSpan span : spans) {
        spannable.removeSpan(span);
      }
    }
    return text;
  }

  public interface OnChipLongClickListener {
    /**
     * Called when a chip in this TextView is long clicked.
     *
     * @param nachoTextView A current view
     * @param chip          the {@link Chip} that was clicked
     * @param event         the {@link MotionEvent} that caused the touch
     */
    void onChipLongClick(NachoTextView nachoTextView, @NonNull Chip chip, MotionEvent event);
  }

  /**
   * A custom realization of {@link ActionMode.Callback} which describes a logic of the text manipulation.
   */
  private class CustomActionModeCallback implements ActionMode.Callback {

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
      return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
      boolean isMenuModified = false;
      for (int i = 0; i < menu.size(); i++) {
        MenuItem menuItem = menu.getItem(i);
        if (menuItem != null) {
          switch (menuItem.getItemId()) {
            case android.R.id.cut:
            case android.R.id.copy:
              break;

            default:
              menu.removeItem(menuItem.getItemId());
              isMenuModified = true;
          }
        }
      }

      return isMenuModified;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem menuItem) {
      switch (menuItem.getItemId()) {
        case android.R.id.copy:
          onTextContextMenuItem(android.R.id.copy);
          mode.finish();
          return true;
      }
      return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {

    }
  }

  private class ChipLongClickOnGestureListener extends GestureDetector.SimpleOnGestureListener {
    public void onLongPress(MotionEvent event) {
      super.onLongPress(event);
      performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

      if (listener != null) {
        Chip chip = findLongClickedChip(event);

        if (chip != null) {
          listener.onChipLongClick(PgpContactsNachoTextView.this, chip, event);
        }
      }
    }

    private Chip findLongClickedChip(MotionEvent event) {
      if (getChipTokenizer() == null) {
        return null;
      }

      Editable text = getText();
      int offset = getOffsetForPosition(event.getX(), event.getY());
      List<Chip> chips = getAllChips();
      for (Chip chip : chips) {
        int chipStart = getChipTokenizer().findChipStart(chip, text);
        int chipEnd = getChipTokenizer().findChipEnd(chip, text);
        if (chipStart <= offset && offset <= chipEnd) {
          float eventX = event.getX();
          float startX = getPrimaryHorizontalForX(chipStart);
          float endX = getPrimaryHorizontalForX(chipEnd - 1);

          int offsetLineNumber = getLineForOffset(offset);
          int chipLineNumber = getLineForOffset(chipEnd - 1);

          if (startX <= eventX && eventX <= endX && offsetLineNumber == chipLineNumber) {
            return chip;
          }
        }
      }
      return null;
    }

    private float getPrimaryHorizontalForX(int offset) {
      Layout layout = getLayout();
      return layout.getPrimaryHorizontal(offset);
    }

    private int getLineForOffset(int offset) {
      return getLayout().getLineForOffset(offset);
    }
  }
}
