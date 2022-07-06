/*
 * © 2016-present FlowCrypt a.s. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.activity.fragment.dialog

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentScreenshotEditorBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.extensions.toast
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor

/**
 * It's a dialog which helps edit the given screenshot
 *
 * @author Denis Bondarenko
 *         Date: 9/4/19
 *         Time: 5:03 PM
 *         E-mail: DenBond7@gmail.com
 */
class EditScreenshotDialogFragment : BaseDialogFragment() {
  private var binding: FragmentScreenshotEditorBinding? = null
  private val args by navArgs<EditScreenshotDialogFragmentArgs>()
  private lateinit var photoEditor: PhotoEditor
  private lateinit var bitmap: Bitmap

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    bitmap = BitmapFactory.decodeByteArray(
      args.screenshot.byteArray, 0, args.screenshot.byteArray.size
    )
  }

  override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
    binding = FragmentScreenshotEditorBinding.inflate(
      LayoutInflater.from(context),
      if ((view != null) and (view is ViewGroup)) view as ViewGroup? else null,
      false
    )

    initViews()

    val builder = AlertDialog.Builder(requireContext()).apply {
      setView(binding?.root)
    }
    return builder.create()
  }

  private fun initViews() {
    binding?.imageButtonUndo?.setOnClickListener { photoEditor.undo() }
    binding?.imageButtonCancel?.setOnClickListener { navController?.navigateUp() }
    binding?.imageButtonSave?.setOnClickListener {
      photoEditor.saveAsBitmap(object : OnSaveBitmap {
        override fun onBitmapReady(saveBitmap: Bitmap?) {
          navController?.navigateUp()
          setFragmentResult(
            REQUEST_KEY_EDIT_SCREENSHOT,
            bundleOf(KEY_SCREENSHOT to UIUtil.getCompressedByteArrayOfBitmap(saveBitmap, 100))
          )
        }

        override fun onFailure(e: Exception?) {
          toast(e?.message ?: e?.javaClass?.simpleName ?: getString(R.string.unknown_error))
        }
      })
    }

    binding?.radioGroupColors?.setOnCheckedChangeListener { _, checkedId ->
      when (checkedId) {
        R.id.radioButtonFullColor -> {
          photoEditor.brushColor = ContextCompat.getColor(requireContext(), android.R.color.black)
          photoEditor.setOpacity(100)
        }

        R.id.radioButtonLightColor -> {
          photoEditor.brushColor = ContextCompat.getColor(requireContext(), R.color.yellow)
          photoEditor.setOpacity(50)
        }
      }
    }

    binding?.photoEditorView?.source?.setImageBitmap(bitmap)

    photoEditor = PhotoEditor.Builder(context, binding?.photoEditorView).build()
    photoEditor.setBrushDrawingMode(true)
    photoEditor.brushColor = ContextCompat.getColor(requireContext(), android.R.color.black)
    photoEditor.setOpacity(100)
  }

  companion object {
    val REQUEST_KEY_EDIT_SCREENSHOT = GeneralUtil.generateUniqueExtraKey(
      "REQUEST_KEY_EDIT_SCREENSHOT", EditScreenshotDialogFragment::class.java
    )

    val KEY_SCREENSHOT = GeneralUtil.generateUniqueExtraKey(
      "KEY_SCREENSHOT", EditScreenshotDialogFragment::class.java
    )
  }
}
