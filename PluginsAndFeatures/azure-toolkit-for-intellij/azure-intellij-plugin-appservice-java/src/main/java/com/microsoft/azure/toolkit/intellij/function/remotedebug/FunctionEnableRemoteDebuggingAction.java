/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.remotedebug;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.ide.appservice.function.FunctionAppActionsContributor;
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTask;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;

public class FunctionEnableRemoteDebuggingAction {
    private static final String FAILED_TITLE = "Failed to %s remote debugging";
    private static final String CONFIRM_MESSAGE = "Are you sure to %s remote debugging for %s? <p>To learn more about configuration for remote debugging, please refer to our <a href=\"https://aka.ms/func-remotedebug-intellij\">wiki</a>";
    private static final String CONFIRM_DIALOG_TITLE = "%s Remote Debugging";
    private static final String SUCCESS_MESSAGE = "Remote debugging is %sd for app %s successfully";

    public static void enableRemoteDebugging(@Nonnull FunctionAppBase<?, ?, ?> app, @Nullable Project project) {
        toggleDebuggingAction(app, true, project);
    }

    public static void disableRemoteDebugging(@Nonnull FunctionAppBase<?, ?, ?> app, @Nullable Project project) {
        toggleDebuggingAction(app, false, project);
    }

    private static void toggleDebuggingAction(@Nonnull FunctionAppBase<?, ?, ?> app, boolean isEnabled, @Nullable Project project) {
        final IAzureMessager messager = AzureMessager.getMessager();
        if (!Objects.requireNonNull(app.getRuntime()).isWindows()) {
            messager.error("Remote debugging is only supported on Windows.");
            return;
        } else if (app.isRemoteDebugEnabled() == isEnabled) {
            messager.warning(String.format("Remote debugging is already %s for app %s", isEnabled ? "enabled" : "disabled", app.getName()));
            return;
        }
        final String action = isEnabled ? "enable" : "disable";
        final boolean useInput = messager.confirm(String.format(CONFIRM_MESSAGE, action, app.getName()),
                StringUtils.capitalize(String.format(CONFIRM_DIALOG_TITLE, action)));
        if (!useInput) {
            return;
        }
        final AzureString title = AzureString.format("%s remote debugging for app %s", action, app.getName());
        AzureTaskManager.getInstance().runInBackground(new AzureTask<>(project, title, false, () -> {
            try {
                if (isEnabled) {
                    app.enableRemoteDebug();
                    messager.success(String.format(SUCCESS_MESSAGE, action, app.getName()), null, generateDebugAction(app, project));
                } else {
                    app.disableRemoteDebug();
                    messager.success(String.format(SUCCESS_MESSAGE, action, app.getName()));
                }
            } catch (final Exception e) {
                final String errorMessage = e.getMessage();
                messager.error(errorMessage, String.format(FAILED_TITLE, action));
            } finally {
                app.refresh();
            }
        }));
    }

    private static Action<?> generateDebugAction(@Nonnull FunctionAppBase<?, ?, ?> app, @Nullable Project project) {
        final Action<FunctionAppBase<?, ?, ?>> remoteDebuggingAction = AzureActionManager.getInstance().getAction(FunctionAppActionsContributor.REMOTE_DEBUGGING);
        return new Action<>((d, e) -> remoteDebuggingAction.handle(app, e), new ActionView.Builder("Attach Debugger"));
    }
}
