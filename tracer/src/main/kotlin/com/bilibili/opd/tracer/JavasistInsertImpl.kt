package com.bilibili.opd.tracer

import com.bilibili.opd.tracer.extension.TracerExtension
import javassist.CannotCompileException
import javassist.CtBehavior
import javassist.CtClass
import javassist.CtMethod
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
                            declaredBehavior.insertBefore(getCodeStatement(declaredBehavior as CtMethod))
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

    private fun unlockAccessIfNeed(ctClass: CtClass) {
        try {
            if (isNeedInsertClass(ctClass.name)) {
                for (field in ctClass.declaredFields) {
                    for (annotation in field.annotations) {
                        if ("@com.bilibili.opd.tracer.core.annotation.TraceField" == annotation.toString()) {
                            println("${ctClass.name} has annotation TraceField")
                            field.modifiers = AccessFlag.setPublic(field.modifiers)
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCodeStatement(ctMethod: CtMethod): String {
        println("process method: ${ctMethod.longName}")
        val isStatic = ctMethod.modifiers and AccessFlag.STATIC != 0
        val returnType = ctMethod.returnType
        val returnTypeName = returnType.name
        val args = getParamsStatements(ctMethod)
        return """
                android.util.Log.d("AAA",
                    "t:" + Thread.currentThread().getName() + "_" + Thread.currentThread().getId() + "|" +
                    "${if (isStatic) "sm:" else "m:"}${ctMethod.longName}|" +
                    "r:$returnTypeName|" +
                    "args:$args");
                """
    }

    private fun isQualifiedMethod(ctBehavior: CtBehavior): Boolean {
        //skip short method(may inline by proguard)
        if (ctBehavior.methodInfo.codeAttribute.codeLength <= 8) {
            return false
        }

        if (ctBehavior.methodInfo.isStaticInitializer) {
            return false
        }

        // synthetic 方法暂时不aop 比如AsyncTask 会生成一些同名synthetic方法,对synthetic 以及private的方法也插入的代码，主要是针对lambda
        if (ctBehavior.modifiers and AccessFlag.SYNTHETIC != 0 && !AccessFlag.isPrivate(ctBehavior.modifiers)) {
            return false
        }
        if (ctBehavior.methodInfo.isConstructor) {
            return false
        }

        if (ctBehavior.modifiers and AccessFlag.ABSTRACT != 0) {
            return false
        }
        if (ctBehavior.modifiers and AccessFlag.NATIVE != 0) {
            return false
        }
        if (ctBehavior.modifiers and AccessFlag.INTERFACE != 0) {
            return false
        }

        if (ctBehavior.methodInfo.isMethod) {
            if (AccessFlag.isPackage(ctBehavior.modifiers)) {
                ctBehavior.modifiers = AccessFlag.setPublic(ctBehavior.modifiers)
            }
            val flag = isMethodWithExpression(ctBehavior as CtMethod)
            if (!flag) {
                return false
            }
        }
        //方法过滤
        for ((methodName, signature) in tracerExtension.excludeMethodSignature) {
            println("ctBehavior.signature : ${ctBehavior.signature}")
            if (ctBehavior.signature == signature && ctBehavior.name == methodName) {
                return false
            }
        }
        return true
    }

    private fun getParamsStatements(ctMethod: CtMethod): String {
        if (ctMethod.parameterTypes.isEmpty()) {
            return " null "
        }
        val stringBuilder = StringBuilder()
        return with(stringBuilder) {
            var countOfHandled = 0
            ctMethod.parameterTypes.forEachIndexed { index, paramType ->
                for (field in paramType.declaredFields) {
                    for (annotation in field.annotations) {
                        if ("@com.bilibili.opd.tracer.core.annotation.TraceField" == annotation.toString()) {
                            println("${paramType.name} has annotation TraceField")
                            try {
                                field.modifiers = AccessFlag.setPublic(field.modifiers)
                            } catch (e: Exception) {
                                println("open access fail $e")
                            }
                            println("print params ${index + 1}.${field.name}")
                            append(""",$${index + 1}.${field.name}=" + $${index + 1}.${field.name} + """")
                            countOfHandled ++
                            break
                        }
                    }
                }
            }



            for (parameterType in ctMethod.parameterTypes) {

            }
            if (startsWith(",")) deleteCharAt(0)
            if (countOfHandled == 0) return "null"
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