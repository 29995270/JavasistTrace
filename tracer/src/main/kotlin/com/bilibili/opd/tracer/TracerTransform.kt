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

private const val MAP_PATH = "/outputs/tracer/methodsMap.json"

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
            val obfuscator = WordObfuscator()
            val insertCodeStrategy : InsertCodeStrategy = JavasistInsertImpl(tracer, obfuscator)
            println("----${jarFile.absolutePath}")
            insertCodeStrategy.insertCode(box, jarFile)
            obfuscator.outputMap(project.buildDir.path + MAP_PATH)
        }
    }
}