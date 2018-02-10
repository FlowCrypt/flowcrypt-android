/*
 * Business Source License 1.0 © 2017 FlowCrypt Limited (human@flowcrypt.com).
 * Use limitations apply. See https://github.com/FlowCrypt/flowcrypt-android/blob/master/LICENSE
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.widget;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.Layout;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;

import com.flowcrypt.email.util.exception.ExceptionUtil;
import com.hootsuite.nachos.NachoTextView;
import com.hootsuite.nachos.chip.Chip;

import java.util.List;

/**
 * The custom realization of {@link NachoTextView}.
 *
 * @author DenBond7
 *         Date: 19.05.2017
 *         Time: 8:52
 *         E-mail: DenBond7@gmail.com
 */

public class PgpContactsNachoTextView extends NachoTextView {
    private GestureDetector gestureDetector;
    private OnChipLongClickListener onChipLongClickListener;
    private ChipLongClickOnGestureListener chipLongClickOnGestureListener;

    public PgpContactsNachoTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setLongClickable(false);
        this.chipLongClickOnGestureListener = new ChipLongClickOnGestureListener();
        this.gestureDetector = new GestureDetector(getContext(), chipLongClickOnGestureListener);
    }

    /**
     * This method prevents add a duplicate email from the dropdown to TextView.
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {

        CharSequence text =
                this.getFilter().convertResultToString(this.getAdapter().getItem(position));

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
        Chip touchedChip = findLongClickedChip(event);

        if (touchedChip != null) {
            chipLongClickOnGestureListener.setChip(touchedChip);
            gestureDetector.onTouchEvent(event);
        }

        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void setOnChipLongClickListener(OnChipLongClickListener onChipLongClickListener) {
        this.onChipLongClickListener = onChipLongClickListener;
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
            int chipEnd = getChipTokenizer().findChipEnd(chip, text); // This is actually the index of the character
            if (chipStart <= offset && offset <= chipEnd) {
                float startX = getPrimaryHorizontalForX(chipStart);
                float endX = getPrimaryHorizontalForX(chipEnd - 1);
                float eventX = event.getX();
                if (startX <= eventX && eventX <= endX) {
                    return chip;
                }
            }
        }
        return null;
    }

    private float getPrimaryHorizontalForX(int index) {
        Layout layout = getLayout();
        return layout.getPrimaryHorizontal(index);
    }

    public interface OnChipLongClickListener {
        /**
         * Called when a chip in this TextView is long clicked.
         *
         * @param chip  the {@link Chip} that was clicked
         * @param event the {@link MotionEvent} that caused the touch
         */
        void onChipLongClick(Chip chip, MotionEvent event);
    }

    private class ChipLongClickOnGestureListener extends GestureDetector.SimpleOnGestureListener {
        private Chip chip;

        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

            if (onChipLongClickListener != null) {
                onChipLongClickListener.onChipLongClick(chip, e);
            }
        }

        void setChip(Chip chip) {
            this.chip = chip;
        }
    }
}
