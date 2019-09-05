/*
 * © 2016-2019 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.flowcrypt.email.R
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.PhotoEditorView

/**
 * It's a dialog which helps edit the given screenshot
 *
 * @author Denis Bondarenko
 *         Date: 9/4/19
 *         Time: 5:03 PM
 *         E-mail: DenBond7@gmail.com
 */
class EditScreenshotDialogFragment : DialogFragment(), View.OnClickListener,
    RadioGroup.OnCheckedChangeListener {
  private lateinit var photoEditor: PhotoEditor
  private lateinit var bitmap: Bitmap
  private lateinit var photoEditorView: PhotoEditorView
  internal var callback: OnScreenshotChangeListener? = null

  fun setOnScreenshotChangeListener(callback: OnScreenshotChangeListener) {
    this.callback = callback
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val bitmapByteArray = arguments?.getByteArray(KEY_BITMAP_BYTES) ?: byteArrayOf()
    bitmapByteArray.let { bitmap = BitmapFactory.decodeByteArray(bitmapByteArray, 0, bitmapByteArray.size) }
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    val dialog = AlertDialog.Builder(activity!!)

    val view = LayoutInflater.from(context).inflate(R.layout.fragment_screenshot_editor,
        if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null, false)

    initViews(view)

    dialog.setView(view)

    photoEditor = PhotoEditor.Builder(context, photoEditorView).build()
    photoEditor.setBrushDrawingMode(true)
    photoEditor.brushColor = ContextCompat.getColor(context!!, R.color.black)
    photoEditor.setOpacity(100)

    return dialog.create()
  }

  private fun initViews(view: View) {
    photoEditorView = view.findViewById(R.id.photoEditorView)
    photoEditorView.source.setImageBitmap(bitmap)

    view.findViewById<View>(R.id.imageButtonUndo)?.setOnClickListener(this)
    view.findViewById<View>(R.id.imageButtonSave)?.setOnClickListener(this)
    view.findViewById<View>(R.id.imageButtonCancel)?.setOnClickListener(this)

    val radioGroup = view.findViewById<RadioGroup>(R.id.radioGroupColors)
    radioGroup?.setOnCheckedChangeListener(this)
  }

  override fun onClick(v: View?) {
    when (v?.id) {
      R.id.imageButtonUndo -> {
        photoEditor.undo()
      }

      R.id.imageButtonSave -> {
        photoEditor.saveAsBitmap(object : OnSaveBitmap {
          override fun onBitmapReady(saveBitmap: Bitmap?) {
            callback?.onScreenshotChanged(UIUtil.getCompressedByteArrayOfBitmap(saveBitmap, 100))
            dismiss()
          }

          override fun onFailure(e: Exception?) {
            Toast.makeText(context, e?.message
                ?: getString(R.string.unknown_error), Toast.LENGTH_SHORT).show()
          }
        })
      }

      R.id.imageButtonCancel -> {
        dismiss()
      }
    }
  }

  override fun onCheckedChanged(group: RadioGroup?, checkedId: Int) {
    when (group?.id) {
      R.id.radioGroupColors -> {
        when (checkedId) {
          R.id.radioButtonFullColor -> {
            photoEditor.brushColor = ContextCompat.getColor(context!!, R.color.black)
            photoEditor.setOpacity(100)
          }

          R.id.radioButtonLightColor -> {
            photoEditor.brushColor = ContextCompat.getColor(context!!, R.color.yellow)
            photoEditor.setOpacity(50)
          }
        }
      }
    }
  }

  interface OnScreenshotChangeListener {
    fun onScreenshotChanged(byteArray: ByteArray?)
  }

  companion object {
    private val KEY_BITMAP_BYTES =
        GeneralUtil.generateUniqueExtraKey("KEY_BITMAP_BYTES", EditScreenshotDialogFragment::class.java)

    fun newInstance(bitmapByteArray: ByteArray?): EditScreenshotDialogFragment {
      val fragment = EditScreenshotDialogFragment()

      val args = Bundle()
      args.putByteArray(KEY_BITMAP_BYTES, bitmapByteArray)
      fragment.arguments = args

      return fragment
    }
  }
}
