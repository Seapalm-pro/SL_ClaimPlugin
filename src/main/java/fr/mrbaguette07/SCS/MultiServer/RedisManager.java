package fr.mrbaguette07.SCS.MultiServer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.mrbaguette07.SCS.SLclaim;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 * Manages Redis connections and pub/sub messaging for multi-server synchronization.
 */
public class RedisManager {
    
    // ***************
    // *  Variables  *
    // ***************
    
    /** Instance of SLclaim */
    private final SLclaim instance;
    
    /** Jedis connection pool */
    private JedisPool jedisPool;
    
    /** Subscriber for pub/sub */
    private JedisPubSub subscriber;
    
    /** Thread for pub/sub listener */
    private Thread subscriberThread;
    
    /** Executor for async operations */
    private final ExecutorService executor;
    
    /** Gson instance for JSON serialization */
    private final Gson gson;
    
    /** Message handler for incoming messages */
    private Consumer<RedisMessage> messageHandler;
    
    /** Whether the Redis connection is active */
    private boolean connected;
    
    // ******************
    // *  Constructors  *
    // ******************
    
    /**
     * Constructor for RedisManager.
     *
     * @param instance The SLclaim instance.
     */
    public RedisManager(SLclaim instance) {
        this.instance = instance;
        this.executor = Executors.newCachedThreadPool();
        this.gson = new GsonBuilder().create();
        this.connected = false;
    }
    
    // ********************
    // *  Public Methods  *
    // ********************
    
    /**
     * Connects to Redis server.
     *
     * @return CompletableFuture that completes when connected
     */
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MultiServerConfig config = instance.getMultiServerManager().getConfig();
                
                JedisPoolConfig poolConfig = new JedisPoolConfig();
                poolConfig.setMaxTotal(10);
                poolConfig.setMaxIdle(5);
                poolConfig.setMinIdle(1);
                poolConfig.setTestOnBorrow(true);
                poolConfig.setTestOnReturn(true);
                poolConfig.setTestWhileIdle(true);
                
                if (config.getRedisPassword() != null && !config.getRedisPassword().isEmpty()) {
                    jedisPool = new JedisPool(
                        poolConfig,
                        config.getRedisHost(),
                        config.getRedisPort(),
                        2000,
                        config.getRedisPassword(),
                        config.getRedisDatabase(),
                        config.isRedisSSL()
                    );
                } else {
                    jedisPool = new JedisPool(
                        poolConfig,
                        config.getRedisHost(),
                        config.getRedisPort(),
                        2000,
                        null,
                        config.getRedisDatabase()
                    );
                }
                
                // Test connection
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.ping();
                }
                
                connected = true;
                instance.info("Redis connection established.");
                
                // Start subscriber
                startSubscriber();
                
                return true;
            } catch (Exception e) {
                instance.info("§cFailed to connect to Redis: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    /**
     * Disconnects from Redis server.
     */
    public void disconnect() {
        connected = false;
        
        if (subscriber != null) {
            try {
                subscriber.unsubscribe();
            } catch (Exception ignored) {}
        }
        
        if (subscriberThread != null) {
            subscriberThread.interrupt();
        }
        
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.close();
        }
        
        executor.shutdown();
        
        instance.info("Redis connection closed.");
    }
    
    /**
     * Publishes a message to Redis.
     *
     * @param message The message to publish
     * @return CompletableFuture that completes when published
     */
    public CompletableFuture<Void> publish(RedisMessage message) {
        return CompletableFuture.runAsync(() -> {
            if (!connected || jedisPool == null) {
                return;
            }
            
            try (Jedis jedis = jedisPool.getResource()) {
                String channel = instance.getMultiServerManager().getConfig().getRedisChannel();
                String json = gson.toJson(message);
                jedis.publish(channel, json);
            } catch (Exception e) {
                instance.info("§cFailed to publish Redis message: " + e.getMessage());
            }
        }, executor);
    }
    
    /**
     * Sets the message handler for incoming messages.
     *
     * @param handler The handler consumer
     */
    public void setMessageHandler(Consumer<RedisMessage> handler) {
        this.messageHandler = handler;
    }
    
    /**
     * Checks if Redis is connected.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * Gets the Jedis pool.
     *
     * @return The Jedis pool
     */
    public JedisPool getJedisPool() {
        return jedisPool;
    }
    
    // *********************
    // *  Private Methods  *
    // *********************
    
    /**
     * Starts the pub/sub subscriber.
     */
    private void startSubscriber() {
        String channel = instance.getMultiServerManager().getConfig().getRedisChannel();
        String serverName = instance.getMultiServerManager().getConfig().getServerName();
        
        subscriber = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    RedisMessage redisMessage = gson.fromJson(message, RedisMessage.class);
                    
                    // Ignore messages from self
                    if (redisMessage.getSourceServer().equals(serverName)) {
                        return;
                    }
                    
                    if (messageHandler != null) {
                        // Handle message on main thread if needed
                        instance.executeSync(() -> messageHandler.accept(redisMessage));
                    }
                } catch (Exception e) {
                    instance.info("§cFailed to parse Redis message: " + e.getMessage());
                }
            }
            
            @Override
            public void onSubscribe(String channel, int subscribedChannels) {
                instance.info("Subscribed to Redis channel: " + channel);
            }
            
            @Override
            public void onUnsubscribe(String channel, int subscribedChannels) {
                instance.info("Unsubscribed from Redis channel: " + channel);
            }
        };
        
        subscriberThread = new Thread(() -> {
            while (connected && !Thread.currentThread().isInterrupted()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(subscriber, channel);
                } catch (Exception e) {
                    if (connected) {
                        instance.info("§cRedis subscriber disconnected, reconnecting in 5 seconds...");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }, "SCS-Redis-Subscriber");
        
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }
}
