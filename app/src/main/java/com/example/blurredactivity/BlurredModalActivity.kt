package com.example.blurredactivity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v8.renderscript.RenderScript
import android.view.View
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.ref.WeakReference

class BlurredModalActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blurred_modal)

        val blurredBackgroundUri: Uri? = intent.getParcelableExtra("capture_uri")
        AsyncBlurredCaptureLoader(this).execute(blurredBackgroundUri)
    }

    fun onLoadBlurredCapture(result: Drawable?) {
        val view: View = findViewById(android.R.id.content)
        view.background = result
    }

    private class AsyncBlurredCaptureLoader(activity: BlurredModalActivity) : AsyncTask<Uri, Unit, Drawable>() {
        private val activity = WeakReference(activity)
        override fun doInBackground(vararg params: Uri?): Drawable? {
            val uri = params[0] ?: return null
            val capture = loadCapture(uri) ?: return null

            val renderScript = activity.get()?.let { RenderScript.create(it) } ?: return null
            val blurredCapture = RSBlurProcessor(renderScript).blur(capture, 25f, 3) ?: return null
            val resources = activity.get()?.resources ?: return null
            return BitmapDrawable(resources, blurredCapture)
        }

        override fun onPostExecute(result: Drawable?) {
            super.onPostExecute(result)
            activity.get()?.onLoadBlurredCapture(result)
        }

        private fun loadCapture(uri: Uri): Bitmap? {
            try {
                val inputStream = activity.get()?.contentResolver?.openInputStream(uri) ?: return null
                return BitmapFactory.decodeStream(inputStream)
            } catch (e: IOException) {
                e.printStackTrace()
                return null
            }
        }
    }

    class AsyncCapturer<T>(private val activity: T) where T : AppCompatActivity, T : AsyncCapturer.Capturable {
        interface Capturable {
            fun onCapture(result: Uri?)
        }

        fun capture() {
            val view = getView()
            val capture = getBitmap(view)
            AsyncCacher(activity).execute(capture)
        }

        private fun getView(): View = activity.window.findViewById(android.R.id.content)

        private fun getBitmap(view: View): Bitmap? {
            return try {
                val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
                val c = Canvas(bitmap)
                view.draw(c)
                bitmap
            } catch (e: IllegalArgumentException) {
                e.printStackTrace()
                null
            }
        }

        private class AsyncCacher<T>(activity: T) : AsyncTask<Bitmap, Unit, Uri>() where T : AppCompatActivity, T : Capturable {
            companion object {
                const val CACHE_FILE_NAME = "capture"
            }

            var weakActivity = WeakReference(activity)
            override fun doInBackground(vararg params: Bitmap?): Uri? {
                val bitmap = params[0] ?: return null
                val cacheDir = weakActivity.get()?.cacheDir ?: return null
                val file = File(cacheDir, CACHE_FILE_NAME)
                return try {
                    val outputStream = FileOutputStream(file)
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    outputStream.flush()
                    outputStream.close()
                    Uri.fromFile(file)
                } catch (e: IOException) {
                    e.printStackTrace()
                    null
                }
            }

            override fun onPostExecute(result: Uri?) {
                super.onPostExecute(result)
                weakActivity.get()?.onCapture(result)
            }
        }
    }

}
