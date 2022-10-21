package com.lt.buff

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid

/**
 * creator: lt  2022/10/20  lt.dygzs@qq.com
 * effect : 访问并处理相应符号
 * warning:
 */
internal class BuffVisitor(private val environment: SymbolProcessorEnvironment) : KSVisitorVoid() {
    /**
     * 访问class的声明
     */
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        //获取class信息并创建kt文件
        val packageName = classDeclaration.containingFile!!.packageName.asString()
        val originalClassName = classDeclaration.simpleName.asString()
        val className = "${originalClassName}WithBuff"
        val file = environment.codeGenerator.createNewFile(
            Dependencies(
                true,
                classDeclaration.containingFile!!
            ), packageName, className
        )
        //写入头文件
        file.appendText("package $packageName\n\n")
        file.appendText(
            "import androidx.compose.runtime.MutableState\n" +
                    "import androidx.compose.runtime.mutableStateOf\n\n"
        )
        file.appendText(
            "@kotlinx.serialization.Serializable\n" +// TODO by lt 2022/10/21 17:23 后续改为多种json解析方式支持
                    "class $className(\n"
        )
        //类内的字段(非构造内的)
        val classFields = mutableListOf<String>()
        //addBuff和removeBuff函数用到的字段
        val functionFields = mutableListOf<Pair<String, Boolean/*isInTheConstructor*/>>()
        //遍历构造内的字段
        classDeclaration.primaryConstructor?.parameters?.forEach {
            val name = it.name?.getShortName() ?: ""
            file.appendText("    ${if (it.isVal) "val" else "var"} $name: ${it.type.resolve()},\n")
            functionFields.add(name to true)
        }
        //遍历所有字段
        classDeclaration.getAllProperties().forEach {
            //只解析类成员
            if (it.parent is KSClassDeclaration) {
                val fieldName = it.simpleName.getShortName()
                val stateFieldName = "_${fieldName}_state"
                file.appendText("    @kotlinx.serialization.Transient val $stateFieldName: MutableState<${it.type.resolve()}> = null!!,\n")
                classFields.add(
                    "    var $fieldName: ${it.type.resolve()} = $stateFieldName.value\n" +
                            "        get() {\n" +
                            "            $stateFieldName.value = field\n" +
                            "            return $stateFieldName.value\n" +
                            "        }\n" +
                            "        set(value) {\n" +
                            "            field = value\n" +
                            "            $stateFieldName.value = value\n" +
                            "        }\n"
                )
                functionFields.add(fieldName to false)
            }
        }
        file.appendText(") {\n")
        //写入非构造内的字段
        classFields.forEach(file::appendText)
        //写入removeBuff
        file.appendText(
            "\n    fun removeBuff(): $originalClassName =\n" +
                    "        $originalClassName(${
                        functionFields.filter { it.second }.map { it.first }.joinToString()
                    }).also {\n"
        )
        functionFields.filter { !it.second }.forEach {
            file.appendText("            it.${it.first} = ${it.first}\n")
        }
        file.appendText("        }\n")
        file.appendText("}\n\n")
        //写入addBuff
        file.appendText(
            "fun $originalClassName.addBuff(): $className =\n" +
                    "    $className(\n"
        )
        functionFields.forEach {
            if (it.second)
                file.appendText("        ${it.first},\n")
            else
                file.appendText("        mutableStateOf(${it.first}),\n")
        }
        file.appendText(
            "    )\n\n" /*+
                    "operator fun $originalClassName.invoke(): $className = addBuff()"*/
        )
        file.close()
    }
}