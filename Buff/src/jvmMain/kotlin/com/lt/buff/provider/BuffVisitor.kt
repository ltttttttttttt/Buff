package com.lt.buff.provider

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.KSVisitorVoid
import com.lt.buff.Buff
import com.lt.buff.BuffVariability
import com.lt.buff.getAnnotationFullClassName
import com.lt.buff.getBuffKSTypeInfo
import com.lt.buff.getClassName
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
        var originalClassName = classDeclaration.simpleName.asString()//原类名
        val className = "$originalClassName${KspOptions.suffix}"//新类名
        originalClassName = getClassName(classDeclaration.parentDeclaration, originalClassName)
        val customOptionsInfo = CustomOptionsInfo(originalClassName, className)
        //是否有Stable注解
        val haveStable = classDeclaration.annotations.find {
            getAnnotationFullClassName(it) == "androidx.compose.runtime.Stable"
        } != null
        //是否有Immutable注解
        val haveImmutable = classDeclaration.annotations.find {
            getAnnotationFullClassName(it) == "androidx.compose.runtime.Immutable"
        } != null
        //所有包名
        val packageSet = mutableSetOf(
            "androidx.compose.runtime.snapshots",
            "androidx.compose.runtime",
            packageName,
        )
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
                    it.type.resolve(),
                    it.isVal,
                    classDeclaration,
                    true,
                    buff,
                    packageSet,
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
                    it.type.resolve(),
                    !it.isMutable,
                    classDeclaration,
                    false,
                    buff,
                    packageSet,
                )
            )
        }

        //将数据写入文件
        val sources = classDeclaration.containingFile?.let { arrayOf(it) } ?: arrayOf()
        val file = environment.codeGenerator.createNewFile(
            Dependencies(
                true,
                *sources,
            ), packageName, className
        )
        //写入文件头部
        file.appendText("package $packageName\n\n")
        packageSet.forEach {
            file.appendText("import $it.*\n")
        }
        file.appendText("\n")
        if (haveStable)
            file.appendText("@Stable\n")
        if (haveImmutable)
            file.appendText("@Immutable\n")
        file.appendText("class $className(\n")
        //写入构造内的字段,并判断是否需要转为state
        constructorFields.forEach {
            file.appendText(genConstructorField(it))
        }
        classFields.forEach {
            file.appendText(genConstructorField(it))
        }
        file.appendText(") {\n")
        //写入类内state get/set代码
        constructorFields.forEach {
            file.appendText(genStateGetSet(it))
        }
        classFields.forEach {
            file.appendText(genStateGetSet(it))
        }
        //写入removeBuff
        file.appendText(
            "\n    fun removeBuff(): $originalClassName =\n" +
                    "        $originalClassName(${
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
                        val value =
                            "${it.fieldName}${it.typeInfo.nullable}.removeBuff${it.getBuffSuffix()}()"
                        if (it.typeInfo.isList)
                            "$value${it.typeInfo.nullable}.toMutableList()"
                        else
                            value
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
            "fun $originalClassName.addBuff(): $className =\n" +
                    "    $className(\n"
        )
        (constructorFields + classFields).forEach {
            if (!it.isToState)
                file.appendText(
                    "        ${
                        if (it.typeInfo.isBuffBean) {
                            val value =
                                "${it.fieldName}${it.typeInfo.nullable}.addBuff${it.getBuffSuffix()}()"
                            if (it.typeInfo.isList)
                                "$value${it.typeInfo.nullable}.toMutableList()"
                            else
                                value
                        } else
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
                        }${it.typeInfo.nullable}.toMutableStateList()${
                            if (it.typeInfo.nullable.isEmpty())
                                ""
                            else
                                " ?: mutableStateListOf()"
                        },\n"
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
            "\n\nfun Collection<$originalClassName?>.addBuffWithNull() =\n" +
                    "    map { it?.addBuff() }"
        )
        file.appendText(
            "\nfun Collection<$originalClassName>.addBuff() =\n" +
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

    //通过ksp获取属性信息
    private fun getFieldInfo(
        parameterName: String,
        parameterType: KSType,
        isVal: Boolean,
        classDeclaration: KSClassDeclaration,
        isInTheConstructor: Boolean,
        buff: Buff,
        packageSet: MutableSet<String>,
    ): FunctionFieldsInfo {
        val ksTypeInfo = getBuffKSTypeInfo(parameterType, classDeclaration, packageSet = packageSet)
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
    private fun genConstructorField(info: FunctionFieldsInfo): String {
        return if (info.isToState) {
            if (info.typeInfo.isList)
                "    val ${info.fieldName}: SnapshotStateList${info.typeInfo.typeString},\n"
            else
                "    private val ${info.getStateFieldName()}: MutableState<${info.typeInfo.finallyTypeName}>,\n"
        } else {
            "    ${if (info.isVal) "val" else "var"} ${info.fieldName}: ${info.typeInfo.finallyTypeName},\n"
        }
    }

    //生成state get/set代码
    private fun genStateGetSet(info: FunctionFieldsInfo): String {
        if (!info.isToState)
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