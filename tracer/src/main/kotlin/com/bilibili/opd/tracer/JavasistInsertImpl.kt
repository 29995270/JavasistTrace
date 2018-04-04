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
class JavasistInsertImpl(tracerExtension: TracerExtension) : InsertCodeStrategy(tracerExtension) {
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
                            println("----code: $codeStatement")
                            declaredBehavior.insertBefore(codeStatement)
                        }
                    } catch (e: Exception) {
                        println("insert code fail $declaredBehavior")
                        e.printStackTrace()
                    }
                }
            }
            zipFile(ctClass.toBytecode(), outStream, ctClass.name.replace(".", "/") + ".class")
        }
        outStream.close()
    }

    private fun isTraceField(field: CtField) =
            field.annotations.any {
                "@com.bilibili.opd.tracer.core.annotation.TraceField" == it.toString()
            }

    private fun isPrimitiveType(ctClass: CtClass)= ctClass.isPrimitive

    private fun isIdField(field: CtField) = field.name.endsWith("id", true) && (field.type.name == "int" || field.type.name == "long")

    private fun isStringType(ctClass: CtClass) = ctClass.name == String::class.java.name

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
        val returnTypeName = returnType.name
        val args = getParamsStatements(ctClass, ctMethod)
        return """
                android.util.Log.d("AAA",
                    "t:" + Thread.currentThread().getName() + "_" + Thread.currentThread().getId() + "|" +
                    "${if (isStatic) "sm:" else "m:"}${ctMethod.longName.compress()}|" +
                    "r:$returnTypeName|" +
                    "p:$args");
                """.replace("""( \+ "")""".toRegex(), "")
    }

    private fun isQualifiedMethod(ctBehavior: CtBehavior): Boolean {


        if (ctBehavior.methodInfo.isMethod) {

            //方法过滤
            for ((methodName, signature) in tracerExtension.excludeMethodSignature) {
                println("ctBehavior.signature : ${ctBehavior.signature}")
                if (ctBehavior.signature == signature && ctBehavior.name == methodName) {
                    return false
                }
            }

            if (AccessFlag.isPackage(ctBehavior.modifiers)) {
                ctBehavior.modifiers = AccessFlag.setPublic(ctBehavior.modifiers)
            }
            val flag = isMethodWithExpression(ctBehavior as CtMethod)
            if (!flag) {
                return false
            }
        }

        return when {
            //skip short method(may inline by proguard)
            ctBehavior.methodInfo.codeAttribute.codeLength <= 8 -> false
            ctBehavior.methodInfo.isStaticInitializer -> false
            ctBehavior.methodInfo.isConstructor -> false
            // synthetic 方法暂时不aop 比如AsyncTask 会生成一些同名synthetic方法,对synthetic 以及private的方法也插入的代码，主要是针对lambda
            ctBehavior.modifiers and AccessFlag.SYNTHETIC != 0 && !AccessFlag.isPrivate(ctBehavior.modifiers) -> false
            ctBehavior.modifiers and AccessFlag.ABSTRACT != 0 -> false
            ctBehavior.modifiers and AccessFlag.NATIVE != 0 -> false
            ctBehavior.modifiers and AccessFlag.INTERFACE != 0 -> false
            else -> true
        }
    }

    private fun getParamsStatements(ctClass: CtClass, ctMethod: CtMethod): String {
        if (ctMethod.parameterTypes.isEmpty()) {
            return "null"
        }
        val stringBuilder = StringBuilder()
        return with(stringBuilder) {
            ctMethod.parameterTypes.forEachIndexed { index, paramType ->
                val paramStatement: StringBuilder = StringBuilder()

                if (isPrimitiveType(paramType)) {//基本类型，直接打印
                    paramStatement.append("""";" + $${index + 1}""")
                } else if (isCollectionType(paramType)) {//集合类型，直接打印长度
                    paramStatement.append("""";" + $${index + 1}.size()""")
                } else if (isStringType(paramType)) {//string 类型，直接打印长度
                    paramStatement.append("""";" + $${index + 1}.length""")
                } else if (isNeedInsertClass(paramType.name)) {
                    paramStatement.append(';')
                    for (field in paramType.declaredFields) {
                        if (!field.visibleFrom(ctClass)) continue
                        when {
                            isTraceField(field) -> {
                                println("print params ${index + 1}.${field.name}")
                                paramStatement.append(""",$${index + 1}.${field.name}=" + $${index + 1}.${field.name} + """")
                            }
                            isIdField(field) -> {
                                paramStatement.append(""",$${index + 1}.${field.name}=" + $${index + 1}.${field.name} + """")
                            }
                            isCollectionType(field.type) -> {
                                paramStatement.append("""" + ($${index + 1}.${field.name} == null ? ",$${index + 1}.${field.name}=null" : (",$${index + 1}.${field.name}.size=" + $${index + 1}.${field.name}.size())) + """")
                            }
                        }
                    }
                    if (paramStatement.length == 1) {
                        paramStatement.append('o')
                    }
                }
                if (paramStatement.startsWith(";")) paramStatement.deleteCharAt(0)
                if (paramStatement.startsWith(",")) paramStatement.deleteCharAt(0)
                if (index == 0) {
                    append("""" + ($${index + 1} == null ? "null" : "$paramStatement") + """")
                } else {
                    append("""" + ($${index + 1} == null ? ";null" : "$paramStatement") + """")
                }
            }
            toString()
        }
    }


    private var isCallMethod = false

    /**
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