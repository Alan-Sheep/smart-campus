package com.campus.smart.utils;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * 封装常用的 Redis 操作，所有方法都做了 try-catch 处理
 * 确保 Redis 宕机时不影响主业务流程
 *
 * @author smart-campus
 */
@Component
public class RedisUtils {

    /**
     * 注入 RedisTemplate，key 为 String 类型，value 为 Object 类型
     * 已在 RedisConfig 中配置了 Jackson 序列化器
     */
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // ============================= 熔断器 =============================

    /**
     * 连续失败次数阈值，超过后进入熔断状态
     */
    private static final int FAIL_THRESHOLD = 3;

    /**
     * 熔断恢复时间（毫秒），熔断后每隔一段时间尝试恢复
     */
    private static final long RECOVER_INTERVAL = 60000L;

    /**
     * 连续失败计数器
     */
    private int failCount = 0;

    /**
     * 是否处于熔断状态
     */
    private volatile boolean circuitBroken = false;

    /**
     * 熔断开始时间
     */
    private long breakStartTime = 0;

    /**
     * 检查是否可以执行 Redis 操作（熔断器判断）
     *
     * @return true 可以执行 false 熔断中
     */
    private boolean isAvailable() {
        if (circuitBroken) {
            // 熔断中，检查是否到了恢复时间
            if (System.currentTimeMillis() - breakStartTime > RECOVER_INTERVAL) {
                // 尝试恢复，重置状态
                circuitBroken = false;
                failCount = 0;
                System.out.println("[Redis] 熔断器尝试恢复");
                return true;
            }
            return false;
        }
        return true;
    }

    /**
     * 记录一次失败
     */
    private void recordFail(String op, String errMsg) {
        failCount++;
        // 只在前几次打印错误，避免刷屏
        if (failCount <= FAIL_THRESHOLD) {
            System.err.println("Redis " + op + " 操作异常 (" + failCount + "/" + FAIL_THRESHOLD + "): " + errMsg);
        }
        if (failCount >= FAIL_THRESHOLD && !circuitBroken) {
            circuitBroken = true;
            breakStartTime = System.currentTimeMillis();
            System.err.println("[Redis] 连续失败 " + FAIL_THRESHOLD + " 次，熔断器已开启，" +
                    (RECOVER_INTERVAL / 1000) + " 秒后尝试恢复。Redis 暂时不可用，将直接走数据库查询。");
        }
    }

    /**
     * 记录一次成功（重置失败计数）
     */
    private void recordSuccess() {
        if (failCount > 0) {
            failCount = 0;
        }
    }

    // ============================= 基础操作 =============================

    /**
     * 设置指定 key 的值
     *
     * @param key   键
     * @param value 值
     * @return true 成功 false 失败
     */
    public boolean set(String key, Object value) {
        if (!isAvailable()) {
            return false;
        }
        try {
            redisTemplate.opsForValue().set(key, value);
            recordSuccess();
            return true;
        } catch (Exception e) {
            recordFail("set", e.getMessage());
            return false;
        }
    }

    /**
     * 获取指定 key 的值
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Object result = key == null ? null : redisTemplate.opsForValue().get(key);
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFail("get", e.getMessage());
            return null;
        }
    }

    /**
     * 删除指定 key
     *
     * @param key 键
     * @return true 成功 false 失败
     */
    public boolean delete(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.delete(key));
        } catch (Exception e) {
            System.err.println("Redis delete 操作异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 批量删除 key
     *
     * @param keys 键集合
     * @return 删除成功的数量
     */
    public long delete(Collection<String> keys) {
        try {
            Long count = redisTemplate.delete(keys);
            return count == null ? 0 : count;
        } catch (Exception e) {
            System.err.println("Redis batch delete 操作异常: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 判断 key 是否存在
     *
     * @param key 键
     * @return true 存在 false 不存在
     */
    public boolean hasKey(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            System.err.println("Redis hasKey 操作异常: " + e.getMessage());
            return false;
        }
    }

    // ============================= 过期时间操作 =============================

    /**
     * 设置指定 key 的过期时间（秒）
     *
     * @param key     键
     * @param seconds 过期时间（秒）
     * @return true 成功 false 失败
     */
    public boolean expire(String key, long seconds) {
        try {
            if (seconds > 0) {
                return Boolean.TRUE.equals(redisTemplate.expire(key, seconds, TimeUnit.SECONDS));
            }
            return true;
        } catch (Exception e) {
            System.err.println("Redis expire 操作异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取指定 key 的过期时间（秒）
     *
     * @param key 键
     * @return 过期时间（秒），-1 表示永不过期，-2 表示 key 不存在
     */
    public long getExpire(String key) {
        try {
            Long expire = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return expire == null ? -2 : expire;
        } catch (Exception e) {
            System.err.println("Redis getExpire 操作异常: " + e.getMessage());
            return -2;
        }
    }

    /**
     * 设置值并指定过期时间（秒）
     *
     * @param key     键
     * @param value   值
     * @param seconds 过期时间（秒）
     * @return true 成功 false 失败
     */
    public boolean setWithExpire(String key, Object value, long seconds) {
        if (!isAvailable()) {
            return false;
        }
        try {
            if (seconds > 0) {
                redisTemplate.opsForValue().set(key, value, seconds, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(key, value);
            }
            recordSuccess();
            return true;
        } catch (Exception e) {
            recordFail("setWithExpire", e.getMessage());
            return false;
        }
    }

    // ============================= 自增自减操作 =============================

    /**
     * 递增
     *
     * @param key   键
     * @param delta 要增加的数量（大于0）
     * @return 增加后的值，失败返回 -1
     */
    public long increment(String key, long delta) {
        try {
            if (delta < 0) {
                throw new RuntimeException("递增因子必须大于0");
            }
            Long result = redisTemplate.opsForValue().increment(key, delta);
            return result == null ? -1 : result;
        } catch (Exception e) {
            System.err.println("Redis increment 操作异常: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 递增 1
     *
     * @param key 键
     * @return 增加后的值，失败返回 -1
     */
    public long increment(String key) {
        return increment(key, 1);
    }

    /**
     * 递减
     *
     * @param key   键
     * @param delta 要减少的数量（大于0）
     * @return 减少后的值，失败返回 -1
     */
    public long decrement(String key, long delta) {
        try {
            if (delta < 0) {
                throw new RuntimeException("递减因子必须大于0");
            }
            Long result = redisTemplate.opsForValue().decrement(key, delta);
            return result == null ? -1 : result;
        } catch (Exception e) {
            System.err.println("Redis decrement 操作异常: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 递减 1
     *
     * @param key 键
     * @return 减少后的值，失败返回 -1
     */
    public long decrement(String key) {
        return decrement(key, 1);
    }

    // ============================= Hash 操作 =============================

    /**
     * Hash 设值
     *
     * @param key   键
     * @param item  项
     * @param value 值
     * @return true 成功 false 失败
     */
    public boolean hSet(String key, String item, Object value) {
        try {
            redisTemplate.opsForHash().put(key, item, value);
            return true;
        } catch (Exception e) {
            System.err.println("Redis hSet 操作异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * Hash 取值
     *
     * @param key  键
     * @param item 项
     * @return 值
     */
    public Object hGet(String key, String item) {
        try {
            return redisTemplate.opsForHash().get(key, item);
        } catch (Exception e) {
            System.err.println("Redis hGet 操作异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * Hash 删除
     *
     * @param key  键
     * @param item 项（可以多个）
     */
    public void hDel(String key, Object... item) {
        try {
            redisTemplate.opsForHash().delete(key, item);
        } catch (Exception e) {
            System.err.println("Redis hDel 操作异常: " + e.getMessage());
        }
    }

    /**
     * 判断 hash 表中是否有该项的值
     *
     * @param key  键
     * @param item 项
     * @return true 存在 false 不存在
     */
    public boolean hHasKey(String key, String item) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForHash().hasKey(key, item));
        } catch (Exception e) {
            System.err.println("Redis hHasKey 操作异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 获取 hash 键对应的所有键值
     *
     * @param key 键
     * @return 对应的多个键值
     */
    public Map<Object, Object> hmGet(String key) {
        try {
            return redisTemplate.opsForHash().entries(key);
        } catch (Exception e) {
            System.err.println("Redis hmGet 操作异常: " + e.getMessage());
            return null;
        }
    }

    // ============================= Set 操作 =============================

    /**
     * 根据 key 获取 Set 中的所有值
     *
     * @param key 键
     * @return Set 集合
     */
    public Set<Object> sGet(String key) {
        try {
            return redisTemplate.opsForSet().members(key);
        } catch (Exception e) {
            System.err.println("Redis sGet 操作异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将数据放入 set 缓存
     *
     * @param key    键
     * @param values 值（可以多个）
     * @return 成功个数
     */
    public long sSet(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().add(key, values);
            return count == null ? 0 : count;
        } catch (Exception e) {
            System.err.println("Redis sSet 操作异常: " + e.getMessage());
            return 0;
        }
    }

    /**
     * 移除值为 value 的 set 元素
     *
     * @param key    键
     * @param values 值（可以多个）
     * @return 移除的个数
     */
    public long sRemove(String key, Object... values) {
        try {
            Long count = redisTemplate.opsForSet().remove(key, values);
            return count == null ? 0 : count;
        } catch (Exception e) {
            System.err.println("Redis sRemove 操作异常: " + e.getMessage());
            return 0;
        }
    }

    // ============================= List 操作 =============================

    /**
     * 获取 list 缓存的内容
     *
     * @param key   键
     * @param start 开始位置
     * @param end   结束位置（0 到 -1 代表所有值）
     * @return List 集合
     */
    public List<Object> lGet(String key, long start, long end) {
        try {
            return redisTemplate.opsForList().range(key, start, end);
        } catch (Exception e) {
            System.err.println("Redis lGet 操作异常: " + e.getMessage());
            return null;
        }
    }

    /**
     * 将 list 放入缓存（从右边放入）
     *
     * @param key   键
     * @param value 值
     * @return true 成功 false 失败
     */
    public boolean lSet(String key, Object value) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            return true;
        } catch (Exception e) {
            System.err.println("Redis lSet 操作异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 将 list 放入缓存并设置过期时间
     *
     * @param key     键
     * @param value   值
     * @param seconds 过期时间（秒）
     * @return true 成功 false 失败
     */
    public boolean lSet(String key, Object value, long seconds) {
        try {
            redisTemplate.opsForList().rightPush(key, value);
            if (seconds > 0) {
                expire(key, seconds);
            }
            return true;
        } catch (Exception e) {
            System.err.println("Redis lSet with expire 操作异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 根据索引修改 list 中的某条数据
     *
     * @param key   键
     * @param index 索引
     * @param value 值
     * @return true 成功 false 失败
     */
    public boolean lUpdateIndex(String key, long index, Object value) {
        try {
            redisTemplate.opsForList().set(key, index, value);
            return true;
        } catch (Exception e) {
            System.err.println("Redis lUpdateIndex 操作异常: " + e.getMessage());
            return false;
        }
    }

    /**
     * 移除 N 个值为 value 的元素
     *
     * @param key   键
     * @param count 移除数量
     * @param value 值
     * @return 移除的个数
     */
    public long lRemove(String key, long count, Object value) {
        try {
            Long result = redisTemplate.opsForList().remove(key, count, value);
            return result == null ? 0 : result;
        } catch (Exception e) {
            System.err.println("Redis lRemove 操作异常: " + e.getMessage());
            return 0;
        }
    }
}
