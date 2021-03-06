package com.limit.common.limiter.bloomfilter;

import com.google.common.hash.Funnels;
import com.google.common.hash.Hashing;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.nio.charset.Charset;

/**
 * 布隆过滤器
 */
public class BloomFilter {

    private static final String keyPrefix = "BloomFilter:";

    private final JedisPool jedisPool;

    private Jedis jedis;

    // bit 数组长度
    private final long numBits;

    // hash 函数个数
    private final int numHashFunctions;

    // 计算 bit 数组长度
    private int optimalNumOfHashFunctions(long n, long m) {
        return Math.max(1, (int) Math.round((double) m / n * Math.log(2)));
    }

    // 为了达到规定的错误率，计算 hash 函数个数
    private long optimalNumOfBits(long n, double p) {
        if (p == 0) {
            p = Double.MIN_VALUE;
        }
        return (long) (-n * Math.log(p) / (Math.log(2) * Math.log(2)));
    }

    /**
     * 构造函数
     */
    public BloomFilter(BloomFilterConfig config) {
        this.numBits = optimalNumOfBits(config.getExpectedInsertions(), config.getFpp());
        this.numHashFunctions = optimalNumOfHashFunctions(config.getExpectedInsertions(), this.numBits);
        this.jedisPool = config.getJedisPool();
    }

    /**
     * 判断 keys 是否存在于集合 where 中
     */
    public boolean isExist(String where, String key) {
        if (jedis == null) {
            jedis = jedisPool.getResource();
        }
        long[] indexs = getIndexs(key);
        boolean result;
        //这里使用了 Redis 管道来降低过滤器运行当中访问 Redis 次数 降低 Redis 并发量
        Pipeline pipeline = jedis.pipelined();
        try {
            for (long index : indexs) {
                pipeline.getbit(realKey(where), index);
            }
            result = !pipeline.syncAndReturnAll().contains(false);
        } finally {
            pipeline.close();
        }
        return result;
    }

    /**
     * 将 key 存入 redis bitmap
     */
    public void put(String where, String key) {
        if (jedis == null) {
            jedis = jedisPool.getResource();
        }
        long[] indexs = getIndexs(key);
        //这里使用了 Redis 管道来降低过滤器运行当中访问 Redis 次数 降低 Redis 并发量
        Pipeline pipeline = jedis.pipelined();
        try {
            for (long index : indexs) {
                pipeline.setbit(realKey(where), index, true);
            }
            pipeline.sync();
        } finally {
            pipeline.close();
        }
    }

    /**
     * 根据 key 获取 bitmap 下标 方法来自 guava
     */
    private long[] getIndexs(String key) {
        long hash1 = hash(key);
        long hash2 = hash1 >>> 16;
        long[] result = new long[numHashFunctions];
        for (int i = 0; i < numHashFunctions; i++) {
            long combinedHash = hash1 + i * hash2;
            if (combinedHash < 0) {
                combinedHash = ~combinedHash;
            }
            result[i] = combinedHash % numBits;
        }
        return result;
    }

    /**
     * 获取一个 hash 值 方法来自 guava
     */
    private long hash(String key) {
        Charset charset = Charset.forName("UTF-8");
        return Hashing.murmur3_128().hashObject(key, Funnels.stringFunnel(charset)).asLong();
    }

    private String realKey(String where) {
        return keyPrefix + where;
    }
}
