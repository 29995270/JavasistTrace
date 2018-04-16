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

    @Test
    fun obfuscateMethodName() {
        val wordObfuscator = Obfuscator()

        val s = wordObfuscator.methodNameObfuscate("com.bilibili.music.MainActivity.onCreate(android.binder.Bunder\$abc)")
        val s2 = wordObfuscator.methodNameObfuscate("com.bilibili.music.MainActivity.onCreate()")
        val s3 = wordObfuscator.methodNameObfuscate("com.bilibili.music.MainActivity.onCreate(int)")
        val s4 = wordObfuscator.methodNameObfuscate("com.bilibili.music.app.domain.song.SongDetailRepository\$\$Lambda\$1.call(java.lang.Object)")
        print(s)
    }

    @Test
    fun testRegex() {
        if (".*Adapter[.]getItemCount\\(\\)".toRegex().matches("com.bilibili.music.app.ui.menus.menulist.MenusFragment\$MenusAdapter.getItemCount()")) {
        }
    }
}
