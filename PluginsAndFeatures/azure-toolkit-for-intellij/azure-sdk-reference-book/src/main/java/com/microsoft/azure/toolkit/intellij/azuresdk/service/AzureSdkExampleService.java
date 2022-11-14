/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.azuresdk.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExampleEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureJavaSdkArtifactExampleIndexEntity;
import com.microsoft.azure.toolkit.intellij.azuresdk.model.AzureSdkArtifactEntity;
import com.microsoft.azure.toolkit.lib.common.cache.Cacheable;
import com.microsoft.azure.toolkit.lib.common.cache.Preload;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AzureSdkExampleService {
    private static final ObjectMapper CSV_MAPPER = new CsvMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String JAVA_LIBRARY_EXAMPLE_LIST_CSV = "/java-library-example-list.csv";
    private static final String JAVA_LIBRARY_EXAMPLE_INDEX_CSV = "/java-library-example-index.csv";
    private static final Map<String, String> TEMPLATE_CACHE = new HashMap<>();

    public static String loadSdkTemplate(@Nonnull final AzureJavaSdkArtifactExampleEntity entity) {
        return TEMPLATE_CACHE.computeIfAbsent(entity.getRawUrl(), ignore -> {
            try {
                return IOUtils.toString(new URL(entity.getRawUrl()), Charset.defaultCharset());
            } catch (final IOException e) {
                log.warn("failed to load Azure SDK example", e);
                return null;
            }
        });
    }

    @Nullable
    public static AzureJavaSdkArtifactExampleIndexEntity getSdkExampleIndex(@Nonnull AzureSdkArtifactEntity entity) {
        return loadAzureSDKExample().stream()
                .filter(example -> StringUtils.equalsIgnoreCase(example.getGroupId(), entity.getGroupId())
                        && StringUtils.equalsIgnoreCase(example.getPackageName(), entity.getArtifactId()))
                .findFirst().orElse(null);
    }

    @Preload
    @Cacheable(value = "sdk/examples")
    public static List<AzureJavaSdkArtifactExampleIndexEntity> loadAzureSDKExample() {
        final Map<Integer, List<AzureJavaSdkArtifactExampleEntity>> exampleMap = loadAzureSDKExampleEntities().stream()
                .collect(Collectors.groupingBy(e -> e.getReleaseId(), Collectors.mapping(e -> e, Collectors.toList())));
        final List<AzureJavaSdkArtifactExampleIndexEntity> indexEntities = loadAzureSDKExampleIndex().stream()
                .filter(entity -> StringUtils.equalsIgnoreCase(entity.getLanguage(), "java"))
                .collect(Collectors.toList());
        indexEntities.forEach(entity -> entity.setExamples(exampleMap.get(entity.getId())));
        return indexEntities;
    }

    @Cacheable(value = "java-library-example-index")
    @AzureOperation(name = "sdk.load_library_example_index", type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    public static List<AzureJavaSdkArtifactExampleIndexEntity> loadAzureSDKExampleIndex() {
        final ObjectReader reader = CSV_MAPPER.readerFor(AzureJavaSdkArtifactExampleIndexEntity.class).with(CsvSchema.emptySchema().withHeader());
        try (final InputStream stream = AzureSdkLibraryService.class.getResourceAsStream(JAVA_LIBRARY_EXAMPLE_INDEX_CSV)) {
            final MappingIterator<AzureJavaSdkArtifactExampleIndexEntity> data = reader.readValues(stream);
            return data.readAll().stream().collect(Collectors.toList());
        } catch (final IOException e) {
            log.warn("failed to load Azure SDK example index", e);
        }
        return Collections.emptyList();
    }

    @Cacheable(value = "java-library-example")
    @AzureOperation(name = "sdk.load_library_example", type = AzureOperation.Type.TASK, target = AzureOperation.Target.PLATFORM)
    public static List<AzureJavaSdkArtifactExampleEntity> loadAzureSDKExampleEntities() {
        final ObjectReader reader = CSV_MAPPER.readerFor(AzureJavaSdkArtifactExampleEntity.class).with(CsvSchema.emptySchema().withHeader());
        try (final InputStream stream = AzureSdkLibraryService.class.getResourceAsStream(JAVA_LIBRARY_EXAMPLE_LIST_CSV)) {
            final MappingIterator<AzureJavaSdkArtifactExampleEntity> data = reader.readValues(stream);
            return data.readAll().stream().collect(Collectors.toList());
        } catch (final IOException e) {
            log.warn("failed to load Azure SDK example index", e);
        }
        return Collections.emptyList();
    }
}
