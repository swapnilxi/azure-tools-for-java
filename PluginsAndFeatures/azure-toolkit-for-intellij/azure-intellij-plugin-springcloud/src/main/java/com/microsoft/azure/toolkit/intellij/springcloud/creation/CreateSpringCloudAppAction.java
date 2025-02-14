/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.IArtifact;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudCluster;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudAppConfig;
import com.microsoft.azure.toolkit.lib.springcloud.config.SpringCloudDeploymentConfig;
import com.microsoft.azure.toolkit.lib.springcloud.task.DeploySpringCloudAppTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public class CreateSpringCloudAppAction {
    private static final int GET_STATUS_TIMEOUT = 180;
    private static final String GET_DEPLOYMENT_STATUS_TIMEOUT = "Deployment succeeded but the app is still starting, " +
        "you can check the app status from Azure Portal.";
    private static final String NOTIFICATION_TITLE = "Deploy Spring app";

    public static void createApp(@Nonnull SpringCloudCluster cluster, @Nullable Project project) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudAppCreationDialog dialog = new SpringCloudAppCreationDialog(cluster, project);
            dialog.setOkActionListener((config) -> {
                dialog.close();
                createApp(config);
            });
            dialog.show();
        });
    }

    @AzureOperation(name = "user/springcloud.create_app.app", params = "config.getAppName()")
    private static void createApp(SpringCloudAppConfig config) {
        AzureTaskManager.getInstance().runInBackground(OperationBundle.description("user/springcloud.create_app.app", config.getAppName()), () -> {
            final DeploySpringCloudAppTask task = new DeploySpringCloudAppTask(config);
            final SpringCloudDeployment deployment = task.execute();
            CacheManager.getUsageHistory(SpringCloudCluster.class).push(deployment.getParent().getParent());
            CacheManager.getUsageHistory(SpringCloudApp.class).push(deployment.getParent());
            final boolean hasArtifact = Optional.ofNullable(config.getDeployment())
                .map(SpringCloudDeploymentConfig::getArtifact)
                .map(IArtifact::getFile).isPresent();
            if (hasArtifact && !deployment.waitUntilReady(GET_STATUS_TIMEOUT)) {
                AzureMessager.getMessager().warning(GET_DEPLOYMENT_STATUS_TIMEOUT, NOTIFICATION_TITLE);
            }
        });
    }
}
