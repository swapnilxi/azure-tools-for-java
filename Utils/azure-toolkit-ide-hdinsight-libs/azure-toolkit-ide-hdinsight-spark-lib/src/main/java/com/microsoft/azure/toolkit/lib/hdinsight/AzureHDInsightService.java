package com.microsoft.azure.toolkit.lib.hdinsight;

import com.azure.core.http.policy.HttpLogDetailLevel;
import com.azure.core.http.policy.HttpLogOptions;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.hdinsight.HDInsightManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzService;
import com.microsoft.azure.toolkit.lib.common.model.AbstractAzServiceSubscription;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class AzureHDInsightService extends AbstractAzService<HDInsightServiceSubscription, HDInsightManager> {

    public AzureHDInsightService() {
        super("Microsoft.HDInsight");
    }

    @Nonnull
    public List<SparkClusterNode> clusters() {
        return this.list().stream().flatMap(m -> m.clusters().list().stream()).collect(Collectors.toList());
    }

    @Nonnull
    public SparkClusterModule clusters(@Nonnull String subscriptionId) {
        final HDInsightServiceSubscription rm = get(subscriptionId, null);
        assert rm != null;
        return rm.clusters();
    }

    @Override
    public void refresh() {
        SparkClusterModule.additionalClusterSet.clear();
        super.refresh();
    }

    @Nonnull
    @Override
    protected HDInsightServiceSubscription newResource(@Nonnull HDInsightManager manager) {
        return new HDInsightServiceSubscription(manager.serviceClient().getSubscriptionId(), this);
    }

    @Nullable
    @Override
    protected HDInsightManager loadResourceFromAzure(@Nonnull String subscriptionId, String resourceGroup) {
        final Account account = Azure.az(AzureAccount.class).account();
        final AzureConfiguration config = Azure.az().config();
        final String userAgent = config.getUserAgent();
        final HttpLogDetailLevel logLevel = Optional.ofNullable(config.getLogLevel()).map(HttpLogDetailLevel::valueOf).orElse(HttpLogDetailLevel.NONE);
        final AzureProfile azureProfile = new AzureProfile(null, subscriptionId, account.getEnvironment());
        return HDInsightManager.configure()
                .withHttpClient(AbstractAzServiceSubscription.getDefaultHttpClient())
                .withLogOptions(new HttpLogOptions().setLogLevel(logLevel))
                .withPolicy(AbstractAzServiceSubscription.getUserAgentPolicy(userAgent))
                .authenticate(account.getTokenCredential(subscriptionId), azureProfile);
    }

    @Nonnull
    @Override
    public String getResourceTypeName() {
        return "clusters";
    }

}