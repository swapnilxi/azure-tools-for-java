package com.microsoft.azure.toolkit.intellij.connector;

import javax.annotation.Nonnull;

public interface IConnectionAware {

    void addConnection(@Nonnull final Connection<?, ?> connection);
}
