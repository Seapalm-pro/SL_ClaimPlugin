package fr.mrbaguette07.SLclaim.Velocity;

import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Configuration for the Velocity plugin.
 */
public class VelocityConfig {
    
    private final Path dataDirectory;
    private final Logger logger;
    
    private boolean enabled;
    private String redisHost;
    private int redisPort;
    private String redisPassword;
    private int redisDatabase;
    private String redisChannel;
    private List<String> survivalServers;
    private List<String> lobbyServers;
    
    public VelocityConfig(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        
        // Default values
        this.enabled = true;
        this.redisHost = "localhost";
        this.redisPort = 6379;
        this.redisPassword = "";
        this.redisDatabase = 0;
        this.redisChannel = "SLclaim";
        this.survivalServers = new ArrayList<>();
        this.lobbyServers = new ArrayList<>();
    }
    
    @SuppressWarnings("unchecked")
    public void load() {
        Path configPath = dataDirectory.resolve("config.yml");
        
        // Create default config if not exists
        if (!Files.exists(configPath)) {
            createDefaultConfig(configPath);
        }
        
        try (InputStream input = Files.newInputStream(configPath)) {
            Yaml yaml = new Yaml();
            Map<String, Object> data = yaml.load(input);
            
            if (data == null) {
                logger.warn("Config file is empty, using defaults.");
                return;
            }
            
            enabled = getBoolean(data, "enabled", true);
            
            Map<String, Object> redis = (Map<String, Object>) data.getOrDefault("redis", new HashMap<>());
            redisHost = getString(redis, "host", "localhost");
            redisPort = getInt(redis, "port", 6379);
            redisPassword = getString(redis, "password", "");
            redisDatabase = getInt(redis, "database", 0);
            redisChannel = getString(redis, "channel", "SLclaim");
            
            Map<String, Object> servers = (Map<String, Object>) data.getOrDefault("servers", new HashMap<>());
            survivalServers = getStringList(servers, "survival");
            lobbyServers = getStringList(servers, "lobby");
            
        } catch (IOException e) {
            logger.error("Failed to load config", e);
        }
    }
    
    private void createDefaultConfig(Path configPath) {
        String defaultConfig = """
            # SLclaim Velocity Configuration
            
            # Enable/disable the plugin
            enabled: true
            
            # Redis configuration
            redis:
              host: localhost
              port: 6379
              password: ""
              database: 0
              channel: SLclaim
            
            # Server lists
            servers:
              survival:
                - survival-1
                - survival-2
              lobby:
                - lobby-1
                - hub
            
            # Teleportation settings
            teleport:
              # Allow cross-server teleportation to claims
              cross-server: true
              # Delay before teleporting (in seconds)
              delay: 3
            """;
        
        try {
            Files.writeString(configPath, defaultConfig);
        } catch (IOException e) {
            logger.error("Failed to create default config", e);
        }
    }
    
    private String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        return value != null ? value.toString() : defaultValue;
    }
    
    private int getInt(Map<String, Object> map, String key, int defaultValue) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return defaultValue;
    }
    
    private boolean getBoolean(Map<String, Object> map, String key, boolean defaultValue) {
        Object value = map.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return defaultValue;
    }
    
    @SuppressWarnings("unchecked")
    private List<String> getStringList(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof List) {
            return (List<String>) value;
        }
        return new ArrayList<>();
    }
    
    // Getters
    public boolean isEnabled() { return enabled; }
    public String getRedisHost() { return redisHost; }
    public int getRedisPort() { return redisPort; }
    public String getRedisPassword() { return redisPassword; }
    public int getRedisDatabase() { return redisDatabase; }
    public String getRedisChannel() { return redisChannel; }
    public List<String> getSurvivalServers() { return survivalServers; }
    public List<String> getLobbyServers() { return lobbyServers; }
    
    public boolean isSurvivalServer(String serverName) {
        return survivalServers.contains(serverName);
    }
    
    public boolean isLobbyServer(String serverName) {
        return lobbyServers.contains(serverName);
    }
}
