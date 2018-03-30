package com.bilibili.opd.tracer

import com.android.build.gradle.BaseExtension
import com.bilibili.opd.tracer.extension.TracerExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by wq on 2018/3/8.
 */
open class TracerPlugin : Plugin<Project> {
    override fun apply(project: Project?) {
        project?.run {
            extensions.create("tracer", TracerExtension::class.java)
            val tracerTransform = TracerTransform(this)
            val androidExt = extensions.getByType(BaseExtension::class.java)
            androidExt.registerTransform(tracerTransform)
        }
    }
}