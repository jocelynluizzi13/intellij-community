// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.kotlin.idea.j2k.post.processing.inference.nullability

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.idea.j2k.post.processing.inference.common.*
import org.jetbrains.kotlin.idea.refactoring.isAbstract
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier

class NullabilityDefaultStateProvider : DefaultStateProvider() {
    override fun defaultStateFor(typeVariable: TypeVariable): State {
        if (typeVariable is TypeElementBasedTypeVariable
            && typeVariable.typeElement.type.constructor.declarationDescriptor is TypeParameterDescriptor
        ) {
            return State.LOWER
        }
        return when (val owner = typeVariable.owner) {
            is FunctionParameter -> {
                val parentOfOwners = owner.owner.parent
                if (owner.owner.isPrivate() || (parentOfOwners is KtClass && parentOfOwners.isEnum() && parentOfOwners.visibilityModifier()?.text == "internal")) {
                    return State.LOWER
                } else {
                    return State.UPPER
                }
            }

            is FunctionReturnType ->
                if (owner.function.isAbstract()
                    || owner.function.hasModifier(KtTokens.OPEN_KEYWORD)
                    || owner.function.bodyExpression == null
                ) State.UPPER else State.LOWER

            is Property -> if (owner.property.isPrivate()
                || !owner.property.isVar
                || !owner.property.isLocal
            ) State.LOWER else State.UPPER

            is TypeArgument -> State.LOWER
            OtherTarget -> State.UPPER
        }
    }
}