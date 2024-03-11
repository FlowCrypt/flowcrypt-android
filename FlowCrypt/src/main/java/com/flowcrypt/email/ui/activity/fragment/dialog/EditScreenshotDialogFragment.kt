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
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.flowcrypt.email.R
import com.flowcrypt.email.databinding.FragmentScreenshotEditorBinding
import com.flowcrypt.email.extensions.navController
import com.flowcrypt.email.util.GeneralUtil
import com.flowcrypt.email.util.UIUtil
import ja.burhanrashid52.photoeditor.OnSaveBitmap
import ja.burhanrashid52.photoeditor.PhotoEditor
import ja.burhanrashid52.photoeditor.shape.ShapeBuilder

/**
 * It's a dialog which helps edit the given screenshot
 *
 * @author Denys Bondarenko
 */
class EditScreenshotDialogFragment : BaseDialogFragment() {
  private var binding: FragmentScreenshotEditorBinding? = null
  private val args by navArgs<EditScreenshotDialogFragmentArgs>()
  private var photoEditor: PhotoEditor? = null
  private var bitmap: Bitmap? = null

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
    binding?.imageButtonUndo?.setOnClickListener { photoEditor?.undo() }
    binding?.imageButtonCancel?.setOnClickListener { navController?.navigateUp() }
    binding?.imageButtonSave?.setOnClickListener {
      photoEditor?.saveAsBitmap(object : OnSaveBitmap {
        override fun onBitmapReady(saveBitmap: Bitmap) {
          navController?.navigateUp()
          setFragmentResult(
            args.requestKey,
            bundleOf(KEY_SCREENSHOT to UIUtil.getCompressedByteArrayOfBitmap(saveBitmap, 100))
          )
        }
      })
    }

    binding?.radioGroupColors?.setOnCheckedChangeListener { _, checkedId ->
      when (checkedId) {
        R.id.radioButtonFullColor -> {
          photoEditor?.setShape(
            ShapeBuilder()
              .withShapeColor(
                ColorUtils.setAlphaComponent(
                  ContextCompat.getColor(
                    requireContext(),
                    android.R.color.black
                  ), 255
                )
              )
          )
        }

        R.id.radioButtonLightColor -> {
          photoEditor?.setShape(
            ShapeBuilder()
              .withShapeColor(
                ColorUtils.setAlphaComponent(
                  ContextCompat.getColor(
                    requireContext(),
                    R.color.yellow
                  ), 127
                )
              )
          )
        }
      }
    }

    binding?.photoEditorView?.let {
      it.source.setImageBitmap(bitmap)
      photoEditor = PhotoEditor.Builder(requireContext(), it).build()
      photoEditor?.setBrushDrawingMode(true)
      photoEditor?.setShape(
        ShapeBuilder()
          .withShapeColor(
            ColorUtils.setAlphaComponent(
              ContextCompat.getColor(
                requireContext(),
                android.R.color.black
              ), 255
            )
          )
      )
    }
  }

  companion object {
    val KEY_SCREENSHOT = GeneralUtil.generateUniqueExtraKey(
      "KEY_SCREENSHOT", EditScreenshotDialogFragment::class.java
    )
  }
}
