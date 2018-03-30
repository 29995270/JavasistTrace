package com.bilibili.opd.tracer

import com.bilibili.opd.tracer.extension.TracerExtension
import javassist.CtClass
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.collections.HashMap

/**
 * Created by wq on 2018/3/25.
 */
abstract class InsertCodeStrategy(val tracerExtension: TracerExtension) {
    val insertMethodCount = AtomicInteger(0)

    val methodMap = HashMap<String, Int>()

    abstract fun insertCode(box: List<CtClass>, jarFile: File)

    open fun isNeedInsertClass(className: String) =
            if (tracerExtension.excludePackageNames.any { className.startsWith(it) }) false else tracerExtension.tracePackageNames.any { className.startsWith(it) }

    open fun zipFile(classBytesArray: ByteArray, zos: ZipOutputStream, entryName: String) {
        try {
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            zos.write(classBytesArray, 0, classBytesArray.size)
            zos.closeEntry()
            zos.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }
}