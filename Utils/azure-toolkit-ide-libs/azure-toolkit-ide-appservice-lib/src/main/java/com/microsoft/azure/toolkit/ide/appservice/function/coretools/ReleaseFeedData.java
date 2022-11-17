/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.ide.appservice.function.coretools;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@Getter
public class ReleaseFeedData {
    private Map<String, TagData> tags;
    private Map<String, ReleaseData> releases;

    @Getter
    public static class TagData {
        private String release;
        private String releaseQuality;
        private boolean hidden;
    }

    @Getter
    public static class ReleaseData {
        private String templates;
        private List<ReleaseCoreTool> coreTools;
    }

    @Getter
    public static class ReleaseCoreTool {
        @JsonProperty("OS")
        private String os;
        @JsonProperty("Architecture")
        private String architecture;
        private String downloadLink;
        private String sha2;
        private String size;
        @JsonProperty("default")
        private String defaultString;
    }
}
