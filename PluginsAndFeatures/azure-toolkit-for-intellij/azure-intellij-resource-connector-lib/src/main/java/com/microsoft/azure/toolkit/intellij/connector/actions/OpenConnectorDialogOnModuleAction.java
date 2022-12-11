/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.lib.common.messager.ExceptionNotification;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

public class OpenConnectorDialogOnModuleAction extends AnAction {
    @Override
    @ExceptionNotification
    @AzureOperation(name = "user/connector.open_connector_dialog_on_module", type = AzureOperation.Type.ACTION)
    public void actionPerformed(@NotNull final AnActionEvent event) {
        final Module module = LangDataKeys.MODULE.getData(event.getDataContext());
        if (module != null) {
            final Project project = module.getProject();
            final ConnectorDialog dialog = new ConnectorDialog(project);
            dialog.setConsumer(new ModuleResource(module.getName()));
            dialog.show();
        }
    }
}
