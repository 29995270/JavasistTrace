package com.bilibili.opd.tracer

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.bilibili.opd.tracer.extension.TracerExtension
import javassist.ClassPool
import org.gradle.api.Project

/**
 * Created by wq on 2018/3/8.
 */

private const val MAP_PATH = "/outputs/tracer/methodsMap.json"
private const val LIST_PATH = "/outputs/tracer/methodList.json"

class TracerTransform(private val project: Project) : Transform() {


    override fun getName() = "Tracer"

    override fun getInputTypes() = TransformManager.CONTENT_CLASS

    override fun isIncremental() = false

    //https://stackoverflow.com/questions/38512487/definitions-for-gradle-transform-api-scopes
    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        project.plugins.any {
            when (it) {
                is LibraryPlugin -> true
                else -> false
            }
        }.run {
            return if (this) {
                mutableSetOf(QualifiedContent.Scope.PROJECT)
            } else {
                TransformManager.SCOPE_FULL_PROJECT
            }
        }
    }

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

            var androidExtension: BaseExtension? = project.extensions.findByType(LibraryExtension::class.java)
            androidExtension = androidExtension ?: project.extensions.findByType(AppExtension::class.java)
            androidExtension!!.bootClasspath.forEach {
                classPool.appendClassPath(it.absolutePath)
            }
            val box = classPool.insertClassPath(inputs)
            println("delegateInstanceHolderClass: ${tracer.delegateInstanceHolderClass}")
            println("tracePackageNames: ${tracer.tracePackageNames}")
            println("excludePackageNames: ${tracer.excludePackageNames}")
            println("excludeMethodNames: ${tracer.excludeMethodSignature}")
            val obfuscator = Obfuscator()
            val insertCodeStrategy : InsertCodeStrategy = JavasistInsertImpl(tracer, obfuscator)
            println("----${jarFile.absolutePath}")
            insertCodeStrategy.insertCode(box, jarFile)
            obfuscator.outputMap(project.buildDir.path + MAP_PATH)
            obfuscator.outputIndex(project.buildDir.path + LIST_PATH)
        }
    }
}