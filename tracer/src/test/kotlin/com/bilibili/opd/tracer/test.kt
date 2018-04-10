package com.bilibili.opd.tracer

import org.junit.Test

class PluginText {
    @Test
    fun testObfuscate() {
        assert(12.toObfuscate() == "b")
        assert(63.toObfuscate() == "00")
        assert(3845.toObfuscate() == "000")
        assert(3846.toObfuscate() == "001")
        assert(3907.toObfuscate() == "010")
    }
}
