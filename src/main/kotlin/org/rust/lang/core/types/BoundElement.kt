/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types

import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveResult
import org.rust.lang.core.psi.ext.RsCompositeElement
import org.rust.lang.core.types.infer.TypeFolder
import org.rust.lang.core.types.infer.TypeVisitor
import org.rust.lang.core.types.ty.Substitution
import org.rust.lang.core.types.ty.emptySubstitution
import org.rust.lang.core.types.ty.foldValues
import org.rust.lang.core.types.ty.substituteInValues

/* Represents a potentially generic Psi Element, like
 * `fn make_t<T>() -> T { ... }`, together with actual
 * type arguments, like `T := i32`.
 */
data class BoundElement<out E : RsCompositeElement>(
    val element: E,
    val subst: Substitution = emptySubstitution
) : ResolveResult {
    override fun getElement(): PsiElement = element
    override fun isValidResult(): Boolean = true

    inline fun <reified T : RsCompositeElement> downcast(): BoundElement<T>? =
        if (element is T) BoundElement(element, subst) else null

    fun substitute(subst: Substitution) =
        BoundElement(element, this.subst.substituteInValues(subst))

    fun foldWith(folder: TypeFolder) =
        BoundElement(element, this.subst.foldValues(folder))

    fun visitWith(visitor: TypeVisitor): Boolean =
        subst.values.any(visitor)
}

