package com.bilibili.opd.tracer

import com.bilibili.opd.tracer.extension.TracerExtension
import javassist.*
import javassist.bytecode.AccessFlag
import javassist.expr.*
import java.io.File
import java.io.FileOutputStream
import java.util.jar.JarOutputStream

/**
 * Created by wq on 2018/3/25.
 */
class JavasistInsertImpl(tracerExtension: TracerExtension, val obfuscator: Obfuscator) : InsertCodeStrategy(tracerExtension) {

    override fun insertCode(box: List<CtClass>, jarFile: File) {
        val outStream = JarOutputStream(FileOutputStream(jarFile))
        for (ctClass in box) {
            unlockAccessIfNeed(ctClass)
        }

        println("--------try insert code")
        for (ctClass in box) {
            if (isNeedInsertClass(ctClass.name)) {
//                ctClass.modifiers = AccessFlag.setPublic(ctClass.modifiers)
                if (ctClass.isInterface || ctClass.declaredMethods.isEmpty()) {
                    zipFile(ctClass.toBytecode(), outStream, ctClass.name.replace(".", "/") + ".class")
                    continue
                }
                for (declaredBehavior in ctClass.declaredBehaviors) {
                    if (!isQualifiedMethod(declaredBehavior)) continue
                    //insert trace into method
                    methodMap[declaredBehavior.longName] = insertMethodCount.incrementAndGet()
                    try {
                        if (declaredBehavior.methodInfo.isMethod) {
                            val codeStatement = getCodeStatement(ctClass, declaredBehavior as CtMethod)
//                            println("----inserted code: $codeStatement")
                            declaredBehavior.insertBefore(codeStatement)
                        }
                    } catch (e: Exception) {
                        println("insert code fail $declaredBehavior")
//                        e.printStackTrace()
                    }
                }
            }
            zipFile(ctClass.toBytecode(), outStream, ctClass.name.replace(".", "/") + ".class")
        }
        outStream.close()
    }

    private fun isTraceField(field: CtField) = false
//            try {
//                field.availableAnnotations.any {
//                    "@com.bilibili.opd.tracer.core.annotation.TraceField" == it.toString()
//                }
//            } catch (e: Exception) {
//                false
//            }

    private fun isPrimitiveType(ctClass: CtClass) = ctClass.isPrimitive

    private fun isIdField(field: CtField) = field.name.endsWith("id", true) && (field.type.name == "int" || field.type.name == "long")

    private fun isStringType(ctClass: CtClass) = ctClass.name == String::class.java.name

    private fun isArrayType(ctClass: CtClass) = ctClass.isArray

    private fun isCollectionType(ctClass: CtClass) =
            ctClass.name == List::class.java.name
                    || ctClass.name == Map::class.java.name
                    || (!ctClass.isInterface && ctClass.interfaces.any { it.name == List::class.java.name || it.name == Map::class.java.name })

    private fun unlockAccessIfNeed(ctClass: CtClass) {
        try {
            if (isNeedInsertClass(ctClass.name)) {
                for (field in ctClass.declaredFields) {

                    when {
                        isTraceField(field) -> {
                            println("${ctClass.name} has annotation TraceField")
                            field.modifiers = AccessFlag.setPublic(field.modifiers)
                        }
                        isIdField(field) -> field.modifiers = AccessFlag.setPublic(field.modifiers)
                        isCollectionType(field.type) -> field.modifiers = AccessFlag.setPublic(field.modifiers)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCodeStatement(ctClass: CtClass, ctMethod: CtMethod): String {
        println("process method: ${ctMethod.longName}")
        val isStatic = ctMethod.modifiers and AccessFlag.STATIC != 0
        val returnType = ctMethod.returnType
        val returnTypeName = returnType.name //todo
//        val args = getParamsStatement(ctClass, ctMethod)
        val argsExp = getParamsStatement(ctClass, ctMethod)
//        return """
//                com.bilibili.opd.tracer.core.LogRecorder.getInstance().enqueue(
//                    "${if (isStatic) "sm:" else "m:"}${ctMethod.longName.compress()}|" +
//                    "p:$args");
//                """.replace("""( \+ "")""".toRegex(), "")

        val methodIndex = obfuscator.methodIndex(ctMethod.longName)

        val replace : String
        if ("empty" == argsExp) {
            replace = """
            if (${tracerExtension.delegateInstanceHolderClass}.isEnable($methodIndex)) {
                ${tracerExtension.delegateInstanceHolderClass}.enqueue($methodIndex, $isStatic, "${if (tracerExtension.enableObfuscate) obfuscator.methodNameObfuscate(ctMethod.longName) else ctMethod.longName}", null);
            }
            """
        } else {
            replace = """
            if (${tracerExtension.delegateInstanceHolderClass}.isEnable($methodIndex)) {
                $argsExp
                ${tracerExtension.delegateInstanceHolderClass}.enqueue($methodIndex, $isStatic, "${if (tracerExtension.enableObfuscate) obfuscator.methodNameObfuscate(ctMethod.longName) else ctMethod.longName}", _trace_string);
            }
            """
        }
        return replace

//        return if (argsArrayList == null) {
//            """
//                com.bilibili.opd.tracer.core.LogRecorder.getInstance().enqueue($isStatic, "${ctMethod.longName.compress()}");
//                """.replace("""( \+ "")""".toRegex(), "")
//        } else if (argsArrayList.size == 1) {
//            """
//                com.bilibili.opd.tracer.core.LogRecorder.getInstance().enqueue($isStatic, "${ctMethod.longName.compress()}", ${argsArrayList[0]});
//                """.replace("""( \+ "")""".toRegex(), "")
//        } else {
//
//            val paramStatement = "new String[]{${argsArrayList.joinToString()}}"
//            """
//                com.bilibili.opd.tracer.core.LogRecorder.getInstance().enqueue($isStatic, "${ctMethod.longName.compress()}", $paramStatement);
//                """.replace("""( \+ "")""".toRegex(), "")
//        }

    }

    private fun isQualifiedMethod(ctBehavior: CtBehavior): Boolean {


        if (ctBehavior.methodInfo.isMethod) {

            //方法过滤
            for ((methodName, signature) in tracerExtension.excludeMethodSignature) {
                if (ctBehavior.signature == signature && methodName.toRegex().matches(ctBehavior.longName)) {
                    println("exclude by match rule: ${ctBehavior.longName}")
                    return false
                }
            }

            if (AccessFlag.isPackage(ctBehavior.modifiers)) {
                ctBehavior.modifiers = AccessFlag.setPublic(ctBehavior.modifiers)
            }
            val flag = isMethodWithExpression(ctBehavior as CtMethod)
            if (!flag) {
                println("exclude by empty body: ${ctBehavior.longName}")
                return false
            }
        }

        return when {
        //skip short method(may inline by proguard)
            ctBehavior.methodInfo.codeAttribute.codeLength <= 8 -> {
                println("exclude by short body: ${ctBehavior.longName}")
                false
            }
            ctBehavior.methodInfo.isStaticInitializer -> {
                println("exclude by static init: ${ctBehavior.longName}")
                false
            }
            ctBehavior.methodInfo.isConstructor -> {
                println("exclude by constructor: ${ctBehavior.longName}")
                false
            }
        // synthetic 方法暂时不aop 比如AsyncTask 会生成一些同名synthetic方法,对synthetic 以及private的方法也插入的代码，主要是针对lambda
            ctBehavior.modifiers and AccessFlag.SYNTHETIC != 0 && !AccessFlag.isPrivate(ctBehavior.modifiers) -> {
                println("exclude by SYNTH !priva: ${ctBehavior.longName}")
                false
            }
            ctBehavior.modifiers and AccessFlag.ABSTRACT != 0 -> {
                println("exclude by abstract: ${ctBehavior.longName}")
                false
            }
            ctBehavior.modifiers and AccessFlag.NATIVE != 0 -> {
                println("exclude by native method: ${ctBehavior.longName}")
                false
            }
            ctBehavior.modifiers and AccessFlag.INTERFACE != 0 -> {
                println("exclude by interface: ${ctBehavior.longName}")
                false
            }
            else -> true
        }
    }

    private fun getParamsStatement(ctClass: CtClass, ctMethod: CtMethod): String {
        if (ctMethod.parameterTypes.isEmpty()) {
            return "empty"
        }
        val stringBuilder = StringBuilder()
        return with(stringBuilder) {
            append("StringBuilder _trace_builder = new StringBuilder();")
            var handleParamsCount = 0
            ctMethod.parameterTypes.forEachIndexed { index, paramType ->
                if (isPrimitiveType(paramType)) {//基本类型，直接打印
                    handleParamsCount++
                    if (index != 0) {
                        append("""_trace_builder.append(";")""")
                        append(""".append($${index + 1});""")
                    } else {
                        append("""_trace_builder.append($${index + 1});""")
                    }
                } else if (isCollectionType(paramType)) {//集合类型，直接打印长度
                    handleParamsCount++
                    if (index != 0) {
                        append("""_trace_builder.append(";")""")
                        append(""".append($${index + 1} == null ? "null" : String.valueOf($${index + 1}.size()));""")
                    } else {
                        append("""_trace_builder.append($${index + 1} == null ? "null" : String.valueOf($${index + 1}.size()));""")
                    }
                } else if (isStringType(paramType)) {//string 类型，直接打印长度
                    handleParamsCount++
                    if (index != 0) {
                        append("""_trace_builder.append(";")""")
                        append(""".append($${index + 1} == null ? "null" : String.valueOf($${index + 1}.length()));""")
                    } else {
                        append("""_trace_builder.append($${index + 1} == null ? "null" : String.valueOf($${index + 1}.length()));""")
                    }
                } else if (isStringType(paramType)) {//array 类型，直接打印长度
                    handleParamsCount++
                    if (index != 0) {
                        append("""_trace_builder.append(";")""")
                        append(""".append($${index + 1} == null ? "null" : String.valueOf($${index + 1}.length));""")
                    } else {
                        append("""_trace_builder.append($${index + 1} == null ? "null" : String.valueOf($${index + 1}.length));""")
                    }
                } else {
                    handleParamsCount++
                    if (index != 0) append("""_trace_builder.append(";");""")
                    append("""
                        if ($${index + 1} == null) {
                            _trace_builder.append("null");
                        } else {
                    """)
                    var tracedFieldCount = 0
                    if (isNeedInsertClass(paramType.name)) {
                        for (field in paramType.declaredFields) {
                            if (!field.visibleFrom(ctClass) || field.modifiers and AccessFlag.STATIC != 0) continue
                            when {
                                isTraceField(field) -> {
                                    if (tracedFieldCount != 0) {
                                        append("""_trace_builder.append(",")""")
                                        append(""".append("$${index + 1}.${field.name}=")""")
                                        append(""".append($${index + 1}.${field.name});""")
                                    } else {
                                        append("""_trace_builder.append("$${index + 1}.${field.name}=")""")
                                        append(""".append($${index + 1}.${field.name});""")
                                    }
                                    tracedFieldCount++
                                }
                                isStringType(field.type) -> {
                                    if (tracedFieldCount != 0) {
                                        append("""_trace_builder.append(",")""")
                                        append(""".append("$${index + 1}.${field.name}=");""")
                                    } else {
                                        append("""_trace_builder.append("$${index + 1}.${field.name}=");""")
                                    }
                                    append("""
                                        if ($${index + 1}.${field.name} == null) {
                                            _trace_builder.append("null");
                                        } else {
                                            _trace_builder.append($${index + 1}.${field.name}.length());
                                        }
                                    """)
                                    tracedFieldCount++
                                }
                                isArrayType(field.type) -> {
                                    if (tracedFieldCount != 0) {
                                        append("""_trace_builder.append(",")""")
                                        append(""".append("$${index + 1}.${field.name}=");""")
                                    } else {
                                        append("""_trace_builder.append("$${index + 1}.${field.name}=");""")
                                    }
                                    append("""
                                        if ($${index + 1}.${field.name} == null) {
                                            _trace_builder.append("null");
                                        } else {
                                            _trace_builder.append($${index + 1}.${field.name}.length);
                                        }
                                    """)
                                    tracedFieldCount++
                                }
                                isPrimitiveType(field.type) -> {
                                    if (tracedFieldCount != 0) {
                                        append("""_trace_builder.append(",")""")
                                        append(""".append("$${index + 1}.${field.name}=")""")
                                    } else {
                                        append("""_trace_builder.append("$${index + 1}.${field.name}=")""")
                                    }
                                    append(""".append($${index + 1}.${field.name});""")
                                    tracedFieldCount++
                                }
                                isCollectionType(field.type) -> {
                                    if (tracedFieldCount != 0) {
                                        append("""_trace_builder.append(",")""")
                                        append(""".append("$${index + 1}.${field.name}=");""")
                                    } else {
                                        append("""_trace_builder.append("$${index + 1}.${field.name}=");""")
                                    }
                                    append("""
                                        if ($${index + 1}.${field.name} == null) {
                                            _trace_builder.append("null");
                                        } else {
                                            _trace_builder.append($${index + 1}.${field.name}.size());
                                        }
                                    """)
                                    tracedFieldCount++
                                }
                            }
                        }
                    }
                    if (tracedFieldCount == 0) {
                        append("""
                                _trace_builder.append("obj");
                            }
                        """)
                    } else {
                        append("}")
                    }
                }
            }
            append("String _trace_string = _trace_builder.toString();")
            toString()
        }
    }

    private fun getParamsStatements(ctClass: CtClass, ctMethod: CtMethod): List<String>? {
        if (ctMethod.parameterTypes.isEmpty()) {
            return null
        }
        val statements = arrayListOf<String>()

        ctMethod.parameterTypes.forEachIndexed { index, paramType ->

            if (isPrimitiveType(paramType)) { //基本类型
                statements.add(""""$${index + 1}:" + $${index + 1}""")
            } else if (isCollectionType(paramType)) {//集合类型，直接打印长度
                statements.add("""$${index + 1} == null ? "$${index + 1}:null" : "$${index + 1}.size:" + $${index + 1}.size()""")
            } else if (isStringType(paramType)) {//string 类型，直接打印长度
                statements.add("""$${index + 1} == null ? "$${index + 1}:null" : "$${index + 1}.len:" + $${index + 1}.length()""")
            } else {
                if (isNeedInsertClass(paramType.name)) {
                    statements.add("""$${index + 1} == null ? "$${index + 1}:null" : "$${index + 1}:o"""")
                    for (field in paramType.declaredFields) {
                        if (!field.visibleFrom(ctClass) || field.modifiers and AccessFlag.STATIC != 0) continue
                        when {
                            isTraceField(field) -> {
                                statements.add(""""$${index + 1}.${field.name}:" + $${index + 1}.${field.name}""")
                            }
                            isStringType(field.type) -> {
                                statements.add("""$${index + 1}.${field.name} == null ? "$${index + 1}.${field.name}:null" : "$${index + 1}..${field.name}.len:" + $${index + 1}.${field.name}.length()""")
                            }
//                            isIdField(field) -> {
//                                statements.add(""""$${index + 1}.${field.name}:" + $${index + 1}.${field.name}""")
//                            }
                            isPrimitiveType(field.type) -> {
                                statements.add(""""$${index + 1}.${field.name}:" + $${index + 1}.${field.name}""")
                            }
                            isCollectionType(field.type) -> {
                                statements.add("""$${index + 1}.${field.name} == null ? "$${index + 1}.${field.name}:null" : "$${index + 1}.${field.name}.size:" + $${index + 1}.${field.name}.size()""")
                            }
                        }
                    }
                } else {
                    statements.add("""$${index + 1} == null ? "$${index + 1}:null" : "$${index + 1}:o"""")
                }
            }
        }
        return statements
    }


    private var isCallMethod = false

    /**
     * todo 可以考虑把Log Blog 的调用都替换成我们的log
     * 判断是否有方法call
     *
     * @return 是否插桩
     */
    @Throws(CannotCompileException::class)
    private fun isMethodWithExpression(ctMethod: CtMethod?): Boolean {
        isCallMethod = false
        if (ctMethod == null) {
            return false
        }

        ctMethod.instrument(object : ExprEditor() {
            /**
             * Edits a <tt>new</tt> expression (overridable).
             * The default implementation performs nothing.
             *
             * @param e the <tt>new</tt> expression creating an object.
             */
            //            public void edit(NewExpr e) throws CannotCompileException { isCallMethod = true; }

            /**
             * Edits an expression for array creation (overridable).
             * The default implementation performs nothing.
             *
             * @param a the <tt>new</tt> expression for creating an array.
             * @throws CannotCompileException
             */
            @Throws(CannotCompileException::class)
            override fun edit(a: NewArray?) {
                isCallMethod = true
            }

            /**
             * Edits a method call (overridable).
             *
             * The default implementation performs nothing.
             */
            @Throws(CannotCompileException::class)
            override fun edit(m: MethodCall?) {
                isCallMethod = true
            }

            /**
             * Edits a constructor call (overridable).
             * The constructor call is either
             * `super()` or `this()`
             * included in a constructor body.
             *
             * The default implementation performs nothing.
             *
             * @see .edit
             */
            @Throws(CannotCompileException::class)
            override fun edit(c: ConstructorCall?) {
                isCallMethod = true
            }

            /**
             * Edits an instanceof expression (overridable).
             * The default implementation performs nothing.
             */
            @Throws(CannotCompileException::class)
            override fun edit(i: Instanceof?) {
                isCallMethod = true
            }

            /**
             * Edits an expression for explicit type casting (overridable).
             * The default implementation performs nothing.
             */
            @Throws(CannotCompileException::class)
            override fun edit(c: Cast?) {
                isCallMethod = true
            }

            /**
             * Edits a catch clause (overridable).
             * The default implementation performs nothing.
             */
            @Throws(CannotCompileException::class)
            override fun edit(h: Handler?) {
                isCallMethod = true
            }
        })
        return isCallMethod
    }

}