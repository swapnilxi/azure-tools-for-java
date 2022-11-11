/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.arm.action;

import com.intellij.ide.actions.RevealFileAction;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.arm.creation.CreateDeploymentDialog;
import com.microsoft.azure.toolkit.intellij.arm.template.ResourceTemplateViewProvider;
import com.microsoft.azure.toolkit.intellij.arm.update.UpdateDeploymentDialog;
import com.microsoft.azure.toolkit.intellij.common.FileChooser;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.intellij.common.fileexplorer.VirtualFileActions;
import com.microsoft.azure.toolkit.intellij.common.properties.AzureResourceEditorViewManager;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.action.Action;
import com.microsoft.azure.toolkit.lib.common.action.ActionView;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azure.toolkit.lib.resource.ResourceDeployment;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup;
import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

public class DeploymentActions {
    private static final String TEMPLATE_SELECTOR_TITLE = "Choose Where to Save the ARM Template File.";
    private static final String PARAMETERS_SELECTOR_TITLE = "Choose Where to Save the ARM Parameter File.";

    private static final String TEMPLATE_FILE_NAME = "%s.template.json";
    private static final String PARAMETERS_FILE_NAME = "%s.parameters.json";

    public static final String NOTIFY_UPDATE_DEPLOYMENT_SUCCESS = "Update deployment successfully";
    public static final String NOTIFY_UPDATE_DEPLOYMENT_FAIL = "Update deployment failed";

    @AzureOperation(name = "arm.create_deployment_ui.rg", params = {"rg.getName()"}, type = AzureOperation.Type.ACTION)
    public static void createDeployment(@Nonnull final Project project, @Nullable ResourceGroup rg) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final CreateDeploymentDialog dialog = new CreateDeploymentDialog(project, rg);
            dialog.show();
        });
    }

    @AzureOperation(name = "arm.open_template_view.deployment", params = {"deployment.getName()"}, type = AzureOperation.Type.ACTION)
    public static void openTemplateView(@Nonnull final Project project, @Nonnull ResourceDeployment deployment) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final Icon icon = IntelliJAzureIcons.getIcon(AzureIcons.Resources.DEPLOYMENT);
            final String name = ResourceTemplateViewProvider.TYPE;
            final AzureResourceEditorViewManager.AzureResourceFileType type = new AzureResourceEditorViewManager.AzureResourceFileType(name, icon);
            final AzureResourceEditorViewManager manager = new AzureResourceEditorViewManager((resource) -> type);
            manager.showEditor(deployment, project);
        });
    }

    @AzureOperation(name = "arm.update_deployment_ui.deployment", params = {"deployment.getName()"}, type = AzureOperation.Type.ACTION)
    public static void updateDeployment(@Nonnull final Project project, @Nonnull final ResourceDeployment deployment) {
        AzureTaskManager.getInstance().runLater(() -> new UpdateDeploymentDialog(project, deployment).show());
    }

    @AzureOperation(name = "arm.export_template.deployment", params = {"deployment.getName"}, type = AzureOperation.Type.ACTION)
    public static void exportTemplate(@Nonnull final Project project, @Nonnull final ResourceDeployment deployment) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final File file = FileChooser.showFileSaver(TEMPLATE_SELECTOR_TITLE, String.format(TEMPLATE_FILE_NAME, deployment.getName()));
            if (file != null) {
                final String template = deployment.getTemplateAsJson();
                try {
                    doExportTemplate(file, template);
                    final String pattern = "Template of Resource {0} is successfully exported to file {1}.";
                    final AzureString msg = AzureString.format(pattern, deployment.getName(), file.getName());
                    AzureMessager.getMessager().success(msg, null, newOpenInEditorAction(file, project), newShowInExplorerAction(file));
                } catch (final AzureToolkitRuntimeException e) {
                    throw e;
                } catch (final Throwable e) {
                    throw new AzureToolkitRuntimeException(String.format("failed to write template to file \"%s\"", file.getName()), e);
                }
            }
        });
    }

    @AzureOperation(name = "arm.export_template_to_file.file", params = {"file.getName()"}, type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    private static void doExportTemplate(File file, String template) throws IOException {
        IOUtils.write(template, new FileOutputStream(file), Charset.defaultCharset());
    }

    @AzureOperation(name = "arm.export_parameter.deployment", params = {"deployment.getName"}, type = AzureOperation.Type.ACTION)
    public static void exportParameters(@Nonnull final Project project, final ResourceDeployment deployment) {
        Azure.az(AzureAccount.class).account();
        AzureTaskManager.getInstance().runLater(() -> {
            final File file = FileChooser.showFileSaver(PARAMETERS_SELECTOR_TITLE, String.format(PARAMETERS_FILE_NAME, deployment.getName()));
            if (file != null) {
                final String parameters = deployment.getParametersAsJson();
                try {
                    doExportParameters(file, parameters);
                    final String pattern = "Parameters of Resource {0} is successfully exported to file {1}.";
                    final AzureString msg = AzureString.format(pattern, deployment.getName(), file.getName());
                    AzureMessager.getMessager().success(msg, null, newOpenInEditorAction(file, project), newShowInExplorerAction(file));
                } catch (final AzureToolkitRuntimeException e) {
                    throw e;
                } catch (final Throwable e) {
                    throw new AzureToolkitRuntimeException(String.format("failed to write parameters to file \"%s\"", file.getName()), e);
                }
            }
        });
    }

    @AzureOperation(name = "arm.export_parameters_to_file.file", params = {"file.getName()"}, type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    private static void doExportParameters(File file, String parameters) throws IOException {
        IOUtils.write(parameters, new FileOutputStream(file), Charset.defaultCharset());
    }

    private static Action<Void> newShowInExplorerAction(@Nonnull final File dest) {
        return new Action<>(v -> VirtualFileActions.revealInExplorer(dest), new ActionView.Builder(RevealFileAction.getActionName()));
    }

    private static Action<Void> newOpenInEditorAction(@Nonnull final File dest, @Nonnull final Project project) {
        return new Action<>(v -> AzureTaskManager.getInstance().runLater(() -> {
            final FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            final VirtualFile virtualFile = VfsUtil.findFileByIoFile(dest, true);
            VirtualFileActions.openFileInEditor(virtualFile, (a) -> false, () -> {
            }, fileEditorManager);
        }), new ActionView.Builder("Open In Editor"));
    }
}