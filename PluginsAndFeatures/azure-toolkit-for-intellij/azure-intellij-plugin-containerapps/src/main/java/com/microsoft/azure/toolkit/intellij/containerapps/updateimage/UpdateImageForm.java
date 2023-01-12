/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.updateimage;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.intellij.icons.AllIcons;
import com.microsoft.azure.toolkit.intellij.common.AzureCommentLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormJPanel;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.containerapps.component.ContainerAppComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.ImageForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerApp;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.model.RevisionMode;
import lombok.Data;
import lombok.Getter;

import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UpdateImageForm extends JPanel implements AzureFormJPanel<UpdateImageForm.UpdateImageConfig> {
    @Getter
    private JPanel contentPanel;
    private ContainerAppComboBox selectorApp;
    private JPanel formImageContainer;
    private AzureCommentLabel commentApp;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private ImageForm imageForm;

    public UpdateImageForm() {
        super();
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.selectorApp.addItemListener(this::onAppChanged);
        this.selectorApp.setRequired(true);
        this.commentApp.setVisible(false);
        this.commentApp.setIconWithAlignment(AllIcons.General.Warning, SwingConstants.LEFT, SwingConstants.CENTER);
    }

    @Override
    public synchronized UpdateImageConfig getValue() {
        final UpdateImageConfig config = new UpdateImageConfig();
        config.setApp(this.selectorApp.getValue());
        config.setImage(this.imageForm.getValue());
        final Map<String, String> envVarsMap = this.inputEnv.getEnvironmentVariables();
        final List<EnvironmentVar> vars = envVarsMap.entrySet().stream()
            .map(e -> new EnvironmentVar().withName(e.getKey()).withValue(e.getValue()))
            .collect(Collectors.toList());
        config.getImage().setEnvironmentVariables(vars);
        return config;
    }

    @Override
    public void setValue(final UpdateImageConfig config) {
        this.imageForm.setValue(config.getImage());
        final ContainerApp app = config.getApp();
        this.setApp(app);
    }

    public void setApp(ContainerApp app) {
        this.selectorApp.setValue(app);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.imageForm,
            this.selectorApp
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    private void onAppChanged(final ItemEvent e) {
        this.commentApp.setVisible(false);
        if (e.getStateChange() == ItemEvent.SELECTED) {
            final ContainerApp app = (ContainerApp) e.getItem();
            if (app.hasUnsupportedFeatures()) {
                final RevisionMode revisionsMode = app.revisionModel();
                final String message = revisionsMode == RevisionMode.SINGLE ?
                    "This will overwrite the active revision and unsupported features in IntelliJ will be lost." :
                    "Unsupported features in IntelliJ will be lost in the new revision.";
                this.commentApp.setText(message);
                this.commentApp.setVisible(true);
            }
        }
    }

    private void createUIComponents() {
    }

    @Data
    public static class UpdateImageConfig {
        private ContainerApp app;
        private ContainerAppDraft.ImageConfig image;
    }
}
