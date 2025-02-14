/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.file;

import com.microsoft.azure.toolkit.ide.common.component.Node;
import com.microsoft.azure.toolkit.ide.common.component.NodeView;
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase;
import com.microsoft.azure.toolkit.lib.appservice.model.AppServiceFile;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.event.AzureEvent;
import com.microsoft.azure.toolkit.lib.common.event.AzureEventBus;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.DateTimeException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class AppServiceFileNode extends Node<AppServiceFile> {
    public static final String FILE_EXTENSION_ICON_PREFIX = "file/";
    public static final String SITE_WWWROOT = "/site/wwwroot";
    public static final String LOG_FILES = "/LogFiles";

    private final AppServiceFile file;
    private final AppServiceAppBase<?, ?, ?> appService;

    public static AppServiceFile getRootFileNodeForAppService(@Nonnull AppServiceAppBase<?, ?, ?> appService) {
        final AppServiceFile appServiceFile = new AppServiceFile();
        appServiceFile.setName("Files");
        appServiceFile.setPath(SITE_WWWROOT);
        appServiceFile.setMime("inode/directory");
        appServiceFile.setApp(appService);
        return appServiceFile;
    }

    public static AppServiceFile getRootLogNodeForAppService(@Nonnull AppServiceAppBase<?, ?, ?> appService) {
        final AppServiceFile appServiceFile = new AppServiceFile();
        appServiceFile.setName("Logs");
        appServiceFile.setPath(LOG_FILES);
        appServiceFile.setMime("inode/directory");
        appServiceFile.setApp(appService);
        return appServiceFile;
    }

    public AppServiceFileNode(@Nonnull AppServiceFile data) {
        super(data);
        this.file = data;
        this.appService = data.getApp();
        final String actionGroupId = data.getType() == AppServiceFile.Type.DIRECTORY ?
            AppServiceFileActionsContributor.APP_SERVICE_DIRECTORY_ACTIONS : AppServiceFileActionsContributor.APP_SERVICE_FILE_ACTIONS;
        this.actions(actionGroupId);
        if (data.getType() != AppServiceFile.Type.DIRECTORY) {
            this.doubleClickAction(AppServiceFileActionsContributor.APP_SERVICE_FILE_VIEW);
        }
        this.view(new AppServiceFileLabelView(data));
    }

    @Override
    public boolean hasChildren() {
        return file.getType() == AppServiceFile.Type.DIRECTORY;
    }

    @Override
    public List<Node<?>> getChildren() {
        if (file.getType() != AppServiceFile.Type.DIRECTORY) {
            return Collections.emptyList();
        }
        if (!appService.getFormalStatus().isRunning()) {
            AzureMessager.getMessager().warning(AzureString.format("Can not list files for app service with status %s", appService.getStatus()));
            return Collections.emptyList();
        }
        return appService.getFilesInDirectory(file.getPath()).stream()
                .sorted((first, second) -> first.getType() == second.getType() ?
                        StringUtils.compare(first.getName(), second.getName()) :
                        first.getType() == AppServiceFile.Type.DIRECTORY ? -1 : 1)
                .map(AppServiceFileNode::new)
                .collect(Collectors.toList());
    }

    static class AppServiceFileLabelView implements NodeView {
        @Nonnull
        @Getter
        private final AppServiceFile file;
        private final AzureEventBus.EventListener listener;

        @Nullable
        @Setter
        @Getter
        private Refresher refresher;

        public AppServiceFileLabelView(@Nonnull AppServiceFile file) {
            this.file = file;
            this.listener = new AzureEventBus.EventListener(this::onEvent);
            AzureEventBus.on("resource.refreshed.resource", listener);
            this.refreshView();
        }

        private void onEvent(AzureEvent event) {
            final String type = event.getType();
            final Object source = event.getSource();
            if ((source instanceof AppServiceFile && StringUtils.equalsIgnoreCase(((AppServiceFile) source).getFullName(), this.file.getFullName()))) {
                AzureTaskManager.getInstance().runLater(this::refreshChildren);
            }
        }

        @Override
        public String getLabel() {
            return file.getName();
        }

        @Override
        public String getIconPath() {
            final String fileIconName = file.getType() == AppServiceFile.Type.DIRECTORY ?
                    StringUtils.equalsAnyIgnoreCase(file.getPath(), SITE_WWWROOT, LOG_FILES) ? "root" : "folder" :
                    FilenameUtils.getExtension(file.getName());
            return FILE_EXTENSION_ICON_PREFIX + fileIconName;
        }

        @Override
        public String getDescription() {
            if (StringUtils.equalsAnyIgnoreCase(file.getPath(), SITE_WWWROOT, LOG_FILES)) {
                return String.format("Type: %s", file.getMime());
            }
            final String modifiedDateTime = formatDateTime(file.getMtime());
            return file.getType() == AppServiceFile.Type.DIRECTORY ?
                    String.format("Type: %s Date modified: %s", file.getMime(), modifiedDateTime) :
                    String.format("Type: %s Size: %s Date modified: %s", file.getMime(), FileUtils.byteCountToDisplaySize(file.getSize()), modifiedDateTime);
        }

        private static String formatDateTime(@Nullable final String dateTime) {
            try {
                return StringUtils.isEmpty(dateTime) ? "Unknown" : DateTimeFormatter.RFC_1123_DATE_TIME.format(OffsetDateTime.parse(dateTime));
            } catch (final DateTimeException dateTimeException) {
                // swallow exception for parse datetime, return unknown in this case
                return "Unknown";
            }
        }

        @Override
        public void dispose() {
            AzureEventBus.off("resource.refreshed.resource", listener);
            this.refresher = null;
        }
    }
}
