/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureJavaSdkArtifactExampleIndexEntity {
    @JsonProperty("id")
    private int id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("language")
    private String language;
    @JsonProperty("tag")
    private String tag;
    @JsonProperty("package")
    private String packageName;
    @JsonProperty("version")
    private String version;
    @JsonProperty("date_epoch")
    private String date_epoch;
    @JsonProperty("date")
    private String date;

    private List<AzureJavaSdkArtifactExampleEntity> examples = new ArrayList<>();

    public String getGroupId() {
        return name.indexOf(":") > 0 ? name.substring(0, name.indexOf(":")) : name;
    }
}
