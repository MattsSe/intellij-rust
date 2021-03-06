/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.test

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import org.rust.cargo.CargoConstants
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.runconfig.cargoArgumentSpeck
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.runconfig.command.CargoCommandConfigurationType
import org.rust.cargo.runconfig.mergeWithDefault
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.lang.core.psi.RsFunction
import org.rust.lang.core.psi.ext.*

class CargoTestRunConfigurationProducer : RunConfigurationProducer<CargoCommandConfiguration>(CargoCommandConfigurationType()) {

    override fun isConfigurationFromContext(
        configuration: CargoCommandConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val location = context.location ?: return false
        val test = findTest(location.psiElement) ?: return false

        return configuration.configurationModule.module == context.module &&
            configuration.canBeFrom(test.cargoCommandLine)
    }

    override fun setupConfigurationFromContext(
        configuration: CargoCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val location = context.location ?: return false
        val test = findTest(location.psiElement) ?: return false
        sourceElement.set(test.sourceElement)

        configuration.configurationModule.module = context.module
        configuration.name = test.configurationName
        val cmd = test.cargoCommandLine.mergeWithDefault(configuration)
        configuration.setFromCmd(cmd)
        return true
    }


    companion object {
        fun findTest(psi: PsiElement, climbUp: Boolean = true): TestConfig? =
            findTestFunction(psi, climbUp) ?: findTestMod(psi, climbUp)

        private fun findTestFunction(psi: PsiElement, climbUp: Boolean): TestConfig? {
            val fn = findElement<RsFunction>(psi, climbUp) ?: return null
            val name = fn.crateRelativePath.configPath() ?: return null
            val target = fn.containingCargoTarget ?: return null
            return if (fn.isTest) TestConfig(fn, "Test $name", name, target) else null
        }

        private fun findTestMod(psi: PsiElement, climbUp: Boolean): TestConfig? {
            val mod = findElement<RsMod>(psi, climbUp) ?: return null

            val testName = if (mod.modName == "test" || mod.modName == "tests")
                "Test ${mod.`super`?.modName}::${mod.modName}"
            else
                "Test ${mod.modName}"
            val testPath = mod.crateRelativePath.configPath() ?: ""
            val target = mod.containingCargoTarget ?: return null
            if (!mod.functionList.any { it.isTest }) return null

            return TestConfig(mod, testName, testPath, target)
        }

        private inline fun <reified T : PsiElement> findElement(base: PsiElement, climbUp: Boolean): T? {
            if (base is T) return base
            if (!climbUp) return null
            return base.parentOfType<T>(strict = false)
        }
    }
}

class TestConfig(
    val sourceElement: RsCompositeElement,
    val configurationName: String,
    testPath: String,
    target: CargoWorkspace.Target
) {
    val cargoCommandLine: CargoCommandLine = CargoCommandLine(
        CargoConstants.Commands.TEST,
        target.cargoArgumentSpeck + testPath,
        workingDirectory = target.pkg.rootDirectory
    )
}

// We need to chop off heading colon `::`, since `crateRelativePath`
// always returns fully-qualified path
private fun String?.configPath(): String? = this?.removePrefix("::")
