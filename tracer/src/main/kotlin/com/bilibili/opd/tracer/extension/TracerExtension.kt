package com.bilibili.opd.tracer.extension

/**
 * Created by wq on 2018/3/8.
 */
open class TracerExtension(
        var delegateInstanceHolderClass: String = "",
        var tracePackageNames: List<String> = emptyList(),
        var excludePackageNames: List<String> = emptyList(),
        var excludeMethodSignature: Map<String, String> = emptyMap(),
        var enableObfuscate: Boolean = true
)