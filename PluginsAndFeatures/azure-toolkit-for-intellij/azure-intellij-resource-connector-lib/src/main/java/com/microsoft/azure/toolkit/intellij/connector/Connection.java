/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector;

import com.azure.core.credential.TokenCredential;
import com.azure.core.management.profile.AzureProfile;
import com.azure.resourcemanager.resources.fluentcore.arm.ResourceId;
import com.azure.resourcemanager.servicelinker.ServiceLinkerManager;
import com.azure.resourcemanager.servicelinker.models.AzureResource;
import com.azure.resourcemanager.servicelinker.models.ClientType;
import com.azure.resourcemanager.servicelinker.models.LinkerResource;
import com.azure.resourcemanager.servicelinker.models.SecretAuthInfo;
import com.intellij.execution.application.ApplicationConfiguration;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ModuleBasedConfiguration;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.common.runconfig.IWebAppRunConfiguration;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * the <b>{@code resource connection}</b>
 *
 * @param <R> type of the resource consumed by {@link C}
 * @param <C> type of the consumer consuming {@link R},
 *            it can only be {@link ModuleResource} for now({@code v3.52.0})
 * @since 3.52.0
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Connection<R, C> {
    public static final String ENV_PREFIX = "%ENV_PREFIX%";
    private static final String SPRING_BOOT_CONFIGURATION = "com.intellij.spring.boot.run.SpringBootApplicationRunConfiguration";
    @Nonnull
    @EqualsAndHashCode.Include
    protected final Resource<R> resource;
    @Nonnull
    @EqualsAndHashCode.Include
    protected final Resource<C> consumer;
    @Nonnull
    @EqualsAndHashCode.Include
    protected final ConnectionDefinition<R, C> definition;
    @Setter
    @Getter(AccessLevel.NONE)
    private String envPrefix;
    private Map<String, String> env = new HashMap<>();

    /**
     * is this connection applicable for the specified {@code configuration}.<br>
     * - the {@code Connect Azure Resource} before run task will take effect if
     * applicable: the {@link #prepareBeforeRun} & {@link #updateJavaParametersAtRun}
     * will be called.
     *
     * @return true if this connection should intervene the specified {@code configuration}.
     */
    public boolean isApplicableFor(@Nonnull RunConfiguration configuration) {
        final boolean javaAppRunConfiguration = configuration instanceof ApplicationConfiguration;
        final boolean springbootAppRunConfiguration = StringUtils.equals(configuration.getClass().getName(), SPRING_BOOT_CONFIGURATION);
        final boolean azureWebAppRunConfiguration = configuration instanceof IWebAppRunConfiguration;
        if (javaAppRunConfiguration || azureWebAppRunConfiguration || springbootAppRunConfiguration) {
            final Module module = getTargetModule(configuration);
            return Objects.nonNull(module) && Objects.equals(module.getName(), this.consumer.getName());
        }
        return false;
    }

    public Map<String, String> getEnvironmentVariables(final Project project) {
        return this.resource.initEnv(project).entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().replaceAll(Connection.ENV_PREFIX, this.getEnvPrefix()), Map.Entry::getValue));
    }

    /**
     * do some preparation in the {@code Connect Azure Resource} before run task
     * of the {@code configuration}<br>
     */
    @AzureOperation(name = "connector.prepare_before_run", type = AzureOperation.Type.ACTION)
    public boolean prepareBeforeRun(@Nonnull RunConfiguration configuration, DataContext dataContext) {
        try {
            this.env = getEnvironmentVariables(configuration.getProject());
            if (configuration instanceof IConnectionAware) { // set envs for remote deploy
                ((IConnectionAware) configuration).addConnection(this);
            }
            return true;
        } catch (final Throwable e) {
            AzureMessager.getMessager().error(e);
            return false;
        }
    }

    public boolean isResourceConnectionSupported() {
        return this.resource.getDefinition().isResourceConnectionSupported();
    }

    @Nonnull
    public void prepareResourceConnection(final String resourceId) {
        LinkerResource resourceConnection = getResourceConnection(resourceId);
        if (Objects.isNull(resourceConnection)) {
            AzureMessager.getMessager().info("Creating resource connection...");
            createResourceConnection(resourceId, ClientType.JAVA);
            createResourceConnection(resourceId, ClientType.SPRING_BOOT);
            AzureMessager.getMessager().info("Resource connection created.");
        } else {
            AzureMessager.getMessager().info("Resource connection already exists.");
        }
    }

    @Nullable
    public LinkerResource getResourceConnection(final String resourceId) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final ServiceLinkerManager manager = ResourceConnectionManagerHolder.getManager(id.subscriptionId());
        return manager.linkers().list(resourceId).stream()
                .filter(linker -> StringUtils.equalsIgnoreCase(resource.getDataId(), linker.targetService() instanceof AzureResource ?
                        ((AzureResource) linker.targetService()).id() : null))
                .findFirst().orElse(null);
    }

    private LinkerResource createResourceConnection(final String resourceId, final ClientType type) {
        final ResourceId id = ResourceId.fromString(resourceId);
        final ServiceLinkerManager manager = ResourceConnectionManagerHolder.getManager(id.subscriptionId());
        return manager.linkers().define(type.toString() + Utils.getTimestamp())
                .withExistingResourceUri(resourceId)
                .withAuthInfo(new SecretAuthInfo())
                .withClientType(type)
                .withTargetService(new AzureResource().withId(resource.getDataId()))
                .create();
    }

    /**
     * update java parameters exactly before start the {@code configuration}
     */
    public void updateJavaParametersAtRun(@Nonnull RunConfiguration configuration, @Nonnull JavaParameters parameters) {
        // todo: replace with local resource connection?
        if (Objects.nonNull(this.env)) {
            for (final Map.Entry<String, String> entry : this.env.entrySet()) {
                parameters.addEnv(entry.getKey(), entry.getValue());
            }
        }
        if (this.resource.getDefinition() instanceof IJavaAgentSupported) {
            parameters.getVMParametersList()
                    .add(String.format("-javaagent:%s", ((IJavaAgentSupported) this.resource.getDefinition()).getJavaAgent().getAbsolutePath()));
        }
    }

    @Nullable
    private static Module getTargetModule(@Nonnull RunConfiguration configuration) {
        if (configuration instanceof ModuleBasedConfiguration) {
            return ((ModuleBasedConfiguration<?, ?>) configuration).getConfigurationModule().getModule();
        } else if (configuration instanceof IWebAppRunConfiguration) {
            return ((IWebAppRunConfiguration) configuration).getModule();
        }
        return null;
    }

    public String getEnvPrefix() {
        if (StringUtils.isBlank(this.envPrefix)) {
            return this.definition.getResourceDefinition().getDefaultEnvPrefix();
        }
        return this.envPrefix;
    }

    public void write(Element connectionEle) {
        this.getDefinition().write(connectionEle, this);
    }

    public boolean validate(Project project) {
        return this.getDefinition().validate(this, project);
    }

    static class ResourceConnectionManagerHolder {
        static Map<String, ServiceLinkerManager> managers = new HashMap<>();

        static ServiceLinkerManager getManager(String subscriptionId) {
            return managers.computeIfAbsent(subscriptionId, id -> {
                final Account account = Azure.az(AzureAccount.class).getAccount();
                final Subscription subscription = account.getSubscription(subscriptionId);
                final TokenCredential tokenCredential = account.getTokenCredential(subscription.getId());
                final AzureProfile azureProfile = new AzureProfile(subscription.getTenantId(), subscription.getId(), account.getEnvironment());
                return ServiceLinkerManager.configure().authenticate(tokenCredential, azureProfile);
            });
        }
    }
}
