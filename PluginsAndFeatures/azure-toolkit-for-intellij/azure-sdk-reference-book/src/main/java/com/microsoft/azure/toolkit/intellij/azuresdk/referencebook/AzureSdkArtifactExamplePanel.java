/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.referencebook;

import com.intellij.icons.AllIcons;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.impl.ActionToolbarImpl;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.components.ActionLink;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExampleEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExamplesEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.components.ExampleComboBox;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.AzureSdkExampleService;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.datatransfer.StringSelection;
import java.util.Optional;

import static com.microsoft.azure.toolkit.ide.common.action.ResourceCommonActionsContributor.OPEN_URL;
import static com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.AzureSdkArtifactGroupPanel.SDK_EXAMPLE_REQUEST_URL;

public class AzureSdkArtifactExamplePanel {
    private static final String NEED_MORE_SAMPLES = "Report Example Issue";
    private static final String NO_EXAMPLE_TEXT = "// No example available for current library, please click `Report Example Issue` to request more samples.";
    private EditorTextField viewer;
    private ExampleComboBox cbExample;
    private ActionToolbarImpl toolbar;
    @Getter
    private JPanel pnlRoot;
    private ActionLink linkRequestMoreExamples;
    private AzureSdkArtifactEntity artifact;
    private AzureJavaSdkArtifactExamplesEntity examples;

    public void setArtifact(final AzureSdkArtifactEntity artifact) {
        this.artifact = artifact;
        this.examples = AzureSdkExampleService.getArtifactExamples(artifact);
        this.cbExample.setEntity(examples);
    }

    private void createUIComponents() {
        this.cbExample = createExampleComboBox();
        this.viewer = createExampleEditorTextField();
        this.toolbar = createExampleEditorToolBar();
        this.toolbar.setTargetComponent(this.viewer);

        this.linkRequestMoreExamples = new ActionLink(NEED_MORE_SAMPLES);
        this.linkRequestMoreExamples.addActionListener(e -> AzureActionManager.getInstance().getAction(OPEN_URL).handle(SDK_EXAMPLE_REQUEST_URL));
        this.linkRequestMoreExamples.setExternalLinkIcon();
    }

    private ExampleComboBox createExampleComboBox() {
        final ExampleComboBox result = new ExampleComboBox();
        result.addValueChangedListener(value -> {
            if (value == null) {
                AzureTaskManager.getInstance().runLater(() -> this.viewer.setText(StringUtils.EMPTY));
                return;
            }
            if (value == ExampleComboBox.NONE) {
                AzureTaskManager.getInstance().runLater(() -> this.viewer.setText(NO_EXAMPLE_TEXT));
                return;
            }
            selectExample(value);
        });
        return result;
    }

    @AzureOperation("user/sdk.select_example")
    private void selectExample(AzureJavaSdkArtifactExampleEntity value) {
        AzureTaskManager.getInstance().runLater(() -> this.viewer.setText("Loading..."));
        AzureTaskManager.getInstance().runInBackground("Loading example", () -> {
            final String example = AzureSdkExampleService.loadArtifactExample(value);
            AzureTaskManager.getInstance().runLater(() -> this.viewer.setText(example));
        });
    }

    private EditorTextField createExampleEditorTextField() {
        final Project project = ProjectManager.getInstance().getOpenProjects()[0];
        final DocumentImpl document = new DocumentImpl("", true);
        final EditorTextField result = new EditorTextField(document, project, JavaFileType.INSTANCE, true, false);
        result.addSettingsProvider(editor -> { // add scrolling/line number features
            editor.setHorizontalScrollbarVisible(true);
            editor.setVerticalScrollbarVisible(true);
            editor.getSettings().setLineNumbersShown(true);
        });
        return result;
    }

    private ActionToolbarImpl createExampleEditorToolBar() {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.add(new AnAction(ActionsBundle.message("action.$Copy.text"), ActionsBundle.message("action.$Copy.description"), AllIcons.Actions.Copy) {
            @Override
            @AzureOperation("user/sdk.copy_artifact_example")
            public void actionPerformed(@NotNull final AnActionEvent e) {
                OperationContext.action().setTelemetryProperty("artifact", artifact.getArtifactId());
                OperationContext.action().setTelemetryProperty("example_id", String.valueOf(Optional.ofNullable(cbExample.getValue())
                        .filter(v -> v != ExampleComboBox.NONE)
                        .map(AzureJavaSdkArtifactExampleEntity::getId).orElse(-1)));
                CopyPasteManager.getInstance().setContents(new StringSelection(viewer.getText()));
            }
        });
        group.add(new AnAction("Browse", "Browse Source Code", IntelliJAzureIcons.getIcon(AzureIcons.Common.OPEN_IN_PORTAL)) {
            @Override
            @AzureOperation("user/sdk.open_example_in_browser")
            public void actionPerformed(@NotNull final AnActionEvent e) {
                final AzureJavaSdkArtifactExampleEntity value = cbExample.getValue();
                OperationContext.action().setTelemetryProperty("artifact", artifact.getArtifactId());
                OperationContext.action().setTelemetryProperty("example_id", String.valueOf(Optional.ofNullable(cbExample.getValue())
                        .filter(v -> v != ExampleComboBox.NONE)
                        .map(AzureJavaSdkArtifactExampleEntity::getId).orElse(-1)));
                Optional.ofNullable(value).ifPresent(v -> AzureActionManager.getInstance().getAction(OPEN_URL).handle(v.getGithubUrl()));
            }
        });
        final ActionToolbarImpl result = new ActionToolbarImpl("toolbar", group, true);
        result.setForceMinimumSize(true);
        return result;
    }

    public void setVisible(boolean visible) {
        this.pnlRoot.setVisible(visible);
    }
}
