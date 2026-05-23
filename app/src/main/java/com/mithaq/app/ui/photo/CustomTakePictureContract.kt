package com.mithaq.app.ui.photo

import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract

class CustomTakePictureContract : ActivityResultContract<Uri, Boolean>() {
    override fun createIntent(context: Context, input: Uri): Intent {
        return Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            .putExtra(MediaStore.EXTRA_OUTPUT, input)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .apply {
                clipData = ClipData.newRawUri(null, input)
            }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): Boolean {
        return resultCode == Activity.RESULT_OK
    }
}
