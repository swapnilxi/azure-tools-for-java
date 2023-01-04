/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.microsoft.azure.toolkit.ide.common.IActionsContributor;
import com.microsoft.azure.toolkit.ide.containerapps.ContainerAppsActionsContributor;
import com.microsoft.azure.toolkit.intellij.containerapps.creation.CreateContainerAppAction;
import com.microsoft.azure.toolkit.intellij.containerapps.updateimage.UpdateContainerImageAction;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.account.IAzureAccount;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Region;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

import static com.microsoft.azure.toolkit.lib.Azure.az;

public class IntelliJContainerAppsActionsContributor implements IActionsContributor {
    @Override
    public void registerHandlers(AzureActionManager am) {
        final BiPredicate<ContainerApp, AnActionEvent> serviceCondition = (r, e) -> r != null;
        am.registerHandler(ContainerAppsActionsContributor.UPDATE_IMAGE, UpdateContainerImageAction::openUpdateDialog);

        am.registerHandler(ContainerAppsActionsContributor.CREATE_CONTAINER_APP,
                (Object r, AnActionEvent e) -> CreateContainerAppAction.create(e.getProject(), getDefaultConfig(r, null)));
        am.registerHandler(ContainerAppsActionsContributor.GROUP_CREATE_CONTAINER_APP,
                (ResourceGroup r, AnActionEvent e) -> CreateContainerAppAction.create(e.getProject(), getDefaultConfig(null, r)));
    }

    private ContainerAppDraft.Config getDefaultConfig(final Object o, final ResourceGroup resourceGroup) {
        final ContainerAppDraft.Config result = new ContainerAppDraft.Config();
        result.setName(Utils.generateRandomResourceName("aca", 32));
        final List<Subscription> subs = Azure.az(IAzureAccount.class).account().getSelectedSubscriptions();
        final Subscription historySub = CacheManager.getUsageHistory(Subscription.class).peek(subs::contains);
        final Subscription sub = Optional.ofNullable(historySub).orElseGet(() -> subs.get(0));
        result.setSubscription(sub);
        final List<Region> regions = az(AzureAccount.class).listRegions(sub.getId());
        final Region historyRegion = CacheManager.getUsageHistory(Region.class).peek(regions::contains);
        result.setRegion(historyRegion);
        final ResourceGroup historyRg = CacheManager.getUsageHistory(ResourceGroup.class)
                .peek(r -> r.getSubscriptionId().equals(sub.getId()));
        result.setResourceGroup(ObjectUtils.firstNonNull(resourceGroup, historyRg));
        final ContainerAppsEnvironment cae = o instanceof ContainerAppsEnvironment ? (ContainerAppsEnvironment) o : CacheManager.getUsageHistory(ContainerAppsEnvironment.class)
                .peek(r -> r.getSubscriptionId().equals(sub.getId()));
        result.setEnvironment(cae);
        return result;
    }

    @Override
    public int getOrder() {
        return ContainerAppsActionsContributor.INITIALIZE_ORDER + 1;
    }
}
