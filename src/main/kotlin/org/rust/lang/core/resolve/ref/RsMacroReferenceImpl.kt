/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref

import com.intellij.psi.PsiElement
import org.rust.lang.core.psi.RsMacroReference
import org.rust.lang.core.psi.ext.RsCompositeElement
import org.rust.lang.core.resolve.collectCompletionVariants
import org.rust.lang.core.resolve.collectResolveVariants
import org.rust.lang.core.resolve.processMacroReferenceVariants
import org.rust.lang.core.types.BoundElement


class RsMacroReferenceImpl(pattern: RsMacroReference) : RsReferenceBase<RsMacroReference>(pattern) {
    override val RsMacroReference.referenceAnchor: PsiElement
        get() = referenceNameElement

    override fun getVariants(): Array<out Any> =
        collectCompletionVariants { processMacroReferenceVariants(element, it) }

    override fun resolveInner(): List<BoundElement<RsCompositeElement>>
        =  collectResolveVariants(element.referenceName) { processMacroReferenceVariants(element, it) }
}
