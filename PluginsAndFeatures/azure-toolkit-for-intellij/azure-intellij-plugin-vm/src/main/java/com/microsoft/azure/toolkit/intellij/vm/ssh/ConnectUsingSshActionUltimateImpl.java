/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.ssh;

import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.remote.RemoteCredentials;
import com.intellij.remote.RemoteSdkException;
import com.intellij.ssh.config.unified.SshConfig;
import com.intellij.ssh.ui.unified.SshConfigConfigurable;
import com.intellij.ssh.ui.unified.SshUiData;
import com.intellij.ui.AppUIUtil;
import com.jetbrains.plugins.remotesdk.console.SshConsoleOptionsProvider;
import com.jetbrains.plugins.remotesdk.console.SshTerminalCachingRunner;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;

import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.terminal.TerminalTabState;
import org.jetbrains.plugins.terminal.TerminalView;

import javax.annotation.Nonnull;

import java.lang.reflect.InvocationTargetException;

/**
 * connect to selected machine by SSH in Intellij Ultimate.
 */
public class ConnectUsingSshActionUltimateImpl implements ConnectUsingSshAction {
    private static final ConnectUsingSshAction instance = new ConnectUsingSshActionUltimateImpl();
    private static final String SSH_CONNECTION_TITLE = "Connect Using SSH";

    public static ConnectUsingSshAction getInstance() {
        return instance;
    }

    @AzureOperation(name = "vm.connect_using_ssh_ultimate.vm", params = "vm.getName()", type = AzureOperation.Type.ACTION)
    public void connectBySsh(VirtualMachine vm, @Nonnull Project project) {
        final SshConfig existingConfig = AddSshConfigAction.getOrCreateSshConfig(vm, project);
        AzureTaskManager.getInstance().runInBackground(SSH_CONNECTION_TITLE,() ->
                connectToSshUnderProgress(project, existingConfig)
        );
    }

    private void connectToSshUnderProgress(final @NotNull Project project, SshConfig ssh) {
        final SshConsoleOptionsProvider provider = SshConsoleOptionsProvider.getInstance(project);
        final RemoteCredentials sshCredential = new SshUiData(ssh, true);
        final SshTerminalCachingRunner runner = new SshTerminalCachingRunner(project, sshCredential, provider.getCharset());
        try {
            runner.connect();
            AppUIUtil.invokeLaterIfProjectAlive(project, () -> createSshSession(project, ssh, runner));
        } catch (final RemoteSdkException e) {
            AzureMessager.getMessager().warning(e.getMessage(), SSH_CONNECTION_TITLE,
                    openSshConfigurationAction(project, sshCredential));
        }
    }

    @AzureOperation(name = "vm.create_ssh_session_iu", type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    private static void createSshSession(@Nonnull Project project, SshConfig ssh, SshTerminalCachingRunner runner) {
        final TerminalTabState tabState = new TerminalTabState();
        tabState.myTabName = ssh.getName();
        TerminalView.getInstance(project).createNewSession(runner, tabState);
    }

    private Action<?> openSshConfigurationAction(final @NotNull Project project, RemoteCredentials sshCredential) {
        return new Action<>(v -> AzureTaskManager.getInstance().runLater(() -> openSshConfiguration(project, sshCredential)), new ActionView.Builder("Modify SSH Configuration"));
    }

    @AzureOperation(name = "vm.open_ssh_configuration", type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    private static void openSshConfiguration(@Nonnull Project project, RemoteCredentials sshCredential) {
        final SshConfigConfigurable configurable = new SshConfigConfigurable.Main(project);
        ShowSettingsUtil.getInstance().editConfigurable(project, configurable, () -> {
            try {
                MethodUtils.invokeMethod(configurable, true, "select", sshCredential);
            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException e2) {
                AzureMessager.getMessager().error(e2);
            }
        });
    }

}
