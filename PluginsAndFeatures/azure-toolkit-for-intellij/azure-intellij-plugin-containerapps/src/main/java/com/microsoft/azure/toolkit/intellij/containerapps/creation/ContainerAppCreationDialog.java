/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.containerapps.creation;

import com.azure.resourcemanager.appcontainers.models.EnvironmentVar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBIntSpinner;
import com.intellij.ui.TitledSeparator;
import com.microsoft.applicationinsights.web.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.AzureTextInput;
import com.microsoft.azure.toolkit.intellij.common.EnvironmentVariablesTextFieldWithBrowseButton;
import com.microsoft.azure.toolkit.intellij.common.component.RegionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.SubscriptionComboBox;
import com.microsoft.azure.toolkit.intellij.common.component.resourcegroup.ResourceGroupComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.AzureContainerAppsEnvironmentComboBox;
import com.microsoft.azure.toolkit.intellij.containerapps.component.ImageForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import com.microsoft.azure.toolkit.lib.common.model.Availability;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.containerapps.containerapp.ContainerAppDraft;
import com.microsoft.azure.toolkit.lib.containerapps.environment.ContainerAppsEnvironment;
import com.microsoft.azure.toolkit.lib.containerapps.model.IngressConfig;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.ItemEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ContainerAppCreationDialog extends AzureDialog<ContainerAppDraft.Config> implements AzureForm<ContainerAppDraft.Config> {
    private static final Pattern CONTAINER_APP_NAME_PATTERN = Pattern.compile("^[a-z][a-z0-9\\-]*[a-z0-9]$");
    private static final String CONTAINER_APP_NAME_VALIDATION_MESSAGE = "A name must consist of lower case alphanumeric characters or '-', start with an alphabetic character, and end with an alphanumeric character and cannot have '--'. The length must not be more than 32 characters.";
    private JLabel lblSubscription;
    private SubscriptionComboBox cbSubscription;
    private JLabel lblResourceGroup;
    private ResourceGroupComboBox cbResourceGroup;
    private JLabel lblContainerAppName;
    private AzureTextInput txtContainerAppName;
    private JLabel lblRegion;
    private RegionComboBox cbRegion;
    private JPanel pnlRoot;
    private JPanel pnlBasic;
    private JTabbedPane pnlAppSettings;
    private JCheckBox chkUseQuickStart;
    private JLabel lblIngress;
    private JCheckBox chkIngress;
    private JLabel lblExternalTraffic;
    private JLabel lblTargetPort;
    private JBIntSpinner txtTargetPort;
    private ImageForm pnlContainer;
    private AzureContainerAppsEnvironmentComboBox cbEnvironment;
    private TitledSeparator titleContainerDetails;
    private TitledSeparator titleIngress;
    private EnvironmentVariablesTextFieldWithBrowseButton inputEnv;
    private JLabel lblEnv;
    private JCheckBox chkExternalTraffic;

    public static final ContainerAppDraft.ImageConfig QUICK_START_IMAGE = ContainerAppDraft.ImageConfig.builder()
            .fullImageName("mcr.microsoft.com/azuredocs/containerapps-helloworld:latest").environmentVariables(new ArrayList<>()).build();
    public static final IngressConfig QUICK_START_INGRESS = IngressConfig.builder().enableIngress(true).external(true).targetPort(80).build();

    private final Project project;

    public ContainerAppCreationDialog(final Project project) {
        super(project);
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    @Override
    protected void init() {
        super.init();
        this.cbSubscription.setRequired(true);
        this.cbResourceGroup.setRequired(true);
        this.cbRegion.setRequired(true);
        this.txtContainerAppName.setRequired(true);

        this.txtContainerAppName.addValidator(this::validateContainerAppName);
        this.txtTargetPort.setMin(1);
        this.txtTargetPort.setMax(65535);
        this.cbSubscription.addItemListener(this::onSubscriptionChanged);
        this.cbResourceGroup.addItemListener(e -> this.txtContainerAppName.validateValueAsync()); // trigger validation after resource group changed
        this.chkUseQuickStart.addItemListener(e -> this.onSelectQuickImage(chkUseQuickStart.isSelected()));
        this.chkIngress.addItemListener(e -> this.onSelectIngress(chkIngress.isSelected()));

        this.chkUseQuickStart.setSelected(true);

        this.lblSubscription.setLabelFor(cbSubscription);
        this.lblResourceGroup.setLabelFor(cbResourceGroup);
        this.lblContainerAppName.setLabelFor(txtContainerAppName);
        this.lblRegion.setLabelFor(cbRegion);
    }

    private void onSubscriptionChanged(ItemEvent itemEvent) {
        if (itemEvent.getStateChange() == ItemEvent.SELECTED && itemEvent.getItem() instanceof Subscription) {
            final Subscription subscription = (Subscription) itemEvent.getItem();
            this.cbResourceGroup.setSubscription(subscription);
            this.cbRegion.setSubscription(subscription);
        }
    }

    private AzureValidationInfo validateContainerAppName() {
        final String name = txtContainerAppName.getValue();
        final ContainerAppsEnvironment value = cbEnvironment.getValue();
        if (value != null && !value.isDraftForCreating()) {
            final Availability availability = value.checkContainerAppNameAvailability(name);
            return availability.isAvailable() ? AzureValidationInfo.success(txtContainerAppName) :
                    AzureValidationInfo.error(availability.getUnavailabilityMessage(), txtContainerAppName);
        } else {
            final Matcher matcher = CONTAINER_APP_NAME_PATTERN.matcher(name);
            return matcher.matches() && !StringUtils.contains(name, "--") ? AzureValidationInfo.success(txtContainerAppName) :
                    AzureValidationInfo.error(CONTAINER_APP_NAME_VALIDATION_MESSAGE, txtContainerAppName);
        }
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.cbEnvironment = new AzureContainerAppsEnvironmentComboBox();
        this.cbEnvironment.setRequired(true);

        this.txtTargetPort = new JBIntSpinner(80, 1, 65535);
    }

    private void onSelectQuickImage(boolean useQuickStartImage) {
        if (useQuickStartImage) {
            // set image config
            this.pnlContainer.setValue(QUICK_START_IMAGE);
            // set ingress config
            this.setIngressConfig(QUICK_START_INGRESS);
        }
        // toggle app settings enable status
        this.titleContainerDetails.setEnabled(!useQuickStartImage);
        this.pnlContainer.setEnabled(!useQuickStartImage);
        this.titleIngress.setEnabled(!useQuickStartImage);
        this.lblIngress.setEnabled(!useQuickStartImage);
        this.chkIngress.setEnabled(!useQuickStartImage);
        this.lblExternalTraffic.setEnabled(!useQuickStartImage);
        this.chkExternalTraffic.setEnabled(!useQuickStartImage);
        this.lblTargetPort.setEnabled(!useQuickStartImage);
        this.lblEnv.setEnabled(!useQuickStartImage);
        this.inputEnv.setEnabled(!useQuickStartImage);
        this.txtTargetPort.setEnabled(!useQuickStartImage);
    }

    private void onSelectIngress(boolean enableIngress) {
        this.lblExternalTraffic.setVisible(enableIngress);
        this.chkExternalTraffic.setVisible(enableIngress);
        this.lblTargetPort.setVisible(enableIngress);
        this.txtTargetPort.setVisible(enableIngress);
    }

    private void setIngressConfig(@Nonnull final IngressConfig config) {
        chkIngress.setSelected(config.isEnableIngress());
        chkExternalTraffic.setSelected(config.isExternal());
        txtTargetPort.setValue(config.getTargetPort());
    }

    @Override
    public AzureForm<ContainerAppDraft.Config> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return "Create Azure Container App";
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return pnlRoot;
    }

    @Override
    public ContainerAppDraft.Config getValue() {
        final ContainerAppDraft.Config result = new ContainerAppDraft.Config();
        result.setSubscription(cbSubscription.getValue());
        result.setResourceGroup(cbResourceGroup.getValue());
        result.setName(txtContainerAppName.getValue());
        result.setRegion(cbRegion.getValue());
        result.setEnvironment(cbEnvironment.getValue());
        final ContainerAppDraft.ImageConfig value = pnlContainer.getValue();
        final List<EnvironmentVar> vars = inputEnv.getEnvironmentVariables().entrySet().stream()
                .map(e -> new EnvironmentVar().withName(e.getKey()).withValue(e.getValue()))
                .collect(Collectors.toList());
        Optional.ofNullable(value).ifPresent(config -> config.setEnvironmentVariables(vars));
        result.setImageConfig(pnlContainer.getValue());
        result.setIngressConfig(this.getIngressConfig());
        return result;
    }

    private IngressConfig getIngressConfig() {
        return IngressConfig.builder().enableIngress(chkIngress.isSelected())
                .external(chkExternalTraffic.isSelected())
                .targetPort(txtTargetPort.getNumber()).build();
    }

    @Override
    public void setValue(ContainerAppDraft.Config data) {
        Optional.ofNullable(data.getSubscription()).ifPresent(cbSubscription::setValue);
        Optional.ofNullable(data.getResourceGroup()).ifPresent(cbResourceGroup::setValue);
        Optional.ofNullable(data.getName()).ifPresent(txtContainerAppName::setValue);
        Optional.ofNullable(data.getRegion()).ifPresent(cbRegion::setValue);
        Optional.ofNullable(data.getEnvironment()).ifPresent(cbEnvironment::setValue);
        final ContainerAppDraft.ImageConfig imageConfig = data.getImageConfig();
        if (Objects.isNull(imageConfig)) {
            // use quick start image by default
            onSelectQuickImage(true);
        } else {
            this.pnlContainer.setValue(imageConfig);
            // todo: add logic to set envs
            Optional.ofNullable(data.getIngressConfig()).ifPresent(this::setIngressConfig);
        }
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Arrays.asList(cbSubscription, cbResourceGroup, txtContainerAppName, cbRegion, pnlContainer);
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    private void $$$setupUI$$$() {
    }
}
