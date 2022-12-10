/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.applicationinsights.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsight;
import com.microsoft.azure.toolkit.lib.applicationinsights.ApplicationInsightDraft;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class CreateApplicationInsightsAction {
    @AzureOperation("user/ai.open_creation_dialog")
    public static void openCreationDialog(@Nonnull Project project, @Nullable final ApplicationInsightDraft data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ApplicationInsightsCreationDialog dialog = new ApplicationInsightsCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            dialog.setOkActionListener(new AzureDialog.OkActionListener<>() {
                @Override
                @AzureOperation(name = "user/ai.create_ai.ai", params = {"config.getName()"}, type = AzureOperation.Type.ACTION)
                public void onOk(ApplicationInsightDraft config) {
                    dialog.close();
                    final AzureString title = OperationBundle.description("user/ai.create_ai.ai", config.getName());
                    AzureTaskManager.getInstance().runInBackground(title, () -> createApplicationInsights(config));
                }
            });
            dialog.show();
        });
    }

    public static ApplicationInsight createApplicationInsights(ApplicationInsightDraft draft) {
        final String subscriptionId = draft.getSubscriptionId();
        OperationContext.action().setTelemetryProperty("subscriptionId", subscriptionId);
        if (draft.getResourceGroup() == null) { // create resource group if necessary.
            final ResourceGroup newResourceGroup = Azure.az(AzureResources.class)
                .groups(subscriptionId).createResourceGroupIfNotExist(draft.getResourceGroupName(), draft.getRegion());
        }
        final ApplicationInsight resource = draft.commit();
        CacheManager.getUsageHistory(ApplicationInsight.class).push(draft);
        return resource;
    }
}
