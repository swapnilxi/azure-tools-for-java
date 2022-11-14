/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AzureJavaSdkArtifactExampleEntity {
    private static final String RAW_URL_PREFIX = "https://raw.githubusercontent.com/Azure/azure-rest-api-specs-examples/main/";
    private static final String GITHUB_URL_PREFIX = "https://github.com/Azure/azure-rest-api-specs-examples/tree/main/";

    @JsonProperty("id")
    private int id;
    @JsonProperty("file")
    private String file;
    @JsonProperty("release_id")
    private int releaseId;

    public String getRawUrl() {
        return RAW_URL_PREFIX + file;
    }

    public String getGithubUrl() {
        return GITHUB_URL_PREFIX + file;
    }
}
