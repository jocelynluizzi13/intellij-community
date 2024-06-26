// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.j2k.ast

import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.j2k.CodeBuilder
import org.jetbrains.kotlin.j2k.Converter

abstract class Constructor(
        annotations: Annotations,
        modifiers: Modifiers,
        parameterList: ParameterList,
        body: DeferredElement<Block>
) : FunctionLike(annotations, modifiers, parameterList, body) {
    override val parameterList: ParameterList
        get() = super.parameterList!!
}

class PrimaryConstructor(
        annotations: Annotations,
        modifiers: Modifiers,
        parameterList: ParameterList,
        body: DeferredElement<Block>
) : Constructor(annotations, modifiers, parameterList, body) {

    override fun generateCode(builder: CodeBuilder) { throw IncorrectOperationException() }

    // Should be lazy, to defer `assignPrototypesFrom(this,...)` a bit,
    // cause when `PrimaryConstructor` created prototypes not yet assigned
    val initializer: Initializer by
    lazy { Initializer(body, Modifiers.Empty).assignPrototypesFrom(this, CommentsAndSpacesInheritance(commentsBefore = false)) }

    fun createSignature(converter: Converter): PrimaryConstructorSignature {
        val signature = PrimaryConstructorSignature(annotations, modifiers, parameterList)

        // assign prototypes later because we don't know yet whether the body is empty or not
        converter.addPostUnfoldDeferredElementsAction {
            val inheritance = CommentsAndSpacesInheritance(spacesBefore = SpacesInheritance.NONE, commentsAfter = body!!.isEmpty, commentsInside = body.isEmpty)
            signature.assignPrototypesFrom(this, inheritance)
        }

        return signature
    }
}

class PrimaryConstructorSignature(val annotations: Annotations, private val modifiers: Modifiers, val parameterList: ParameterList) : Element() {
    val accessModifier: Modifier? = run {
        val modifier = modifiers.accessModifier()
        if (modifier != Modifier.PUBLIC) modifier else null
    }

    override fun generateCode(builder: CodeBuilder) {
        var needConstructorKeyword = false

        if (!annotations.isEmpty) {
            builder append " " append annotations
            needConstructorKeyword = true
        }

        if (accessModifier != null) {
            builder append " " append Modifiers(listOf(accessModifier)).assignPrototypesFrom(modifiers)
            needConstructorKeyword = true
        }

        if (needConstructorKeyword) {
            builder.append(" constructor")
        }

        builder.append(parameterList)
    }
}

class SecondaryConstructor(
        annotations: Annotations,
        modifiers: Modifiers,
        parameterList: ParameterList,
        body: DeferredElement<Block>,
        private val thisOrSuperCall: DeferredElement<Expression>?
) : Constructor(annotations, modifiers, parameterList, body) {

    override fun generateCode(builder: CodeBuilder) {
        builder.append(annotations)
                .appendWithSpaceAfter(modifiers)
                .append("constructor")
                .append(parameterList)

        if (thisOrSuperCall != null) {
            builder append " : " append thisOrSuperCall
        }

        builder append " " append body!!
    }
}

