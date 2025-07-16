package com.lt.buff.provider

import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.lt.buff.getAnnotationFullClassName
import com.lt.buff.getBuffKSTypeInfo
import com.lt.buff.options.CustomOptionsInfo
import com.lt.buff.options.FunctionFieldsInfo
import com.lt.buff.options.KspOptions
import com.lt.ksp.appendText

/**
 * creator: lt  2022/10/20  lt.dygzs@qq.com
 * effect : 访问并处理相应符号
 * warning:
 */
internal class BuffVisitor(private val environment: SymbolProcessorEnvironment) : KSVisitorVoid() {
    private val options = KspOptions(environment)

    /**
     * 访问class的声明
     */
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        //获取class信息并创建kt文件
        val packageName = classDeclaration.packageName.asString()
        val originalClassName = classDeclaration.simpleName.asString()
        val fullName = classDeclaration.qualifiedName?.asString()
            ?: (classDeclaration.packageName.asString() + classDeclaration.simpleName.asString())
        val className = "$originalClassName${KspOptions.suffix}"
        val haveStable = classDeclaration.annotations.find {
            getAnnotationFullClassName(it) == "androidx.compose.runtime.Stable"
        } != null
        val haveImmutable = classDeclaration.annotations.find {
            getAnnotationFullClassName(it) == "androidx.compose.runtime.Immutable"
        } != null

        val sources = classDeclaration.containingFile?.let { arrayOf(it) } ?: run { arrayOf() }
        val file = environment.codeGenerator.createNewFile(
            Dependencies(
                true,
                *sources,
            ), packageName, className
        )
        //写入头文件
        file.appendText("package $packageName\n\n")
        file.appendText(
            "import androidx.compose.runtime.MutableState\n" +
                    "import androidx.compose.runtime.mutableStateListOf\n" +
                    "import androidx.compose.runtime.mutableStateOf\n" +
                    "import androidx.compose.runtime.Stable\n" +
                    "import androidx.compose.runtime.Immutable\n" +
                    "import androidx.compose.runtime.snapshots.SnapshotStateList\n" +
                    "import androidx.compose.runtime.toMutableStateList\n\n"
        )
        if (haveStable)
            file.appendText("@Stable\n")
        if (haveImmutable)
            file.appendText("@Immutable\n")
        file.appendText(
            "${options.getClassSerializeAnnotation()}\n" +
                    "class $className @Deprecated(\"Do not directly call the constructor, instead use addBuff()\") constructor(\n"
        )
        //类内的字段(非构造内的)
        val classFields = mutableListOf<String>()
        //构造内的字段
        val constructorFields = mutableListOf<String>()
        //addBuff和removeBuff函数用到的字段
        val functionFields = mutableListOf<FunctionFieldsInfo>()
        //遍历构造内的字段
        classDeclaration.primaryConstructor?.parameters?.forEach {
            val name = it.name?.getShortName() ?: ""
            constructorFields.add(name)
            val ksTypeInfo = getBuffKSTypeInfo(it.type, classDeclaration)
            //写入构造内的普通字段
            file.appendText("    ${if (it.isVal) "val" else "var"} $name: ${ksTypeInfo.finallyTypeName},\n")
            functionFields.add(
                FunctionFieldsInfo(
                    name,
                    true,
                    ksTypeInfo.isBuffBean,
                    ksTypeInfo.nullable,
                    ksTypeInfo.isList,
                    ksTypeInfo.typeString,
                )
            )
        }
        //遍历所有字段
        classDeclaration.getAllProperties().forEach {
            //只解析类成员
            val fieldName = it.simpleName.getShortName()
            if (fieldName !in constructorFields) {
                if (!it.isMutable)
                    throw RuntimeException("$originalClassName.$fieldName: It is meaningless for the field of val to change to the MutableState<T>")
                val info = getBuffKSTypeInfo(it.type, classDeclaration)
                val (ksType, isBuffBean, typeName, nullable, finallyTypeName) = info
                val stateFieldName = "_${fieldName}_state"
                //写入构造内的state字段,普通state或list state
                if (!info.isList) {
                    file.appendText("    ${options.getFieldSerializeTransientAnnotation()} val $stateFieldName: MutableState<$finallyTypeName> = null!!,\n")
                    classFields.add(
                        "    var $fieldName: $finallyTypeName = $stateFieldName.value\n" +
                                "        get() {\n" +
                                "            $stateFieldName.value = field\n" +
                                "            return $stateFieldName.value\n" +
                                "        }\n" +
                                "        set(value) {\n" +
                                "            field = value\n" +
                                "            $stateFieldName.value = value\n" +
                                "        }\n"
                    )
                } else {
                    file.appendText("    ${options.getFieldSerializeTransientAnnotation()} val $stateFieldName: SnapshotStateList${info.typeString} = null!!,\n")
                    classFields.add(
                        "    val $fieldName: $typeName${info.typeString} = $stateFieldName\n"
                    )
                }
                functionFields.add(
                    FunctionFieldsInfo(
                        fieldName,
                        false,
                        isBuffBean,
                        nullable,
                        info.isList,
                        info.typeString,
                    )
                )
            }
        }
        file.appendText(") {\n")

        fun getInfo() = CustomOptionsInfo(
            originalClassName, className
        )
        //写入非构造内的字段
        classFields.forEach(file::appendText)
        //写入removeBuff
        file.appendText(
            "\n    fun removeBuff(): $fullName =\n" +
                    "        $fullName(${
                        functionFields.filter { it.isInTheConstructor }.joinToString {
                            if (it.isBuffBean)
                                "${it.fieldName}${it.nullable}.removeBuff${it.getBuffSuffix()}()"
                            else
                                it.fieldName
                        }
                    }).also {\n"
        )
        functionFields.filter { !it.isInTheConstructor }.forEach {
            file.appendText(
                "            it.${it.fieldName} = ${
                    if (it.isBuffBean)
                        "${it.fieldName}${
                            if (it.isList) "" else it.nullable
                        }.removeBuff${it.getBuffSuffix()}()"
                    else
                        it.fieldName
                }\n"
            )
        }
        file.appendText("        }\n")
        file.appendText("\n${options.getCustomInClass(::getInfo)}\n\n")
        file.appendText("}\n\n")
        //写入addBuff
        file.appendText(
            "fun $fullName.addBuff(): $className =\n" +
                    "    $className(\n"
        )
        functionFields.forEach {
            if (it.isInTheConstructor)
                file.appendText(
                    "        ${
                        if (it.isBuffBean)
                            "${it.fieldName}${it.nullable}.addBuff${it.getBuffSuffix()}()"
                        else
                            it.fieldName
                    },\n"
                )
            else {
                if (!it.isList) {
                    file.appendText(
                        "        mutableStateOf(${
                            if (it.isBuffBean)
                                "${it.fieldName}${it.nullable}.addBuff${it.getBuffSuffix()}()"
                            else
                                it.fieldName
                        }),\n"
                    )
                } else {
                    file.appendText(
                        "        ${
                            if (it.isBuffBean)
                                "${it.fieldName}${it.nullable}.addBuff${it.getBuffSuffix()}()"
                            else
                                it.fieldName
                        }${it.nullable}.toMutableStateList() ?: mutableStateListOf(),\n"
                    )
                }
            }
        }
        file.appendText("    )\n\n${options.getCustomInFile(::getInfo)}")
        //写入Collection<addBuff>
        file.appendText(
            "\n\nfun Collection<$fullName?>.addBuffWithNull() =\n" +
                    "    map { it?.addBuff() }"
        )
        file.appendText(
            "\nfun Collection<$fullName>.addBuff() =\n" +
                    "    map { it.addBuff() }"
        )
        //写入Collection<removeBuff>
        file.appendText(
            "\n\nfun Collection<$className?>.removeBuffWithNull() =\n" +
                    "    map { it?.removeBuff() }"
        )
        file.appendText(
            "\nfun Collection<$className>.removeBuff() =\n" +
                    "    map { it.removeBuff() }"
        )
        file.flush()
        file.close()
    }
}