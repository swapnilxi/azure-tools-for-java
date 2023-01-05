/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironmentModule;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask;

import javax.annotation.Nullable;
import java.util.Objects;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class CreateContainerAppsEnvironmentAction {
    public static void create(@Nullable Project project, @Nullable ContainerAppsEnvironmentDraft.Config data) {
        AzureTaskManager.getInstance().runLater(() -> {
            final ContainerAppsEnvironmentCreationDialog dialog = new ContainerAppsEnvironmentCreationDialog(project);
            if (Objects.nonNull(data)) {
                dialog.getForm().setValue(data);
            }
            dialog.setOkActionListener(config -> {
                doCreate(config, project);
                dialog.close();
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "user/containerapps.create_container_apps_environment.environment", params = {"config.getName()"})
    private static void doCreate(final ContainerAppsEnvironmentDraft.Config config, final Project project) {
        final AzureString title = OperationBundle.description("user/containerapps.create_container_apps_environment.environment", config.getName());
        AzureTaskManager.getInstance().runInBackground(title, () -> {
            final ResourceGroup rg = config.getResourceGroup();
            if (rg.isDraftForCreating()) {
                new CreateResourceGroupTask(rg.getSubscriptionId(), rg.getName(), config.getRegion()).execute();
            }
            final ContainerAppsEnvironmentModule module = az(AzureContainerApps.class).environments(config.getSubscription().getId());
            final ContainerAppsEnvironmentDraft draft = module.create(config.getName(), config.getResourceGroup().getName());
            CacheManager.getUsageHistory(ContainerAppsEnvironment.class).push(draft);
            draft.setConfig(config);
            draft.commit();
        });
    }
}
