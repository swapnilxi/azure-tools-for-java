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
import com.intellij.ui.HyperlinkLabel;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExampleEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExampleIndexEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.referencebook.components.ExampleComboBox;
import com.microsoft.azure.toolkit.intellij.azuresdk.service.AzureSdkExampleService;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;
import com.microsoft.azure.toolkit.lib.common.action.AzureActionManager;
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
    private EditorTextField viewer;
    private ExampleComboBox cbExample;
    private ActionToolbarImpl toolbar;
    @Getter
    private JPanel pnlRoot;
    private HyperlinkLabel linkRequestMoreExamples;
    private AzureJavaSdkArtifactExampleIndexEntity entity;

    public void setExampleIndex(final AzureJavaSdkArtifactExampleIndexEntity entity) {
        this.entity = entity;
        cbExample.setEntity(entity);
    }

    private void createUIComponents() {
        this.cbExample = createExampleComboBox();
        this.viewer = createExampleEditorTextField();
        this.toolbar = createExampleEditorToolBar();
        this.toolbar.setTargetComponent(this.viewer);

        this.linkRequestMoreExamples = new HyperlinkLabel("Request More Examples");
        this.linkRequestMoreExamples.addHyperlinkListener(e -> AzureActionManager.getInstance().getAction(OPEN_URL).handle(SDK_EXAMPLE_REQUEST_URL));
    }

    private ExampleComboBox createExampleComboBox() {
        final ExampleComboBox result = new ExampleComboBox();
        result.addValueChangedListener(value -> {
            if (value == null) {
                AzureTaskManager.getInstance().runLater(() -> this.viewer.setText(StringUtils.EMPTY));
                return;
            }
            AzureTaskManager.getInstance().runLater(() -> this.viewer.setText("Loading..."));
            AzureTaskManager.getInstance().runInBackground("Loading example", () -> {
                final String example = AzureSdkExampleService.loadSdkTemplate(value);
                AzureTaskManager.getInstance().runLater(() -> this.viewer.setText(example));
            });
        });
        return result;
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
            public void actionPerformed(@NotNull final AnActionEvent e) {
                CopyPasteManager.getInstance().setContents(new StringSelection(viewer.getText()));
            }
        });
        group.add(new AnAction("Browse", "Browse Source Code", IntelliJAzureIcons.getIcon(AzureIcons.Common.OPEN_IN_PORTAL)) {
            @Override
            public void actionPerformed(@NotNull final AnActionEvent e) {
                final AzureJavaSdkArtifactExampleEntity value = cbExample.getValue();
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
