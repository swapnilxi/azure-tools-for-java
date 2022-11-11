/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.messager;

import com.azure.core.http.HttpResponse;
import com.azure.core.http.rest.Response;
import com.azure.core.management.exception.ManagementException;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.ex.ActionUtil;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureHtmlMessage;
import com.microsoft.azure.toolkit.lib.common.messager.IAzureMessage;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public class IntellijAzureMessage extends AzureHtmlMessage {
    @Nullable
    @Getter
    @Setter
    private Project project;

    public IntellijAzureMessage(@Nonnull Type type, @Nonnull AzureString message) {
        super(type, message);
    }

    public IntellijAzureMessage(IAzureMessage raw) {
        super(raw);
    }

    protected String getErrorColor() {
        return "#" + Integer.toHexString(JBColor.RED.getRGB()).substring(2);
    }

    protected String getValueColor() {
        return "#" + Integer.toHexString(JBColor.BLUE.getRGB()).substring(2);
    }
}

class DialogMessage extends IntellijAzureMessage {
    DialogMessage(@Nonnull IAzureMessage original) {
        super(original);
    }

    public String getContent() {
        final String content = super.getContent();
        return String.format("<html>%s</html>", content);
    }

    @Override
    public String getDetails() {
        final String details = super.getDetails();
        if (StringUtils.isNotBlank(details)) {
            final String style = "margin:0;margin-top:2px;padding-left:1px;list-style-type:none;";
            return String.format("<html><ul style='%s'>%s</ul></html>", style, details);
        }
        return "";
    }
}

class NotificationMessage extends IntellijAzureMessage {

    public static final int FORBIDDEN = 403;

    NotificationMessage(@Nonnull IAzureMessage original) {
        super(original);
    }

    public String getContent() {
        if (this.getType() == Type.ERROR) {
            return super.getContent() + this.getDetails();
        }
        return super.getContent();
    }

    @Override
    public String getDetails() {
        final String details = super.getDetails();
        if (StringUtils.isNotBlank(details)) {
            final String style = "margin:0;margin-top:2px;padding-left:0;list-style-type:none;";
            return String.format("<div>Call Stack:</div><ul style='%s'>%s</ul>", style, details);
        }
        return "";
    }

    @Nonnull
    @Override
    public Action<?>[] getActions() {
        final Action<?>[] actions = super.getActions();
        if (payload instanceof Throwable) {
            final Action<?>[] actionFromException = getActionFromException((Throwable) payload, getProject());
            if (ArrayUtils.isNotEmpty(actionFromException)) {
                return ArrayUtils.addAll(actions, actionFromException);
            }
        }
        return super.getActions();
    }

    // todo: make it expandable
    @Nullable
    private static Action<?>[] getActionFromException(Throwable throwable, Project project) {
        final Throwable rootCause = ExceptionUtils.getRootCause(throwable);
        if (rootCause instanceof ManagementException) {
            final int errorCode = Optional.ofNullable((ManagementException) rootCause).map(ManagementException::getResponse)
                    .map(HttpResponse::getStatusCode).orElse(0);
            if (errorCode == FORBIDDEN) {
                final Consumer<Void> consumer = ignore -> {
                    final AnAction action = ActionManager.getInstance().getAction("AzureToolkit.SelectSubscriptions");
                    final DataContext context = dataId -> CommonDataKeys.PROJECT.getName().equals(dataId) ? project : null;
                    AzureTaskManager.getInstance().runLater(() -> ActionUtil.invokeAction(action, context, "AzurePluginErrorNotification", null, null));
                };
                final ActionView.Builder view = new ActionView.Builder("Select Subscription")
                        .title(ignore -> AzureString.fromString("Select subscription")).enabled(ignore -> true);
                final Action<Void> action = new Action<>(consumer, view);
                action.setAuthRequired(false);
                return new Action[]{action};
            }
        }
        return null;
    }
}

