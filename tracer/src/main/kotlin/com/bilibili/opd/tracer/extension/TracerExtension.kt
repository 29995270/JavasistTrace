package com.bilibili.opd.tracer.extension

/**
 * Created by wq on 2018/3/8.
 */
open class TracerExtension(
        var delegateInstanceMethodFullName: String = "",
        var tracePackageNames: List<String> = emptyList(),
        var excludePackageNames: List<String> = emptyList(),
        var excludeMethodSignature: Map<String, String> = emptyMap(),
        var enableObfuscate: Boolean = true
)