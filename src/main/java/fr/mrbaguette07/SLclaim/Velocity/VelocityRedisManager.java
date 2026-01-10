package fr.mrbaguette07.SLclaim.Velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.mrbaguette07.SLclaim.MultiServer.RedisMessage;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

/**
 * Manages Redis connections for the Velocity plugin.
 */
public class VelocityRedisManager {
    
    private final SCSVelocityPlugin plugin;
    private final VelocityConfig config;
    private final Gson gson;
    
    private JedisPool jedisPool;
    private JedisPubSub subscriber;
    private Thread subscriberThread;
    private boolean connected;
    
    public VelocityRedisManager(SCSVelocityPlugin plugin, VelocityConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.gson = new GsonBuilder().create();
        this.connected = false;
    }
    
    public boolean connect() {
        try {
            JedisPoolConfig poolConfig = new JedisPoolConfig();
            poolConfig.setMaxTotal(10);
            poolConfig.setMaxIdle(5);
            poolConfig.setMinIdle(1);
            poolConfig.setTestOnBorrow(true);
            
            if (config.getRedisPassword() != null && !config.getRedisPassword().isEmpty()) {
                jedisPool = new JedisPool(
                    poolConfig,
                    config.getRedisHost(),
                    config.getRedisPort(),
                    2000,
                    config.getRedisPassword(),
                    config.getRedisDatabase()
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
            plugin.getLogger().info("Connecté à Redis.");
            
            // Start subscriber
            startSubscriber();
            
            return true;
        } catch (Exception e) {
            plugin.getLogger().error("Échec de la connexion à Redis", e);
            return false;
        }
    }
    
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
        
        plugin.getLogger().info("Déconnecté de Redis.");
    }
    
    public void publish(RedisMessage message) {
        if (!connected || jedisPool == null) return;
        
        try (Jedis jedis = jedisPool.getResource()) {
            String json = gson.toJson(message);
            jedis.publish(config.getRedisChannel(), json);
        } catch (Exception e) {
            plugin.getLogger().error("Échec de la publication du message", e);
        }
    }
    
    private void startSubscriber() {
        subscriber = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    RedisMessage redisMessage = gson.fromJson(message, RedisMessage.class);
                    handleMessage(redisMessage);
                } catch (Exception e) {
                    plugin.getLogger().error("Échec de l'analyse du message Redis", e);
                }
            }
        };
        
        subscriberThread = new Thread(() -> {
            while (connected && !Thread.currentThread().isInterrupted()) {
                try (Jedis jedis = jedisPool.getResource()) {
                    jedis.subscribe(subscriber, config.getRedisChannel());
                } catch (Exception e) {
                    if (connected) {
                        plugin.getLogger().warn("Abonné Redis déconnecté, reconnexion en cours...");
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }, "SLclaim-Velocity-Redis");
        
        subscriberThread.setDaemon(true);
        subscriberThread.start();
    }
    
    private void handleMessage(RedisMessage message) {
        // Handle teleportation requests
        if (message.getType() == RedisMessage.MessageType.PLAYER_TELEPORT_REQUEST) {
            handleTeleportRequest(message);
        }
    }
    
    private void handleTeleportRequest(RedisMessage message) {
        String playerUUID = message.getPlayerUUID();
        String targetServer = message.getData("target_server");
        
        if (playerUUID == null || targetServer == null) return;
        
        plugin.getServer().getPlayer(java.util.UUID.fromString(playerUUID)).ifPresent(player -> {
            plugin.getServer().getServer(targetServer).ifPresent(server -> {
                player.createConnectionRequest(server).fireAndForget();
                plugin.getLogger().info("Téléportation de " + player.getUsername() + " vers " + targetServer);
            });
        });
    }
    
    public boolean isConnected() {
        return connected;
    }
}
