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
        gestureDetector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    public void setOnChipLongClickListener(OnChipLongClickListener onChipLongClickListener) {
        this.onChipLongClickListener = onChipLongClickListener;
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
        public void onLongPress(MotionEvent event) {
            super.onLongPress(event);
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

            if (onChipLongClickListener != null) {
                Chip chip = findLongClickedChip(event);

                if (chip != null) {
                    onChipLongClickListener.onChipLongClick(chip, event);
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
