/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.storm.redis.trident.state;

import org.apache.storm.redis.common.mapper.TupleMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.JedisCluster;
import storm.trident.operation.TridentCollector;
import storm.trident.state.BaseStateUpdater;
import storm.trident.tuple.TridentTuple;

import java.util.List;

public class RedisClusterStateUpdater extends BaseStateUpdater<RedisClusterState> {
    private static final Logger logger = LoggerFactory.getLogger(RedisClusterState.class);

    private final String redisKeyPrefix;
    private final TupleMapper tupleMapper;
    private final int expireIntervalSec;

    public RedisClusterStateUpdater(String redisKeyPrefix, TupleMapper tupleMapper, int expireIntervalSec) {
        this.redisKeyPrefix = redisKeyPrefix;
        this.tupleMapper = tupleMapper;
        if (expireIntervalSec > 0) {
            this.expireIntervalSec = expireIntervalSec;
        } else {
            this.expireIntervalSec = 0;
        }
    }

    @Override
    public void updateState(RedisClusterState redisClusterState, List<TridentTuple> inputs,
                            TridentCollector collector) {

        JedisCluster jedisCluster = null;
        try {
            jedisCluster = redisClusterState.getJedisCluster();
            for (TridentTuple input : inputs) {
                String key = this.tupleMapper.getKeyFromTuple(input);
                String redisKey = key;
                if (redisKeyPrefix != null && redisKeyPrefix.length() > 0) {
                    redisKey = redisKeyPrefix + redisKey;
                }
                String value = this.tupleMapper.getValueFromTuple(input);

                logger.debug("update key[" + key + "] redisKey[" + redisKey + "] value[" + value + "]");

                if (this.expireIntervalSec > 0) {
                    jedisCluster.setex(redisKey, expireIntervalSec, value);
                } else {
                    jedisCluster.set(redisKey, value);
                }
            }
        } finally {
            if (jedisCluster != null) {
                redisClusterState.returnJedisCluster(jedisCluster);
            }
        }
    }
}
