package kb.dev.trackme.common

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shaohui.advancedluban.Luban
import me.shaohui.advancedluban.OnCompressListener
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ImageStorage(private val context: Context) {
    suspend fun storeImage(
        image: Bitmap,
        imageName: String = System.currentTimeMillis().toString()
    ): String = withContext(Dispatchers.IO) {
        suspendCoroutine { ct ->
            val pictureFile = File("${context.filesDir.path}/$imageName.jpg")
            try {
                val fos = FileOutputStream(pictureFile)
                image.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.close()
                Luban.compress(context, pictureFile)
                    .putGear(Luban.THIRD_GEAR)      // set the compress mode, default is : THIRD_GEAR
                    .launch(object : OnCompressListener {
                        override fun onStart() {
                        }

                        override fun onSuccess(file: File) {
                            ct.resume(file.absolutePath)
                        }

                        override fun onError(e: Throwable?) {
                            e?.printStackTrace()
                        }

                    })
            } catch (e: FileNotFoundException) {
                Log.e(TAG, "File not found: " + e.message)
            } catch (e: IOException) {
                Log.e(TAG, "Error accessing file: " + e.message)
            }
        }
    }

    companion object {
        private val TAG = ImageStorage::class.java.simpleName
    }
}