package com.lt.buff.provider

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.lt.buff.Buff
import com.lt.buff.BuffVariability
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
    @OptIn(KspExperimental::class)
    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
        //获取class信息并创建kt文件
        val buff = classDeclaration.getAnnotationsByType(Buff::class).first()

        //收集数据
        val packageName = classDeclaration.packageName.asString()//包名
        val originalClassName = classDeclaration.simpleName.asString()//原类名
        val fullName = classDeclaration.qualifiedName?.asString()
            ?: (classDeclaration.packageName.asString() + classDeclaration.simpleName.asString())//原全类名
        val className = "$originalClassName${KspOptions.suffix}"//新类名
        val customOptionsInfo = CustomOptionsInfo(originalClassName, className)
        //是否有Stable注解
        val haveStable = classDeclaration.annotations.find {
            getAnnotationFullClassName(it) == "androidx.compose.runtime.Stable"
        } != null
        //是否有Immutable注解
        val haveImmutable = classDeclaration.annotations.find {
            getAnnotationFullClassName(it) == "androidx.compose.runtime.Immutable"
        } != null
        //构造内字段
        val constructorFields = mutableListOf<FunctionFieldsInfo>()
        //类内字段
        val classFields = mutableListOf<FunctionFieldsInfo>()

        //遍历主构造内的字段
        classDeclaration.primaryConstructor?.parameters?.forEach {
            val name = it.name?.getShortName() ?: ""
            constructorFields.add(
                getFieldInfo(
                    name,
                    it.type,
                    it.isVal,
                    classDeclaration,
                    true,
                    buff,
                )
            )
        }
        //遍历类内字段
        classDeclaration.getAllProperties().forEach {
            //只解析类成员
            val fieldName = it.simpleName.getShortName()
            if (constructorFields.find { it.fieldName == fieldName } != null)
                return@forEach
            classFields.add(
                getFieldInfo(
                    fieldName,
                    it.type,
                    !it.isMutable,
                    classDeclaration,
                    false,
                    buff,
                )
            )
        }
        val buffBeanPackage = "com.lt.buff.bean"

        //将数据写入文件
        val sources = classDeclaration.containingFile?.let { arrayOf(it) } ?: arrayOf()
        val file = environment.codeGenerator.createNewFile(
            Dependencies(
                true,
                *sources,
            ), buffBeanPackage, className
        )
        //写入文件头部
        file.appendText("package $buffBeanPackage\n\n")
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
        file.appendText("class $className(\n")
        //写入构造内的字段,并判断是否需要转为state
        constructorFields.forEach {
            file.appendText(genConstructorField(it, buff))
        }
        classFields.forEach {
            file.appendText(genConstructorField(it, buff))
        }
        file.appendText(") {\n")
        //写入类内state get/set代码
        constructorFields.forEach {
            file.appendText(genStateGetSet(it, buff))
        }
        classFields.forEach {
            file.appendText(genStateGetSet(it, buff))
        }
        //写入removeBuff
        file.appendText(
            "\n    fun removeBuff(): $fullName =\n" +
                    "        $fullName(${
                        constructorFields.joinToString {
                            if (it.typeInfo.isBuffBean) {
                                if (it.typeInfo.isList) {
                                    "${it.fieldName}.removeBuff${it.getBuffSuffix()}().toMutableList()"
                                } else
                                    "${it.fieldName}${it.typeInfo.nullable}.removeBuff${it.getBuffSuffix()}()"
                            } else
                                it.fieldName
                        }
                    }).also {\n"
        )
        classFields.forEach {
            if (it.isVal)
                return@forEach
            file.appendText(
                "            it.${it.fieldName} = ${
                    if (it.typeInfo.isBuffBean) {
                        if (it.typeInfo.isList) {
                            "${it.fieldName}.removeBuff${it.getBuffSuffix()}().toMutableList()"
                        } else
                            "${it.fieldName}${it.typeInfo.nullable}.removeBuff${it.getBuffSuffix()}()"
                    } else
                        it.fieldName
                }\n"
            )
        }
        file.appendText("        }\n")
        file.appendText("\n")
        val customInClass = options.getCustomInClass(customOptionsInfo)
        if (customInClass.isNotEmpty())
            file.appendText(customInClass + "\n\n")
        file.appendText("}\n\n")
        //写入addBuff
        file.appendText(
            "fun $fullName.addBuff(): $className =\n" +
                    "    $className(\n"
        )
        (constructorFields + classFields).forEach {
            if (!isToState(it, buff))
                file.appendText(
                    "        ${
                        if (it.typeInfo.isBuffBean)
                            "${it.fieldName}${it.typeInfo.nullable}.addBuff${it.getBuffSuffix()}()"
                        else
                            it.fieldName
                    },\n"
                )
            else {
                if (!it.typeInfo.isList) {
                    file.appendText(
                        "        mutableStateOf(${
                            if (it.typeInfo.isBuffBean)
                                "${it.fieldName}${it.typeInfo.nullable}.addBuff${it.getBuffSuffix()}()"
                            else
                                it.fieldName
                        }),\n"
                    )
                } else {
                    file.appendText(
                        "        ${
                            if (it.typeInfo.isBuffBean)
                                "${it.fieldName}${it.typeInfo.nullable}.addBuff${it.getBuffSuffix()}()"
                            else
                                it.fieldName
                        }${it.typeInfo.nullable}.toMutableStateList() ?: mutableStateListOf(),\n"
                    )
                }
            }
        }
        file.appendText("    )")
        val customInFile = options.getCustomInFile(customOptionsInfo)
        if (customInFile.isNotEmpty())
            file.appendText("\n\n" + customInFile)
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


        //将数据写入文件
        /*val file = environment.codeGenerator.createNewFile(
            Dependencies(
                true,
                classDeclaration.containingFile!!
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
        file.close()*/
    }

    //通过ksp获取属性信息
    private fun getFieldInfo(
        parameterName: String,
        parameterType: KSTypeReference,
        isVal: Boolean,
        classDeclaration: KSClassDeclaration,
        isInTheConstructor: Boolean,
        buff: Buff,
    ): FunctionFieldsInfo {
        val ksTypeInfo = getBuffKSTypeInfo(parameterType, classDeclaration)
        val fieldInfo = FunctionFieldsInfo(
            fieldName = parameterName,
            isInTheConstructor = isInTheConstructor,
            isVal = isVal,
            typeInfo = ksTypeInfo,
        )
        fieldInfo.isToState = isToState(fieldInfo, buff)
        return fieldInfo
    }

    //生成属性在构造中的代码
    private fun genConstructorField(
        info: FunctionFieldsInfo,
        buff: Buff,
    ): String {
        val toState = isToState(info, buff)
        return if (toState) {
            if (info.typeInfo.isList)
                "    val ${info.fieldName}: SnapshotStateList${info.typeInfo.typeString},\n"
            else
                "    private val ${info.getStateFieldName()}: MutableState<${info.typeInfo.finallyTypeName}>,\n"
        } else {
            "    ${if (info.isVal) "val" else "var"} ${info.fieldName}: ${info.typeInfo.finallyTypeName},\n"
        }
    }

    //生成state get/set代码
    private fun genStateGetSet(
        info: FunctionFieldsInfo,
        buff: Buff,
    ): String {
        val toState = isToState(info, buff)
        if (!toState)
            return ""
        if (info.typeInfo.isList)
            return ""
        val value = (
                "    var ${info.fieldName}: ${info.typeInfo.finallyTypeName}\n" +
                        "        get() = ${info.getStateFieldName()}.value\n" +
                        "        set(value) {\n" +
                        "            ${info.getStateFieldName()}.value = value\n" +
                        "        }\n"
                )
        return value
    }

    //判断属性是否要转为state
    private fun isToState(
        info: FunctionFieldsInfo,
        buff: Buff,
    ): Boolean {
        //scope检查
        //如果不转换构造,但在构造内
        if (!buff.scope.hasConstructorProperties && info.isInTheConstructor)
            return false
        //如果不转换类内,但在类内
        if (!buff.scope.hasClassProperties && !info.isInTheConstructor)
            return false

        //variability检查
        if (buff.variability == BuffVariability.All) {
            //只有类内val的非list不转
            if (!info.isInTheConstructor && info.isVal && !info.typeInfo.isList)
                return false
            return true
        } else {
            //var和MutableList都转
            if (!info.isVal)
                return true
            if (info.typeInfo.isMutableList)
                return true
            return false
        }
    }
}