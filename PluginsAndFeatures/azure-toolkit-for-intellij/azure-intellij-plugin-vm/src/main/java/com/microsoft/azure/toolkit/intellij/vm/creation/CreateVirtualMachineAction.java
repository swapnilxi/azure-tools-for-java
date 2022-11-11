/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.vm.creation;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.cache.CacheManager;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.operation.OperationBundle;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.compute.AzureCompute;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachine;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.VirtualMachineDraft;
import com.microsoft.azure.toolkit.lib.compute.virtualmachine.task.CreateVirtualMachineTask;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class CreateVirtualMachineAction {

    public static final String REOPEN_CREATION_DIALOG = "Reopen Creation Dialog";

    public static void create(@Nonnull Project project, @Nullable final VirtualMachineDraft draft) {
        AzureTaskManager.getInstance().runLater(() -> openDialog(project, draft));
    }

    private static void openDialog(final Project project, final VirtualMachineDraft draft) {
        final VMCreationDialog dialog = new VMCreationDialog(project);
        dialog.setOkActionListener((config) -> {
            dialog.close();
            doCreateVirtualMachine(project, config);
        });
        AzureTaskManager.getInstance().runOnPooledThread(() -> {
            final VirtualMachineDraft defaultData = Optional.ofNullable(draft).orElseGet(() -> {
                final List<Subscription> subs = Azure.az(AzureAccount.class).account().getSelectedSubscriptions();
                final Subscription historySub = CacheManager.getUsageHistory(Subscription.class).peek(subs::contains);
                final Subscription sub = Optional.ofNullable(historySub).orElseGet(() -> subs.get(0));
                final String name = VirtualMachineDraft.generateDefaultName();
                final VirtualMachineDraft vmDraft = Azure.az(AzureCompute.class).virtualMachines(sub.getId()).create(name, "<none>");
                return vmDraft.withDefaultConfig();
            });
            AzureTaskManager.getInstance().runLater(() -> dialog.setValue(defaultData), AzureTask.Modality.ANY);
        });
        dialog.show();
    }

    private static void doCreateVirtualMachine(final Project project, final VirtualMachineDraft draft) {
        final AzureTaskManager tm = AzureTaskManager.getInstance();
        tm.runInBackground(OperationBundle.description("vm.create_vm.vm", draft.getName()), () -> {
            OperationContext.action().setTelemetryProperty("subscriptionId", draft.getSubscriptionId());
            try {
                new CreateVirtualMachineTask(draft).execute();
                CacheManager.getUsageHistory(VirtualMachine.class).push(draft);
            } catch (final Exception e) {
                final Consumer<Object> act = t -> tm.runLater("open dialog", () -> openDialog(project, draft));
                final Action<?> action = new Action<>(act, new ActionView.Builder(REOPEN_CREATION_DIALOG));
                AzureMessager.getMessager().error(e, null, action);
            }
        });
    }
}
