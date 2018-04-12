package com.bilibili.opd.tracer

import com.google.gson.Gson
import java.io.File
import java.io.FileWriter

class WordObfuscator {
    private val map = HashMap<String, String>()
    private var count = 0

    fun obfuscate(statement: String): String {
        val dividerMap = mutableMapOf<Int, Char>()
        var dividerIndex = 0
        statement.forEachIndexed { _, c ->
            when (c) {
                in listOf('.', '(', ')', '$', ',') -> dividerMap[dividerIndex++] = c
            }
        }

        var dividerConsumeCount = 0;
        return statement.split('.', '(', ')', '$', ',').mapIndexed { _, s ->
            map[s] ?: run{
                if (s.isEmpty()) {
                    s
                } else {
                    val toObfuscate = count.toObfuscate()
                    count ++
                    map[s] = toObfuscate
                    toObfuscate
                }
            }

        }.reduce { acc, s ->
            val d = dividerMap[dividerConsumeCount++]
            acc + d + s
        }.run {
            var result = this
            for (i in IntRange(dividerConsumeCount, dividerIndex - 1)) {
                result = result.plus(dividerMap[i])
            }
            result
        }
    }

    fun outputMap(path: String) {
        val file = File(path)
        if (!file.exists() && (!file.parentFile.mkdirs() || !file.createNewFile())) {

        }
        with(FileWriter(file)) {
            write(Gson().toJson(map))
            flush()
            close()
        }
    }
}