package com.bilibili.opd.tracer

import com.android.build.api.transform.Format
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.bilibili.opd.tracer.extension.TracerExtension
import javassist.ClassPool
import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.zip.GZIPOutputStream

/**
 * Created by wq on 2018/3/8.
 */
class TracerTransform(private val project: Project) : Transform() {
    override fun getName() = "Tracer"

    override fun getInputTypes() = TransformManager.CONTENT_CLASS

    override fun isIncremental() = false

    override fun getScopes() = TransformManager.SCOPE_FULL_PROJECT

    override fun transform(transformInvocation: TransformInvocation?) {
        super.transform(transformInvocation)
        println("start tracer transform")
        transformInvocation?.run {
            val tracer = project.extensions.getByType(TracerExtension::class.java)

            outputProvider.deleteAll()
            val jarFile = outputProvider.getContentLocation("main", outputTypes, scopes, Format.JAR)
            if (jarFile.parentFile.exists().not()) {
                jarFile.parentFile.mkdirs()
            }

            if (jarFile.exists()) {
                jarFile.delete()
            }

            val classPool = ClassPool()

            val androidExtension = project.extensions.getByType(AppExtension::class.java)
            androidExtension.bootClasspath.forEach {
                classPool.appendClassPath(it.absolutePath)
            }
            val box = classPool.insertClassPath(inputs)
            println("tracePackageNames: ${tracer.tracePackageNames}")
            println("excludePackageNames: ${tracer.excludePackageNames}")
            println("excludeMethodNames: ${tracer.excludeMethodSignature}")
            val insertCodeStrategy : InsertCodeStrategy = JavasistInsertImpl(tracer)
            println("----${jarFile.absolutePath}")
            insertCodeStrategy.insertCode(box, jarFile)
        }
    }

    private fun writeMap2File(map: Map<String, Int>, path: String) {
        val file = File(project.buildDir.path + path)
        if (file.exists().not() && (file.parentFile.mkdirs().not() || !file.createNewFile())) {

        }

        val fileOut = FileOutputStream(file)
        val byteOut = ByteArrayOutputStream()
        val objectOut = ObjectOutputStream(byteOut)
        objectOut.writeObject(map)

        val gzip = GZIPOutputStream(fileOut)
        gzip.write(byteOut.toByteArray())
        objectOut.close()
        gzip.flush()
        gzip.close()
        fileOut.flush()
        fileOut.close()

    }
}