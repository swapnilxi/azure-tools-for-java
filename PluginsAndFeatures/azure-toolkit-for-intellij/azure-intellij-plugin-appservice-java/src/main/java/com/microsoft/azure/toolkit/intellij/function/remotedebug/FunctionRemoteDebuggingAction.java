/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.remotedebug;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.ProgramRunnerUtil;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
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
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.core.FunctionUtils;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FunctionRemoteDebuggingAction {
    public static void startDebugging(@Nonnull FunctionAppBase<?, ?, ?> target, Project project) {
        if (!target.isRemoteDebugEnabled()) {
            showEnableDebuggingMessage(target);
            return;
        }
        addExecutionListener(project);
        executeRunConfiguration(target, project);
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
                            if (beforeRunTask instanceof FunctionPortForwardingTaskProvider.FunctionPortForwarderBeforeRunTask) {
                                ((FunctionPortForwardingTaskProvider.FunctionPortForwarderBeforeRunTask) beforeRunTask).getForwarder().stopForward();
                            }
                        });
                    }
                });
                messageBusConnection.disconnect();
            }
        });
    }

    private static RemoteConfiguration generateRemoteConfiguration(Project project, @Nonnull FunctionAppBase<?, ?, ?> target) {
        final RemoteConfiguration remoteConfig = (RemoteConfiguration) RemoteConfigurationType.getInstance().createTemplateConfiguration(project);
        remoteConfig.PORT = String.valueOf(FunctionUtils.findFreePort());
        remoteConfig.HOST = "localhost";
        remoteConfig.USE_SOCKET_TRANSPORT = true;
        remoteConfig.SERVER_MODE = false;
        remoteConfig.setName(String.format("remote-debug-%s", target.getName()));
        final Module[] modules = ModuleManager.getInstance(project).getModules();
        if (modules.length > 0) {
            remoteConfig.setModule(modules[0]);
        }
        final List<BeforeRunTask<?>> beforeRunTasks = new ArrayList<>();
        beforeRunTasks.add(createPortForwardingTask(remoteConfig, target));
        beforeRunTasks.addAll(remoteConfig.getBeforeRunTasks());
        remoteConfig.setBeforeRunTasks(beforeRunTasks);
        return remoteConfig;
    }

    private static FunctionPortForwardingTaskProvider.FunctionPortForwarderBeforeRunTask createPortForwardingTask(RemoteConfiguration runConfiguration, @Nonnull FunctionAppBase<?, ?, ?> target) {
        final FunctionPortForwardingTaskProvider provider = new FunctionPortForwardingTaskProvider();
        final FunctionPortForwardingTaskProvider.FunctionPortForwarderBeforeRunTask runTask = provider.createTask(runConfiguration);
        Optional.ofNullable(runTask).ifPresent(task -> task.setTarget(target));
        return runTask;
    }

    @AzureOperation(name = "function.start_debug_configuration.app", params = {"target.getName()"}, type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    private static void executeRunConfiguration(@Nonnull FunctionAppBase<?, ?, ?> target, Project project) {
        final RemoteConfiguration remoteConfiguration = generateRemoteConfiguration(project, target);
        final RunManagerImpl managerImpl = new RunManagerImpl(project);
        final RunnerAndConfigurationSettings settings = new RunnerAndConfigurationSettingsImpl(managerImpl, remoteConfiguration, false);
        final RunManagerEx runManager = RunManagerEx.getInstanceEx(project);
        settings.storeInLocalWorkspace();
        runManager.addConfiguration(settings);
        runManager.setSelectedConfiguration(settings);
        ProgramRunnerUtil.executeConfiguration(settings, DefaultDebugExecutor.getDebugExecutorInstance());
    }

    private static void showEnableDebuggingMessage(@Nonnull FunctionAppBase<?, ?, ?> target) {
        final String confirmEnableDebuggingMessage = "Remote debugging should be enabled first before debugging. Would you like to enable it?";
        final Action<FunctionAppBase<?, ?, ?>> enableDebuggingAction = AzureActionManager.getInstance().getAction(FunctionAppActionsContributor.ENABLE_REMOTE_DEBUGGING);
        AzureMessager.getMessager().warning(confirmEnableDebuggingMessage, null,
                new Action<>(new ActionView.Builder("Enable Remote Debugging")) {
                    @Override
                    public void handle(Object source, Object e) {
                        enableDebuggingAction.handle(target, e);
                    }
                });
    }
}
