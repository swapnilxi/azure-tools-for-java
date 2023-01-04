package com.microsoft.azure.toolkit.intellij.containerapps.component;

import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerApps;
import com.microsoft.azure.toolkit.lib.containerapps.AzureContainerAppsServiceSubscription;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class AzureContainerAppsEnvironmentComboBox extends AzureComboBox<ContainerAppsEnvironment> {
    private Subscription subscription;
    private final List<ContainerAppsEnvironment> draftItems = new LinkedList<>();

    @Override
    public String getLabel() {
        return "Container Apps Environment";
    }

    @Override
    protected String getItemText(Object item) {
        if (item instanceof ContainerAppsEnvironment) {
            final ContainerAppsEnvironment environment = (ContainerAppsEnvironment) item;
            return environment.isDraftForCreating() ? "(New) " + environment.getName() : environment.getName();
        }
        return super.getItemText(item);
    }

    public void setSubscription(Subscription subscription) {
        if (Objects.equals(subscription, this.subscription)) {
            return;
        }
        this.subscription = subscription;
        if (subscription == null) {
            this.clear();
            return;
        }
        this.reloadItems();
    }

    @Override
    public void setValue(@Nullable ContainerAppsEnvironment val) {
        if (Objects.nonNull(val) && val.isDraftForCreating() && !val.exists()) {
            this.draftItems.remove(val);
            this.draftItems.add(0, val);
            this.reloadItems();
        }
        super.setValue(val);
    }

    @Nullable
    @Override
    protected ContainerAppsEnvironment doGetDefaultValue() {
        return CacheManager.getUsageHistory(ContainerAppsEnvironment.class)
                .peek(g -> Objects.isNull(subscription) || Objects.equals(subscription.getId(), g.getSubscriptionId()));
    }

    @Nonnull
    @Override
    protected List<? extends ContainerAppsEnvironment> loadItems() {
        Stream<AzureContainerAppsServiceSubscription> stream = Azure.az(AzureContainerApps.class).list().stream();
        if (Objects.nonNull(this.subscription)) {
            stream = stream.filter(s -> s.getSubscriptionId().equalsIgnoreCase(this.subscription.getId()));
        }
        final List<ContainerAppsEnvironment> remoteEnvironments = stream.flatMap(s -> s.environments().list().stream())
                .sorted(Comparator.comparing(ContainerAppsEnvironment::getName)).toList();
        final List<ContainerAppsEnvironment> environments = new ArrayList<>(remoteEnvironments);
        if (CollectionUtils.isNotEmpty(this.draftItems)) {
            this.draftItems.stream()
                    .filter(i -> StringUtils.equalsIgnoreCase(this.subscription.getId(), i.getSubscriptionId()))
                    .filter(i -> !remoteEnvironments.contains(i)) // filter out the draft item which has been created
                    .forEach(environments::add);
        }
        return environments;
    }

    @Override
    protected void refreshItems() {
        Optional.ofNullable(this.subscription).ifPresent(s -> Azure.az(AzureContainerApps.class).containerApps(s.getId()).refresh());
        super.refreshItems();
    }
}
