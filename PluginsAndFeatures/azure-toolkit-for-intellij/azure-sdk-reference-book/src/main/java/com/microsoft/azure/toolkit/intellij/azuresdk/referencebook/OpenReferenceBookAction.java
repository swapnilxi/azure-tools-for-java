/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

public class OpenReferenceBookAction extends AnAction implements DumbAware {
    public static final String ID = "user/AzureToolkit.OpenSdkReferenceBook";

    @Override
    public void actionPerformed(@Nonnull final AnActionEvent event) {
        openSdkReferenceBook(event.getProject(), null);
    }

    @AzureOperation("user/sdk.open_reference_book")
    public static void openSdkReferenceBook(final @Nullable Project project, @Nullable final String feature) {
        AzureTaskManager.getInstance().runLater(() -> {
            final AzureSdkReferenceBookDialog dialog = new AzureSdkReferenceBookDialog(project);
            if (StringUtils.isNotEmpty(feature)) {
                dialog.selectFeature(feature);
            }
            dialog.show();
        });
    }
}
