/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.model.rediscache;

import com.microsoft.azure.management.redis.RedisAccessKeys;
import com.microsoft.azure.management.redis.RedisCache;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RedisConnectionPoolsTest {

    @Mock
    private Jedis jedisMock;

    @Mock
    private JedisPool jedisPoolMock;

    @Mock
    private AzureRedisMvpModel azureRedisMvpModelMock;

    @Mock
    private RedisCache redisCacheMock;

    @Mock
    private RedisAccessKeys redisAccessKeysMock;

    private static final String MOCK_SUBSCRIPTION = "00000000-0000-0000-0000-000000000000";
    private static final String MOCK_REDIS_ID = "test-id";
    private static final String MOCK_RETURN_STRING = "RedisTest";
    private static final int MOCK_PORT = 6380;

    private static MockedStatic<AzureRedisMvpModel> azureRedisMvpModelMockedStatic;

    @BeforeClass
    public static void setUpClass() {
        azureRedisMvpModelMockedStatic = mockStatic(AzureRedisMvpModel.class);
    }

    @AfterClass
    public static void tearDownClass() {
        Optional.ofNullable(azureRedisMvpModelMockedStatic).ifPresent(MockedStatic::close);
    }

    @Before
    public void setUp() throws Exception {
        azureRedisMvpModelMockedStatic.when(AzureRedisMvpModel::getInstance).thenReturn(azureRedisMvpModelMock);
        when(azureRedisMvpModelMock.getRedisCache(anyString(), anyString())).thenReturn(redisCacheMock);

        when(redisCacheMock.hostName()).thenReturn(MOCK_RETURN_STRING);
        when(redisCacheMock.keys()).thenReturn(redisAccessKeysMock);
        when(redisAccessKeysMock.primaryKey()).thenReturn(MOCK_RETURN_STRING);
        when(redisCacheMock.sslPort()).thenReturn(MOCK_PORT);
    }

    @After
    public void tearDown() {
        jedisMock = null;
        jedisPoolMock = null;
        azureRedisMvpModelMock = null;
        redisCacheMock = null;
        redisAccessKeysMock = null;
    }


    @Test
    public void testGetAndReleaseJedis() throws Exception {
        try (final MockedConstruction<JedisPool> construction = mockConstruction(JedisPool.class,
                (mock, context) -> when(mock.getResource()).thenReturn(jedisMock))) {
            RedisConnectionPools.getInstance().getJedis(MOCK_SUBSCRIPTION, MOCK_REDIS_ID);
            assert CollectionUtils.isNotEmpty(construction.constructed());
            final JedisPool mockPool = construction.constructed().get(0);
            verify(mockPool, times(1)).getResource();
            RedisConnectionPools.getInstance().releasePool(MOCK_REDIS_ID);
            verify(mockPool, times(1)).destroy();
        }
    }

    @Test
    public void testReleaseNonExistedJedis() {
        // Just release without getJedis
        RedisConnectionPools.getInstance().releasePool(MOCK_REDIS_ID);
        verify(jedisPoolMock, times(0)).destroy();
    }
}
