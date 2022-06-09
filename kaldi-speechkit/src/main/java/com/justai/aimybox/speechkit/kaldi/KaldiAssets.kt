package com.justai.aimybox.speechkit.kaldi

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URL
import java.nio.channels.Channels

class KaldiAssets private constructor(
    val directory: String
) {
    companion object {


        fun fromUrl(
            context: Context,
            lang: String,
            force: Boolean
        ): KaldiAssets {
            var lang = "ru";
            if (SDK_INT >= 24) {
                lang = context.resources.configuration.locales.get(0).language.lowercase()
            } else {
                lang = context.resources.configuration.locale.language.lowercase()
            }
            val url = URL("http://wtf-dev.ru/kaldi_stt/model_${lang}.zip")
            val outputFileName =
                "${context.getExternalFilesDir(null)?.absolutePath}/kaldi-assets-${lang}/"
            val dir = File(outputFileName + "ivector/")
            if (!dir.exists()) {
                val dir2 = File(outputFileName)
                dir2.mkdirs()
            } else {
                if (!force) return KaldiAssets(outputFileName)
            }
            url.openStream().use {
                Channels.newChannel(it).use { rbc ->
                    FileOutputStream(outputFileName).use { fos ->
                        fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
                    }
                }
            }
            val zip = File(outputFileName + "model_${lang}.zip")
            UnzipUtils.unzip(zip, outputFileName)
            return KaldiAssets(outputFileName)
        }

        @SuppressLint("NewApi")
        fun fromApkAssets(
            context: Context,
            assetsDirectory: String
        ): KaldiAssets {
            val directory =
                "${context.getExternalFilesDir(null)?.absolutePath}/kaldi-assets/"

            File(directory).takeIf { !it.exists() }?.also {
                copyAssetToExternalStorage(context, assetsDirectory, directory)
            }

            return KaldiAssets(directory)
        }

        private fun copyAssetToExternalStorage(
            context: Context,
            assetsDirectory: String,
            destination: String
        ) {
            val assetManager = context.assets
            val files = assetManager.list(assetsDirectory)

            files?.forEach { file ->
                try {
                    if (assetManager.list("$assetsDirectory/$file")!!.isNotEmpty()) {
                        copyAssetToExternalStorage(
                            context,
                            "$assetsDirectory/$file",
                            "$destination/$file"
                        )
                    } else {
                        copyAssetFileToExternalStorage(
                            assetManager.open("$assetsDirectory/$file"),
                            "$destination/$file"
                        )
                    }
                } catch (e: Throwable) {
                    L.e("Cannot copy $destination/$file", e)
                }
            }
        }

        private fun copyAssetFileToExternalStorage(
            stream: InputStream,
            destinationPath: String
        ) {

            stream.use { inputStream ->
                File(destinationPath)
                    .apply {
                        parentFile?.mkdirs()
                        createNewFile()
                    }
                    .outputStream()
                    .use { outputStream ->
                        inputStream.copyTo(outputStream, 1024)
                        outputStream.flush()
                    }
            }
        }
    }
}