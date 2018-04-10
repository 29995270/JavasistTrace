package com.bilibili.opd.tracer

import com.google.gson.Gson
import java.io.File
import java.io.FileOutputStream

class WordObfuscator {



    private val map = HashMap<String, String>()
    private var count = 0

    fun obfuscate(statement: String): String {
        val splited = statement.split('.', '(', ')', '&').filter { it.isNotEmpty() }
        return ""
    }

    fun outputMap(path: String) {
        with(FileOutputStream(File(path, "traceMap.txt"))) {
            write(Gson().toJson(map).toByteArray())
            flush()
            close()
        }
    }
}