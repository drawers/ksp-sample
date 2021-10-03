package com.tsongkha.kspexample.processor

import com.google.auto.service.AutoService
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ksp.TypeParameterResolver
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.toTypeParameterResolver
import com.squareup.kotlinpoet.ksp.writeTo
import com.tsongkha.kspexample.annotation.IntSummable

class IntSummableProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {

    private lateinit var intType: KSType

    override fun process(resolver: Resolver): List<KSAnnotated> {
        intType = resolver.builtIns.intType
        val symbols = resolver.getSymbolsWithAnnotation(IntSummable::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }

        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(Visitor(), Unit) }

        return unableToProcess.toList()
    }

    private inner class Visitor : KSVisitorVoid() {

        private lateinit var ksType: KSType
        private lateinit var packageName: String
        private val summables: MutableList<String> = mutableListOf()

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val qualifiedName = classDeclaration.qualifiedName?.asString() ?: run {
                logger.error(
                    "@IntSummable must target classes with qualified names",
                    classDeclaration
                )
                return
            }

            if (!classDeclaration.isDataClass()) {
                logger.error(
                    "@IntSummable cannot target non-data class $qualifiedName",
                    classDeclaration
                )
                return
            }

            if (classDeclaration.typeParameters.any()) {
                logger.error(
                    "@IntSummable must data classes with no type parameters",
                    classDeclaration
                )
                return
            }

            ksType = classDeclaration.asType(emptyList())
            packageName = classDeclaration.packageName.asString()

            classDeclaration.getAllProperties()
                .forEach {
                    it.accept(this, Unit)
                }

            if (summables.isEmpty()) {
                return
            }

            val fileSpec = FileSpec.builder(
                packageName = packageName,
                fileName = classDeclaration.simpleName.asString() + "Ext"
            ).apply {
                addFunction(
                    FunSpec.builder("sumInts")
                        .receiver(ksType.toTypeName(TypeParameterResolver.EMPTY))
                        .returns(Int::class)
                        .addStatement("val sum = %L", summables.joinToString(" + "))
                        .addStatement("return sum")
                        .build()
                )
            }.build()

            fileSpec.writeTo(codeGenerator = codeGenerator, aggregating = false)
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            if (property.type.resolve().isAssignableFrom(intType)) {
                val name = property.simpleName.asString()
                summables.add(name)
            }
        }
    }

    private data class ClassDetails(
        val type: KSType,
        val simpleName: String,
        val packageName: String
    )

    private sealed class UnsupportedIntSummableException : Exception() {
        object DataClassWithTypeParameters: UnsupportedIntSummableException()
        object NonDataClassException: UnsupportedIntSummableException()
    }

    private fun KSClassDeclaration.isDataClass() =
        modifiers.contains(Modifier.DATA)
}
