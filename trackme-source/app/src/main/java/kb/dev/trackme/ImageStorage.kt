package kb.dev.trackme

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class ImageStorage(private val context: Context) {
    fun storeImage(
        image: Bitmap,
        imageName: String = System.currentTimeMillis().toString()
    ): String {
        val tag = "storeImage"
        val pictureFile = File("${context.filesDir.path}/$imageName.jpg")
        try {
            val fos = FileOutputStream(pictureFile)
            image.compress(Bitmap.CompressFormat.PNG, 90, fos)
            fos.close()
            Log.e("save image success", pictureFile.path)
            return pictureFile.absolutePath
        } catch (e: FileNotFoundException) {
            Log.e(tag, "File not found: " + e.message)
        } catch (e: IOException) {
            Log.e(tag, "Error accessing file: " + e.message)
        }
        return ""
    }
}