/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.connector.aad;

import com.intellij.execution.util.ListTableWithButtons;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.util.ui.ListTableModel;
import com.microsoft.azure.toolkit.intellij.common.AzureFormInputComponent;
import com.microsoft.azure.toolkit.lib.common.form.AzureValidationInfo;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.util.List;
import java.util.stream.Collectors;

public class AzureCallbackUrlTable extends ListTableWithButtons<StringBuilder> implements AzureFormInputComponent<List<StringBuilder>> {

    public AzureCallbackUrlTable() {
        super();
        getTableView().getEmptyText().setText(MessageBundle.message("dialog.identity.ad.register_app.callback_url.emptyText"));
        this.addValidator(this::doValidateValue);
        this.setRequired(true);
    }

    @Override
    protected ListTableModel<StringBuilder> createListModel() {
        return new ListTableModel<>(new NameColumnInfo());
    }

    @Override
    protected StringBuilder createElement() {
        return new StringBuilder();
    }

    @Override
    protected boolean isEmpty(StringBuilder stringBuilder) {
        return stringBuilder.length() == 0;
    }

    @Override
    protected StringBuilder cloneElement(StringBuilder stringBuilder) {
        return new StringBuilder(stringBuilder);
    }

    @Override
    protected boolean canDeleteElement(StringBuilder stringBuilder) {
        return true;
    }

    @Override
    public List<StringBuilder> getValue() {
        return getElements().stream().filter(e -> StringUtils.isNotBlank(e.toString())).collect(Collectors.toList());
    }

    @Override
    public void setValue(List<StringBuilder> val) {
        this.setValues(val);
    }

    @Nonnull
    public AzureValidationInfo doValidateValue() {
        final var urls = this.getValue();
        for (final var url : urls) {
            if (!AzureUtils.isValidCallbackURL(url.toString())) {
                return AzureValidationInfo.error(MessageBundle.message("action.azure.aad.registerApp.callbackURLInvalid", url.toString()), this);
            }
        }

        return AzureValidationInfo.success(this);
    }

    @Override
    public String getLabel() {
        return MessageBundle.message("dialog.identity.ad.register_app.callback_url").replace(":", "");
    }

    @Override
    public JComponent getInputComponent() {
        return this.getComponent();
    }

    private static class NameColumnInfo extends ElementsColumnInfoBase<StringBuilder> {
        protected NameColumnInfo() {
            super("URL");
        }

        @Override
        public void setValue(StringBuilder stringBuilder, @NlsContexts.ListItem String value) {
            stringBuilder.replace(0, stringBuilder.length(), value);
        }

        @Override
        protected @Nullable
        String getDescription(StringBuilder stringBuilder) {
            return null;
        }

        @Override
        public @Nullable
        String valueOf(StringBuilder stringBuilder) {
            return stringBuilder.toString();
        }

        @Override
        public boolean isCellEditable(StringBuilder stringBuilder) {
            return true;
        }
    }
}
