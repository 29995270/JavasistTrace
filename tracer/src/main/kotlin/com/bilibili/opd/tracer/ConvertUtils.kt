package com.bilibili.opd.tracer

import com.android.SdkConstants
import com.android.build.api.transform.TransformInput
import javassist.ClassPool
import javassist.CtClass
import javassist.NotFoundException
import org.apache.commons.io.FileUtils
import java.io.File
import java.util.*
import java.util.jar.JarFile
import java.util.regex.Matcher

/**
 * Created by wq on 2018/3/25.
 */
fun toCtClasses(input: Collection<TransformInput>, classPool: ClassPool) : List<CtClass> {
    val classNames = ArrayList<String>()
    val allClass = ArrayList<CtClass>()
    input.forEach {
        println("-----------process dir---------------")
        it.directoryInputs.forEach {
            val dirPath = it.file.absolutePath
            classPool.insertClassPath(it.file.absolutePath)
            FileUtils.listFiles(it.file, null, true).forEach {
                if (it.absolutePath.endsWith(SdkConstants.DOT_CLASS)) {
                    val className = it.absolutePath.substring(dirPath.length + 1, it.absolutePath.length - SdkConstants.DOT_CLASS.length).replace(File.separator, ".")
                    if (classNames.contains(className)) {
                        throw RuntimeException("You have duplicate classes with the same name : $className please remove duplicate classes ")
                    }
                    classNames.add(className)
                }
            }
        }
        println("-----------process jar---------------")
        it.jarInputs.forEach {
            classPool.insertClassPath(it.file.absolutePath)
            val jarFile = JarFile(it.file)
            val classes = jarFile.entries()
            for (libClass in classes) {
                val originName = libClass.name
                if (originName.endsWith(SdkConstants.DOT_CLASS)) {
                    val className = originName.substring(0, originName.length - SdkConstants.DOT_CLASS.length).replace("/", ".")
                    if (classNames.contains(className)) {
                        throw RuntimeException("You have duplicate classes with the same name : $className please remove duplicate classes ")
                    }
                    classNames.add(className)
                }
            }
        }
    }
    println("--------------------------")
    classNames.forEach {
        try {
            allClass.add(classPool.get(it))
        } catch (e: NotFoundException) {
            println("class not found exception class name:  $it ")
        }
    }

    println("-----processed class count: ${allClass.size}")
    println("--------------------------")
    return allClass
}