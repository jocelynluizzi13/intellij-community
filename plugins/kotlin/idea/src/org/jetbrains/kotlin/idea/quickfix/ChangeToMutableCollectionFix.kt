// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModPsiUpdater
import com.intellij.modcommand.Presentation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.base.resources.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.base.psi.replaced
import org.jetbrains.kotlin.idea.codeinsight.api.applicable.intentions.KotlinPsiUpdateModCommandAction
import org.jetbrains.kotlin.idea.project.builtIns
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.calls.util.getType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.KotlinType

class ChangeToMutableCollectionFix(property: KtProperty, context: ElementContext) :
    KotlinPsiUpdateModCommandAction.ElementBased<KtProperty, ChangeToMutableCollectionFix.ElementContext>(property, context) {

    class ElementContext(
        val typeName: String,
    )

    override fun getPresentation(context: ActionContext, element: KtProperty): Presentation {
        val elementContext = getElementContext(context, element)
        return Presentation.of(KotlinBundle.message("fix.change.to.mutable.type.text", "Mutable${elementContext.typeName}"))
    }

    override fun getFamilyName(): @IntentionFamilyName String = KotlinBundle.message("fix.change.to.mutable.type.family")

    override fun invoke(actionContext: ActionContext, element: KtProperty, elementContext: ElementContext, updater: ModPsiUpdater) {
        val property = element
        val context = property.analyze(BodyResolveMode.PARTIAL)
        val type = property.initializer?.getType(context) ?: return
        applyFix(property, type)
        updater.moveCaretTo(property.endOffset)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val element = Errors.NO_SET_METHOD.cast(diagnostic).psiElement as? KtArrayAccessExpression ?: return null
            val arrayExpr = element.arrayExpression ?: return null
            val context = arrayExpr.analyze(BodyResolveMode.PARTIAL)
            val type = arrayExpr.getType(context) ?: return null
            if (!type.isReadOnlyListOrMap(element.builtIns)) return null
            val property = arrayExpr.mainReference?.resolve() as? KtProperty ?: return null
            if (!isApplicable(property)) return null
            val typeName = type.constructor.declarationDescriptor?.name?.asString() ?: return null
            return ChangeToMutableCollectionFix(property, ElementContext(typeName)).asIntention()
        }

        private fun KotlinType.isReadOnlyListOrMap(builtIns: KotlinBuiltIns): Boolean {
            val leftDefaultType = constructor.declarationDescriptor?.defaultType ?: return false
            return leftDefaultType in listOf(builtIns.list.defaultType, builtIns.map.defaultType)
        }

        fun isApplicable(property: KtProperty): Boolean {
            return property.isLocal && property.initializer != null
        }

        fun applyFix(property: KtProperty, type: KotlinType) {
            val initializer = property.initializer ?: return
            val fqName = initializer.resolveToCall()?.resultingDescriptor?.fqNameOrNull()?.asString()
            val psiFactory = KtPsiFactory(property.project)
            val mutableOf = mutableConversionMap[fqName]
            if (mutableOf != null) {
                (initializer as? KtCallExpression)?.calleeExpression?.replaced(psiFactory.createExpression(mutableOf)) ?: return
            } else {
                val builtIns = property.builtIns
                val toMutable = when (type.constructor) {
                    builtIns.list.defaultType.constructor -> "toMutableList"
                    builtIns.set.defaultType.constructor -> "toMutableSet"
                    builtIns.map.defaultType.constructor -> "toMutableMap"
                    else -> null
                } ?: return
                val dotQualifiedExpression = initializer.replaced(
                    psiFactory.createExpressionByPattern("($0).$1()", initializer, toMutable)
                ) as KtDotQualifiedExpression
                val receiver = dotQualifiedExpression.receiverExpression
                val deparenthesize = KtPsiUtil.deparenthesize(dotQualifiedExpression.receiverExpression)
                if (deparenthesize != null && receiver != deparenthesize) receiver.replace(deparenthesize)
            }
            property.typeReference?.also { it.replace(psiFactory.createType("Mutable${it.text}")) }
        }

        private const val COLLECTIONS = "kotlin.collections"

        private val mutableConversionMap = mapOf(
            "$COLLECTIONS.listOf" to "mutableListOf",
            "$COLLECTIONS.setOf" to "mutableSetOf",
            "$COLLECTIONS.mapOf" to "mutableMapOf"
        )

    }
}