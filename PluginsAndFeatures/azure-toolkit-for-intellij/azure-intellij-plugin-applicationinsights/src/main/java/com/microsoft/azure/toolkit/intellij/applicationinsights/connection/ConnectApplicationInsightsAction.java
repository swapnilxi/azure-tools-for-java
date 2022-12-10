/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.connection;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.intellij.connector.AzureServiceResource;
import com.microsoft.azure.toolkit.intellij.connector.ConnectorDialog;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.common.model.AzResource;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;

public class ConnectApplicationInsightsAction {

    @AzureOperation(name = "user/ai.open_connector_dialog.ai", params = {"r.getName()"}, type = AzureOperation.Type.ACTION)
    public static void openConnectorDialog(AzResource r, AnActionEvent e) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ConnectorDialog dialog = new ConnectorDialog(e.getProject());
            dialog.setResource(new AzureServiceResource<>(((ApplicationInsight) r), ApplicationInsightsResourceDefinition.INSTANCE));
            dialog.show();
        });
    }
}
