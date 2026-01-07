package fr.mrbaguette07.SCS.MultiServer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a message sent through Redis for multi-server synchronization.
 */
public class RedisMessage {
    
    // ***************
    // *  Variables  *
    // ***************
    
    /** The type of message */
    private MessageType type;
    
    /** The source server that sent this message */
    private String sourceServer;
    
    /** The target server (null for broadcast) */
    private String targetServer;
    
    /** The UUID of the player involved (if applicable) */
    private String playerUUID;
    
    /** The name of the claim involved (if applicable) */
    private String claimName;
    
    /** Additional data as key-value pairs */
    private Map<String, String> data;
    
    /** Timestamp of when the message was created */
    private long timestamp;
    
    // ******************
    // *  Message Types *
    // ******************
    
    public enum MessageType {
        // Claim operations
        CLAIM_CREATE,
        CLAIM_DELETE,
        CLAIM_UPDATE,
        CLAIM_ADD_CHUNK,
        CLAIM_REMOVE_CHUNK,
        CLAIM_TRANSFER,
        CLAIM_MERGE,
        
        // Member operations
        MEMBER_ADD,
        MEMBER_REMOVE,
        MEMBER_BAN,
        MEMBER_UNBAN,
        
        // Settings operations
        SETTING_UPDATE,
        
        // Player operations
        PLAYER_DATA_UPDATE,
        PLAYER_TELEPORT_REQUEST,
        
        // Server operations
        SERVER_SYNC_REQUEST,
        SERVER_SYNC_RESPONSE,
        CACHE_INVALIDATE,
        
        // Sale operations
        CLAIM_SALE_START,
        CLAIM_SALE_CANCEL,
        CLAIM_SALE_COMPLETE
    }
    
    // ******************
    // *  Constructors  *
    // ******************
    
    /**
     * Default constructor for JSON deserialization.
     */
    public RedisMessage() {
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    /**
     * Constructor for creating a new message.
     *
     * @param type The message type
     * @param sourceServer The source server name
     */
    public RedisMessage(MessageType type, String sourceServer) {
        this.type = type;
        this.sourceServer = sourceServer;
        this.data = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    // *********************
    // *  Builder Methods  *
    // *********************
    
    /**
     * Sets the target server.
     *
     * @param targetServer The target server
     * @return This message instance
     */
    public RedisMessage targetServer(String targetServer) {
        this.targetServer = targetServer;
        return this;
    }
    
    /**
     * Sets the player UUID.
     *
     * @param playerUUID The player UUID
     * @return This message instance
     */
    public RedisMessage playerUUID(UUID playerUUID) {
        this.playerUUID = playerUUID != null ? playerUUID.toString() : null;
        return this;
    }
    
    /**
     * Sets the player UUID from string.
     *
     * @param playerUUID The player UUID string
     * @return This message instance
     */
    public RedisMessage playerUUID(String playerUUID) {
        this.playerUUID = playerUUID;
        return this;
    }
    
    /**
     * Sets the claim name.
     *
     * @param claimName The claim name
     * @return This message instance
     */
    public RedisMessage claimName(String claimName) {
        this.claimName = claimName;
        return this;
    }
    
    /**
     * Adds data to the message.
     *
     * @param key The data key
     * @param value The data value
     * @return This message instance
     */
    public RedisMessage addData(String key, String value) {
        this.data.put(key, value);
        return this;
    }
    
    /**
     * Adds data to the message.
     *
     * @param key The data key
     * @param value The data value (will be converted to string)
     * @return This message instance
     */
    public RedisMessage addData(String key, Object value) {
        this.data.put(key, String.valueOf(value));
        return this;
    }
    
    // *********************
    // *  Getters/Setters  *
    // *********************
    
    public MessageType getType() { return type; }
    public void setType(MessageType type) { this.type = type; }
    
    public String getSourceServer() { return sourceServer; }
    public void setSourceServer(String sourceServer) { this.sourceServer = sourceServer; }
    
    public String getTargetServer() { return targetServer; }
    public void setTargetServer(String targetServer) { this.targetServer = targetServer; }
    
    public String getPlayerUUID() { return playerUUID; }
    public void setPlayerUUID(String playerUUID) { this.playerUUID = playerUUID; }
    
    public UUID getPlayerUUIDAsUUID() {
        return playerUUID != null ? UUID.fromString(playerUUID) : null;
    }
    
    public String getClaimName() { return claimName; }
    public void setClaimName(String claimName) { this.claimName = claimName; }
    
    public Map<String, String> getData() { return data; }
    public void setData(Map<String, String> data) { this.data = data; }
    
    public String getData(String key) { return data.get(key); }
    public String getData(String key, String defaultValue) { return data.getOrDefault(key, defaultValue); }
    
    public int getDataAsInt(String key, int defaultValue) {
        String value = data.get(key);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public long getDataAsLong(String key, long defaultValue) {
        String value = data.get(key);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    public boolean getDataAsBoolean(String key, boolean defaultValue) {
        String value = data.get(key);
        if (value == null) return defaultValue;
        return Boolean.parseBoolean(value);
    }
    
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    
    /**
     * Checks if this message is targeted at a specific server.
     *
     * @param serverName The server name to check
     * @return true if this message is for the specified server or is a broadcast
     */
    public boolean isTargetedAt(String serverName) {
        return targetServer == null || targetServer.equals(serverName);
    }
    
    @Override
    public String toString() {
        return "RedisMessage{" +
                "type=" + type +
                ", sourceServer='" + sourceServer + '\'' +
                ", targetServer='" + targetServer + '\'' +
                ", playerUUID='" + playerUUID + '\'' +
                ", claimName='" + claimName + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}
