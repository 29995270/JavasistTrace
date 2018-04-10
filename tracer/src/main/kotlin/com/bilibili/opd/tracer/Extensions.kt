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
import kotlin.collections.HashMap
import kotlin.collections.HashSet

/**
 * Created by wq on 2018/3/25.
 */
fun ClassPool.insertClassPath(input: Collection<TransformInput>): List<CtClass> {
    val classNames = ArrayList<String>()
    val allClass = ArrayList<CtClass>()
    input.forEach {
        println("-----------process dir---------------")
        it.directoryInputs.forEach {
            val dirPath = it.file.absolutePath
            insertClassPath(it.file.absolutePath)
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
            insertClassPath(it.file.absolutePath)
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
            allClass.add(get(it))
        } catch (e: NotFoundException) {
            println("class not found exception class name:  $it ")
        }
    }

    println("-----processed class count: ${allClass.size}")
    println("--------------------------")
    return allClass
}


//0,1,2,3,4,5,6,7,8,9,a,b,c,d,e,f,g,h,i,j,k....X,Y,Z
//10 + 26 + 26
fun Int.toObfuscate(): String {
    val result = this / 62
    val result2 = this % 62
    return if (result == 0) {
        when(this) {
            in 0..9 -> this.toString()
            in 10..35 -> ('a' + (this - 10)).toString()
            else -> ('A' + (this - 36)).toString()
        }
    } else {
        result.toObfuscate() + result2.toObfuscate()
    }
}

var start = 0
val map = HashMap<String, String>()

fun String.compress(): String {
    var result = this
    val splited = this.substring(0, this.length - 1).replace("(", ".").split(".")
    println(splited + "-----------")
    splited.filter { it.trim().isNotEmpty() }
            .forEach {
        var obf = map[it]
        if (obf == null) {
            start ++
            obf = start.toString()
            map[it] = obf
        }
        result = result.replace(it, obf, true)
    }
    //todo 压缩
    return result
}