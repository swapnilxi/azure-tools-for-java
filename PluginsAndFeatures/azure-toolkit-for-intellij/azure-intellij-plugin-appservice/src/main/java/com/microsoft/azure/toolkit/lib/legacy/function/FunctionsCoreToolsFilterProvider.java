/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function;

import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.system.CpuArch;
import com.microsoft.azure.toolkit.ide.appservice.function.coretools.FunctionsCoreToolsManager;
import com.microsoft.azure.toolkit.ide.appservice.function.coretools.ReleaseFilterProvider;

import java.util.List;

public class FunctionsCoreToolsFilterProvider implements ReleaseFilterProvider {

    @Override
    public FunctionsCoreToolsManager.ReleaseFilter getFilter() {
        if (SystemInfo.isWindows && CpuArch.isIntel64()) {
            return new FunctionsCoreToolsManager.ReleaseFilter("Windows", List.of("x64"), List.of("minified", "full"));
        } else if (SystemInfo.isWindows) {
            return new FunctionsCoreToolsManager.ReleaseFilter("Windows", List.of("x86"), List.of("minified", "full"));
        } else if (SystemInfo.isMac && CpuArch.isArm64()) {
            return new FunctionsCoreToolsManager.ReleaseFilter("MacOS", List.of("arm64", "x64"), List.of("full"));
        } else if (SystemInfo.isMac) {
            return new FunctionsCoreToolsManager.ReleaseFilter("MacOS", List.of("x64"), List.of("full"));
        } else if (SystemInfo.isLinux) {
            return new FunctionsCoreToolsManager.ReleaseFilter("Linux", List.of("x64"), List.of("full"));
        }
        return new FunctionsCoreToolsManager.ReleaseFilter("Unknown", List.of("x64"), List.of("full"));
    }
}
