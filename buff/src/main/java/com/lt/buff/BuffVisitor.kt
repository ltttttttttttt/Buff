package com.lt.buff

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.google.devtools.ksp.symbol.Nullability

/**
 * creator: lt  2022/10/20  lt.dygzs@qq.com
 * effect : 访问并处理相应符号
 * warning:
 */
internal class BuffVisitor(private val environment: SymbolProcessorEnvironment) : KSVisitorVoid() {
    private val suffix = "WithBuff"
    private val buffName = Buff::class.simpleName

    /**
     * 访问class的声明
     */
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        //获取class信息并创建kt文件
        val packageName = classDeclaration.containingFile!!.packageName.asString()
        val originalClassName = classDeclaration.simpleName.asString()
        val className = "$originalClassName$suffix"
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
        val functionFields = mutableListOf<FunctionFieldsInfo>()
        //遍历构造内的字段
        classDeclaration.primaryConstructor?.parameters?.forEach {
            val name = it.name?.getShortName() ?: ""
            val ksType = it.type.resolve()
            val isBuffBean =
                ksType.declaration.annotations.toList()
                    .find { it.shortName.getShortName() == buffName } != null
            val type = ksType.declaration.simpleName.getShortName()
            val nullable = if (ksType.nullability == Nullability.NULLABLE) "?" else ""
            //写入构造内的普通字段
            file.appendText("    ${if (it.isVal) "val" else "var"} $name: ${if (isBuffBean) "$type$suffix$nullable" else "$type$nullable"},\n")
            functionFields.add(FunctionFieldsInfo(name, true, isBuffBean))
        }
        //遍历所有字段
        classDeclaration.getAllProperties().forEach {
            //只解析类成员
            if (it.parent is KSClassDeclaration) {
                val fieldName = it.simpleName.getShortName()
                if (!it.isMutable)
                    throw RuntimeException("$originalClassName.$fieldName: It is meaningless for the field of val to change to the MutableState<T>")
                val ksType = it.type.resolve()
                val isBuffBean =
                    ksType.declaration.annotations.toList()
                        .find { it.shortName.getShortName() == buffName } != null
                val typeName = ksType.declaration.simpleName.getShortName()
                val nullable = if (ksType.nullability == Nullability.NULLABLE) "?" else ""
                val stateFieldName = "_${fieldName}_state"
                val buffType = if (isBuffBean) "$typeName$suffix$nullable" else "$typeName$nullable"
                //写入构造内的state字段
                file.appendText("    @kotlinx.serialization.Transient val $stateFieldName: MutableState<$buffType> = null!!,\n")
                classFields.add(
                    "    var $fieldName: $buffType = $stateFieldName.value\n" +
                            "        get() {\n" +
                            "            $stateFieldName.value = field\n" +
                            "            return $stateFieldName.value\n" +
                            "        }\n" +
                            "        set(value) {\n" +
                            "            field = value\n" +
                            "            $stateFieldName.value = value\n" +
                            "        }\n"
                )
                functionFields.add(FunctionFieldsInfo(fieldName, false, isBuffBean))
            }
        }
        file.appendText(") {\n")
        //写入非构造内的字段
        classFields.forEach(file::appendText)
        //写入removeBuff
        file.appendText(
            "\n    fun removeBuff(): $originalClassName =\n" +
                    "        $originalClassName(${
                        functionFields.filter { it.isInTheConstructor }
                            .map {
                                if (it.isBuffBean)
                                    "${it.fieldName}?.removeBuff()"
                                else
                                    it.fieldName
                            }
                            .joinToString()
                    }).also {\n"
        )
        functionFields.filter { !it.isInTheConstructor }.forEach {
            file.appendText(
                "            it.${it.fieldName} = ${
                    if (it.isBuffBean)
                        "${it.fieldName}?.removeBuff()"
                    else
                        it.fieldName
                }\n"
            )
        }
        file.appendText("        }\n")
        file.appendText("}\n\n")
        //写入addBuff
        file.appendText(
            "fun $originalClassName.addBuff(): $className =\n" +
                    "    $className(\n"
        )
        functionFields.forEach {
            if (it.isInTheConstructor)
                file.appendText(
                    "        ${
                        if (it.isBuffBean)
                            "${it.fieldName}?.addBuff()"
                        else
                            it.fieldName
                    },\n"
                )
            else
                file.appendText(
                    "        mutableStateOf(${
                        if (it.isBuffBean)
                            "${it.fieldName}?.addBuff()"
                        else
                            it.fieldName
                    }),\n"
                )
        }
        file.appendText("    )\n\n")
        file.close()
    }
}