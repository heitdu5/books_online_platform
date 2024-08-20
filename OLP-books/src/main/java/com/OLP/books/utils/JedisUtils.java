package com.OLP.books.utils;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class JedisUtils {
    private static JedisPool jedisPool = null;

    static {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(100);
        config.setMinIdle(10);
        jedisPool = new JedisPool(config,"127.0.0.1");
    }
    public static Jedis getJedis() {
        return  jedisPool.getResource();
    }
}