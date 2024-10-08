// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.idea.base.analysisApiPlatform

import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.analysis.api.platform.packages.KotlinPackagePartProviderFactory
import org.jetbrains.kotlin.idea.caches.resolve.IDEPackagePartProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider

internal class IdeKotlinPackagePartProviderFactory : KotlinPackagePartProviderFactory {
    override fun createPackagePartProvider(scope: GlobalSearchScope): PackagePartProvider {
        return IDEPackagePartProvider(scope)
    }
}
