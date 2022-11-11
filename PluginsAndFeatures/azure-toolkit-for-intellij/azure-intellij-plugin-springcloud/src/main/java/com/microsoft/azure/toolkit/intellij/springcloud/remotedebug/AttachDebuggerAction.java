/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.springcloud.remotedebug;

import com.intellij.execution.*;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.impl.RunManagerImpl;
import com.intellij.execution.impl.RunnerAndConfigurationSettingsImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.remote.RemoteConfiguration;
import com.intellij.execution.remote.RemoteConfigurationType;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.MessageBusConnection;
import com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor;
import com.microsoft.azure.toolkit.ide.springcloud.SpringCloudActionsContributor;
import com.microsoft.azure.toolkit.ide.springcloud.portforwarder.SpringPortForwarder;
import com.microsoft.azure.toolkit.intellij.springcloud.component.SpringCloudAppInstanceSelectionDialog;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudApp;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudAppInstance;
import com.microsoft.azure.toolkit.lib.springcloud.SpringCloudDeployment;
import org.apache.commons.collections.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttachDebuggerAction {
    private static final int DEFAULT_PORT = 5005;
    private static final String NO_AVAILABLE_INSTANCES = "No available instances in current app %s.";

    @AzureOperation(name = "springcloud.start_remote_debugging.instance", params = {"appInstance.getName()"}, type = AzureOperation.Type.ACTION)
    public static void startDebugging(@Nonnull SpringCloudAppInstance appInstance, Project project) {
        if (!appInstance.getParent().isRemoteDebuggingEnabled()) {
            showEnableDebuggingMessage(appInstance);
            return;
        }
        addExecutionListener(project);
        executeRunConfiguration(appInstance, project);
        showOpenUrlMessage(appInstance);
    }

    @AzureOperation(name = "springcloud.start_remote_debugging.app", params = {"app.getName()"}, type = AzureOperation.Type.ACTION)
    public static void startDebuggingApp(@Nonnull SpringCloudApp app, Project project) {
        final SpringCloudDeployment deployment = app.getActiveDeployment();
        final List<SpringCloudAppInstance> instances = deployment.getInstances();
        if (CollectionUtils.isEmpty(instances)) {
            AzureMessager.getMessager().error(String.format(NO_AVAILABLE_INSTANCES, app.getName()));
            return;
        }
        AzureTaskManager.getInstance().runLater(() -> {
            final SpringCloudAppInstanceSelectionDialog dialog = new SpringCloudAppInstanceSelectionDialog(project, instances);
            if (dialog.showAndGet()) {
                final SpringCloudAppInstance target = dialog.getInstance();
                startDebugging(target, project);
            }
        });
    }

    public static int getDefaultPort() {
        return DEFAULT_PORT;
    }

    private static void addExecutionListener(Project project) {
        final MessageBusConnection messageBusConnection = project.getMessageBus().connect();
        messageBusConnection.subscribe(ExecutionManager.EXECUTION_TOPIC, new ExecutionListener() {
            @Override
            public void processTerminated(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler, int exitCode) {
                Optional.ofNullable(env.getRunnerAndConfigurationSettings()).ifPresent(settings -> {
                    if (settings.getConfiguration() instanceof RemoteConfiguration) {
                        final List<BeforeRunTask<?>> beforeRunTaskList = settings.getConfiguration().getBeforeRunTasks();
                        beforeRunTaskList.forEach(beforeRunTask -> {
                            if (beforeRunTask instanceof PortForwardingTaskProvider.PortForwarderBeforeRunTask) {
                                Optional.ofNullable(((PortForwardingTaskProvider.PortForwarderBeforeRunTask) beforeRunTask).getForwarder())
                                        .ifPresent(SpringPortForwarder::stopForward);
                            }
                        });
                    }
                });
                messageBusConnection.disconnect();
            }
        });
    }

    private static RemoteConfiguration generateRemoteConfiguration(Project project, SpringCloudAppInstance appInstance) {
        final RemoteConfiguration remoteConfig = (RemoteConfiguration) RemoteConfigurationType.getInstance().createTemplateConfiguration(project);
        remoteConfig.PORT = String.valueOf(DEFAULT_PORT);
        remoteConfig.HOST = "localhost";
        remoteConfig.USE_SOCKET_TRANSPORT = true;
        remoteConfig.SERVER_MODE = false;
        remoteConfig.setName(String.format("remote-debug-%s", appInstance.getName()));
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length > 0) {
            remoteConfig.setModule(modules[0]);
        }
        final List<BeforeRunTask<?>> beforeRunTasks = new ArrayList<>();
        beforeRunTasks.add(createPortForwardingTask(remoteConfig, appInstance));
        beforeRunTasks.addAll(remoteConfig.getBeforeRunTasks());
        remoteConfig.setBeforeRunTasks(beforeRunTasks);
        return remoteConfig;
    }

    private static PortForwardingTaskProvider.PortForwarderBeforeRunTask createPortForwardingTask(RemoteConfiguration runConfiguration, SpringCloudAppInstance appInstance) {
        final PortForwardingTaskProvider provider = new PortForwardingTaskProvider();
        final PortForwardingTaskProvider.PortForwarderBeforeRunTask runTask = provider.createTask(runConfiguration);
        Optional.ofNullable(runTask).ifPresent(task -> task.setAppInstance(appInstance));
        return runTask;
    }

    @AzureOperation(name = "springcloud.start_debug_configuration.instance", params = {"appInstance.getName()"}, type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    private static void executeRunConfiguration(@Nonnull SpringCloudAppInstance appInstance, Project project) {
        final RemoteConfiguration remoteConfiguration = generateRemoteConfiguration(project, appInstance);
        final RunManagerImpl managerImpl = new RunManagerImpl(project);
        final RunnerAndConfigurationSettings settings = new RunnerAndConfigurationSettingsImpl(managerImpl, remoteConfiguration, false);
        final RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
        settings.storeInLocalWorkspace();
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance());
    }

    private static void showEnableDebuggingMessage(@Nonnull SpringCloudAppInstance appInstance) {
        final String confirmEnableDebuggingMessage = "Remote debugging should be enabled first before debugging. Would you like to enable it?";
        final Action<SpringCloudApp> enableDebuggingAction = AzureActionManager.getInstance().getAction(SpringCloudActionsContributor.ENABLE_REMOTE_DEBUGGING);
        AzureMessager.getMessager().warning(confirmEnableDebuggingMessage, null,
                new Action<>(new ActionView.Builder("Enable Remote Debugging")) {
                    @Override
                    public void handle(Object source, Object e) {
                        enableDebuggingAction.handle(appInstance.getParent().getParent(), e);
                    }
        });
    }

    private static void showOpenUrlMessage(@Nonnull SpringCloudAppInstance appInstance) {
        final String attachSuccessMessage = "Debugger is attached to Azure Spring Apps app instance %s successfully";
        final SpringCloudApp app = appInstance.getParent().getParent();
        AzureMessager.getMessager().success(String.format(attachSuccessMessage, appInstance.getName()), null, generateAccessPublicUrlAction(app), generateAccessTestUrlAction(app));
    }

    @Nullable
    private static Action<?> generateAccessPublicUrlAction(@Nonnull SpringCloudApp app) {
        if (app.isPublicEndpointEnabled()) {
            return new Action<>(new ActionView.Builder("Access Public Endpoint")) {
                @Override
                public void handle(Object source, Object e) {
                    AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(app.getApplicationUrl());
                }
            };
        }
        return null;
    }


    private static Action<?> generateAccessTestUrlAction(@Nonnull SpringCloudApp app) {
        return new Action<>(new ActionView.Builder("Access Test Endpoint")) {
            @Override
            public void handle(Object source, Object e) {
                AzureActionManager.getInstance().getAction(ResourceCommonActionsContributor.OPEN_URL).handle(app.getTestUrl());
            }
        };
    }
}
