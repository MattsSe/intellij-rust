/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.formatter

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementWalkingVisitor
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessorHelper
import org.rust.ide.formatter.impl.CommaList
import org.rust.lang.core.psi.RsElementTypes.COMMA
import org.rust.lang.core.psi.ext.elementType
import org.rust.lang.core.psi.ext.getPrevNonCommentSibling

class RsTrailingCommaFormatProcessor : PostFormatProcessor {
    override fun processElement(source: PsiElement, settings: CodeStyleSettings): PsiElement {
        doProcess(source, settings, null)
        return source
    }

    override fun processText(source: PsiFile, rangeToReformat: TextRange, settings: CodeStyleSettings): TextRange {
        return doProcess(source, settings, rangeToReformat).resultTextRange
    }

    private fun doProcess(source: PsiElement, settings: CodeStyleSettings, range: TextRange? = null): PostFormatProcessorHelper {
        val helper = PostFormatProcessorHelper(settings)
        helper.resultTextRange = range
        if (settings.rust.PRESERVE_PUNCTUATION) return helper

        source.accept(object : PsiRecursiveElementWalkingVisitor() {
            override fun visitElement(element: PsiElement) {
                super.visitElement(element)
                if (!helper.isElementFullyInRange(element)) return
                val commaList = CommaList.forElement(element.elementType) ?: return
                if (fixSingleLineBracedBlock(element, commaList)) {
                    helper.updateResultRange(1, 0)
                }
            }
        })

        return helper
    }

    companion object {
        /**
         * Delete trailing comma in one-line blocks
         */
        fun fixSingleLineBracedBlock(block: PsiElement, commaList: CommaList): Boolean {
            if (PostFormatProcessorHelper.isMultiline(block)) return false
            val rbrace = block.lastChild
            if (rbrace.elementType != commaList.closingBrace) return false
            val comma = rbrace.getPrevNonCommentSibling() ?: return false

            return if (comma.elementType == COMMA) {
                comma.delete()
                true
            } else {
                false
            }
        }
    }
}

