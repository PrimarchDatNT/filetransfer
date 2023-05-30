package com.file.zip

import android.os.Environment
import com.genonbeta.android.framework.io.DocumentFile
import java.io.File

object ZipUtil {

    private val root = Environment.getExternalStorageDirectory().absolutePath.plus(File.separator).plus("ZipDemo")

    fun getUnzipDir() = root.plus(File.separator).plus("unzipFolder")

    fun getZipDir() = root.plus(File.separator).plus("zipFolder")

    fun getUnzipDocumentFile(): DocumentFile {
        val defaultPath = getUnzipDir()
        val defaultFolder = File(defaultPath)
        if (!defaultFolder.exists()) {
            println(defaultFolder.mkdirs())
        }
        return DocumentFile.fromFile(defaultFolder)
    }

    fun getZipDocumentFile(): DocumentFile {
        val defaultPath = getZipDir()
        val defaultFolder = File(defaultPath)
        if (!defaultFolder.exists()) {
            println(defaultFolder.mkdirs())
        }
        return DocumentFile.fromFile(defaultFolder)
    }

}