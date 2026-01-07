package fr.mrbaguette07.SCS.MultiServer;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.bson.Document;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import fr.mrbaguette07.SCS.ClaimMain;
import fr.mrbaguette07.SCS.SLclaim;
import fr.mrbaguette07.SCS.MultiServer.RedisMessage.MessageType;
import fr.mrbaguette07.SCS.Types.Claim;
import fr.mrbaguette07.SCS.Types.CustomSet;

import java.io.File;

/**
 * Main manager for multi-server functionality.
 * Coordinates Redis pub/sub and MongoDB data storage.
 */
public class MultiServerManager {
    
    // ***************
    // *  Variables  *
    // ***************
    
    /** Instance of SLclaim */
    private final SLclaim instance;
    
    /** Multi-server configuration */
    private MultiServerConfig config;
    
    /** Redis manager */
    private RedisManager redisManager;
    
    /** MongoDB manager */
    private MongoDBManager mongoDBManager;
    
    /** Whether multi-server is initialized */
    private boolean initialized;
    
    // ******************
    // *  Constructors  *
    // ******************
    
    /**
     * Constructor for MultiServerManager.
     *
     * @param instance The SLclaim instance.
     */
    public MultiServerManager(SLclaim instance) {
        this.instance = instance;
        this.config = new MultiServerConfig();
        this.initialized = false;
    }
    
    // ********************
    // *  Public Methods  *
    // ********************
    
    /**
     * Initializes the multi-server system.
     *
     * @return CompletableFuture that completes when initialized
     */
    public CompletableFuture<Boolean> initialize() {
        return CompletableFuture.supplyAsync(() -> {
            // Load configuration
            loadConfig();
            
            if (!config.isEnabled()) {
                instance.info("Multi-server mode is disabled.");
                initialized = true;
                return true;
            }
            
            instance.info("Initializing multi-server mode...");
            instance.info("Server name: " + config.getServerName());
            instance.info("Server type: " + config.getServerType().name());
            
            // Initialize MongoDB
            mongoDBManager = new MongoDBManager(instance);
            boolean mongoConnected = mongoDBManager.connect().join();
            
            if (!mongoConnected) {
                instance.info("§cFailed to connect to MongoDB. Multi-server mode disabled.");
                config.setEnabled(false);
                return false;
            }
            
            // Initialize Redis
            redisManager = new RedisManager(instance);
            boolean redisConnected = redisManager.connect().join();
            
            if (!redisConnected) {
                instance.info("§cFailed to connect to Redis. Multi-server mode disabled.");
                mongoDBManager.disconnect();
                config.setEnabled(false);
                return false;
            }
            
            // Set up message handler
            redisManager.setMessageHandler(this::handleMessage);
            
            // Send sync request to other servers
            sendSyncRequest();
            
            initialized = true;
            instance.info("Multi-server mode initialized successfully!");
            
            return true;
        });
    }
    
    /**
     * Shuts down the multi-server system.
     */
    public void shutdown() {
        if (redisManager != null) {
            redisManager.disconnect();
        }
        
        if (mongoDBManager != null) {
            mongoDBManager.disconnect();
        }
        
        initialized = false;
    }
    
    /**
     * Gets the multi-server configuration.
     *
     * @return The configuration
     */
    public MultiServerConfig getConfig() {
        return config;
    }
    
    /**
     * Gets the Redis manager.
     *
     * @return The Redis manager
     */
    public RedisManager getRedisManager() {
        return redisManager;
    }
    
    /**
     * Gets the MongoDB manager.
     *
     * @return The MongoDB manager
     */
    public MongoDBManager getMongoDBManager() {
        return mongoDBManager;
    }
    
    /**
     * Checks if multi-server is enabled and initialized.
     *
     * @return true if enabled and initialized
     */
    public boolean isEnabled() {
        return config.isEnabled() && initialized;
    }
    
    /**
     * Checks if claiming is allowed on this server.
     *
     * @return true if claiming is allowed
     */
    public boolean canClaim() {
        return config.canClaim();
    }
    
    /**
     * Checks if this is a lobby server.
     *
     * @return true if lobby server
     */
    public boolean isLobbyServer() {
        return config.isLobbyServer();
    }
    
    // *************************
    // *  Synchronization API  *
    // *************************
    
    /**
     * Broadcasts a claim creation to other servers.
     *
     * @param claim The created claim
     * @param ownerUUID The owner's UUID
     */
    public void broadcastClaimCreate(Claim claim, UUID ownerUUID) {
        if (!isEnabled()) return;
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.CLAIM_CREATE, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claim.getName())
            .addData("id_claim", claim.getId())
            .addData("owner_name", claim.getOwner())
            .addData("world", claim.getLocation().getWorld().getName());
        
        redisManager.publish(message);
    }
    
    /**
     * Broadcasts a claim deletion to other servers.
     *
     * @param ownerUUID The owner's UUID
     * @param claimId The claim ID
     * @param claimName The claim name
     */
    public void broadcastClaimDelete(UUID ownerUUID, int claimId, String claimName) {
        if (!isEnabled()) return;
        
        // Delete from MongoDB
        mongoDBManager.deleteClaim(ownerUUID, claimId);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.CLAIM_DELETE, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claimName)
            .addData("id_claim", claimId);
        
        redisManager.publish(message);
    }
    
    /**
     * Broadcasts a claim update to other servers.
     *
     * @param claim The updated claim
     * @param ownerUUID The owner's UUID
     */
    public void broadcastClaimUpdate(Claim claim, UUID ownerUUID) {
        if (!isEnabled()) return;
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.CLAIM_UPDATE, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claim.getName())
            .addData("id_claim", claim.getId());
        
        redisManager.publish(message);
    }
    
    /**
     * Broadcasts a chunk addition to other servers.
     *
     * @param claim The claim
     * @param ownerUUID The owner's UUID
     * @param chunk The added chunk info (world;x;z)
     */
    public void broadcastChunkAdd(Claim claim, UUID ownerUUID, String chunk) {
        if (!isEnabled()) return;
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.CLAIM_ADD_CHUNK, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claim.getName())
            .addData("id_claim", claim.getId())
            .addData("chunk", chunk);
        
        redisManager.publish(message);
    }
    
    /**
     * Broadcasts a chunk removal to other servers.
     *
     * @param claim The claim
     * @param ownerUUID The owner's UUID
     * @param chunk The removed chunk info (world;x;z)
     */
    public void broadcastChunkRemove(Claim claim, UUID ownerUUID, String chunk) {
        if (!isEnabled()) return;
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.CLAIM_REMOVE_CHUNK, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claim.getName())
            .addData("id_claim", claim.getId())
            .addData("chunk", chunk);
        
        redisManager.publish(message);
    }
    
    /**
     * Broadcasts a member addition to other servers.
     *
     * @param claim The claim
     * @param ownerUUID The owner's UUID
     * @param memberUUID The added member's UUID
     */
    public void broadcastMemberAdd(Claim claim, UUID ownerUUID, UUID memberUUID) {
        if (!isEnabled()) return;
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.MEMBER_ADD, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claim.getName())
            .addData("id_claim", claim.getId())
            .addData("member_uuid", memberUUID.toString());
        
        redisManager.publish(message);
    }
    
    /**
     * Broadcasts a member removal to other servers.
     *
     * @param claim The claim
     * @param ownerUUID The owner's UUID
     * @param memberUUID The removed member's UUID
     */
    public void broadcastMemberRemove(Claim claim, UUID ownerUUID, UUID memberUUID) {
        if (!isEnabled()) return;
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.MEMBER_REMOVE, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claim.getName())
            .addData("id_claim", claim.getId())
            .addData("member_uuid", memberUUID.toString());
        
        redisManager.publish(message);
    }
    
    /**
     * Broadcasts a setting update to other servers.
     *
     * @param claim The claim
     * @param ownerUUID The owner's UUID
     * @param setting The setting name
     * @param category The setting category
     * @param value The new value
     */
    public void broadcastSettingUpdate(Claim claim, UUID ownerUUID, String setting, String category, boolean value) {
        if (!isEnabled()) return;
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.SETTING_UPDATE, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claim.getName())
            .addData("id_claim", claim.getId())
            .addData("setting", setting)
            .addData("category", category)
            .addData("value", value);
        
        redisManager.publish(message);
    }
    
    /**
     * Broadcasts a claim sale start to other servers.
     *
     * @param claim The claim
     * @param ownerUUID The owner's UUID
     * @param price The sale price
     */
    public void broadcastSaleStart(Claim claim, UUID ownerUUID, long price) {
        if (!isEnabled()) return;
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.CLAIM_SALE_START, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claim.getName())
            .addData("id_claim", claim.getId())
            .addData("price", price);
        
        redisManager.publish(message);
    }
    
    /**
     * Broadcasts a claim sale cancellation to other servers.
     *
     * @param claim The claim
     * @param ownerUUID The owner's UUID
     */
    public void broadcastSaleCancel(Claim claim, UUID ownerUUID) {
        if (!isEnabled()) return;
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID);
        
        // Broadcast via Redis
        RedisMessage message = new RedisMessage(MessageType.CLAIM_SALE_CANCEL, config.getServerName())
            .playerUUID(ownerUUID)
            .claimName(claim.getName())
            .addData("id_claim", claim.getId());
        
        redisManager.publish(message);
    }
    
    /**
     * Requests a cache invalidation on other servers.
     */
    public void broadcastCacheInvalidate() {
        if (!isEnabled()) return;
        
        RedisMessage message = new RedisMessage(MessageType.CACHE_INVALIDATE, config.getServerName());
        redisManager.publish(message);
    }
    
    // *********************
    // *  Private Methods  *
    // *********************
    
    /**
     * Loads the multi-server configuration from file.
     */
    private void loadConfig() {
        File configFile = new File(instance.getDataFolder(), "multiserver.yml");
        
        if (!configFile.exists()) {
            instance.saveResource("multiserver.yml", false);
        }
        
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(configFile);
        
        // Load general settings
        config.setEnabled(fileConfig.getBoolean("enabled", false));
        config.setServerName(fileConfig.getString("server-name", "server-1"));
        
        String serverTypeStr = fileConfig.getString("server-type", "STANDALONE");
        try {
            config.setServerType(ServerType.valueOf(serverTypeStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            instance.info("§cInvalid server type: " + serverTypeStr + ". Using STANDALONE.");
            config.setServerType(ServerType.STANDALONE);
        }
        
        // Load server lists
        config.setSurvivalServers(fileConfig.getStringList("servers.survival"));
        config.setLobbyServers(fileConfig.getStringList("servers.lobby"));
        
        // Load Redis configuration
        config.setRedisHost(fileConfig.getString("redis.host", "localhost"));
        config.setRedisPort(fileConfig.getInt("redis.port", 6379));
        config.setRedisPassword(fileConfig.getString("redis.password", ""));
        config.setRedisSSL(fileConfig.getBoolean("redis.ssl", false));
        config.setRedisDatabase(fileConfig.getInt("redis.database", 0));
        config.setRedisChannel(fileConfig.getString("redis.channel", "SLclaim"));
        
        // Load MongoDB configuration
        config.setMongoConnectionString(fileConfig.getString("mongodb.connection-string", "mongodb://localhost:27017"));
        config.setMongoDatabaseName(fileConfig.getString("mongodb.database", "SLclaim"));
        config.setMongoClaimsCollection(fileConfig.getString("mongodb.collections.claims", "claims"));
        config.setMongoPlayersCollection(fileConfig.getString("mongodb.collections.players", "players"));
    }
    
    /**
     * Sends a sync request to other servers.
     */
    private void sendSyncRequest() {
        RedisMessage message = new RedisMessage(MessageType.SERVER_SYNC_REQUEST, config.getServerName());
        redisManager.publish(message);
    }
    
    /**
     * Handles an incoming Redis message.
     *
     * @param message The message to handle
     */
    private void handleMessage(RedisMessage message) {
        // Check if message is targeted at this server
        if (!message.isTargetedAt(config.getServerName())) {
            return;
        }
        
        switch (message.getType()) {
            case CLAIM_CREATE:
                handleClaimCreate(message);
                break;
            case CLAIM_DELETE:
                handleClaimDelete(message);
                break;
            case CLAIM_UPDATE:
                handleClaimUpdate(message);
                break;
            case CLAIM_ADD_CHUNK:
            case CLAIM_REMOVE_CHUNK:
                handleChunkChange(message);
                break;
            case MEMBER_ADD:
            case MEMBER_REMOVE:
                handleMemberChange(message);
                break;
            case SETTING_UPDATE:
                handleSettingUpdate(message);
                break;
            case CLAIM_SALE_START:
            case CLAIM_SALE_CANCEL:
            case CLAIM_SALE_COMPLETE:
                handleSaleChange(message);
                break;
            case SERVER_SYNC_REQUEST:
                handleSyncRequest(message);
                break;
            case CACHE_INVALIDATE:
                handleCacheInvalidate(message);
                break;
            default:
                break;
        }
    }
    
    /**
     * Handles a claim creation message.
     */
    private void handleClaimCreate(RedisMessage message) {
        UUID ownerUUID = message.getPlayerUUIDAsUUID();
        int claimId = message.getDataAsInt("id_claim", -1);
        
        if (ownerUUID == null || claimId == -1) return;
        
        // Reload claim from MongoDB
        reloadClaimFromMongo(ownerUUID, claimId);
    }
    
    /**
     * Handles a claim deletion message.
     */
    private void handleClaimDelete(RedisMessage message) {
        UUID ownerUUID = message.getPlayerUUIDAsUUID();
        String claimName = message.getClaimName();
        
        if (ownerUUID == null || claimName == null) return;
        
        // Remove from local cache
        Claim claim = instance.getMain().getClaimByName(claimName, ownerUUID);
        if (claim != null) {
            instance.getMain().removeClaimFromCache(claim);
        }
    }
    
    /**
     * Handles a claim update message.
     */
    private void handleClaimUpdate(RedisMessage message) {
        UUID ownerUUID = message.getPlayerUUIDAsUUID();
        int claimId = message.getDataAsInt("id_claim", -1);
        
        if (ownerUUID == null || claimId == -1) return;
        
        // Reload claim from MongoDB
        reloadClaimFromMongo(ownerUUID, claimId);
    }
    
    /**
     * Handles a chunk change message.
     */
    private void handleChunkChange(RedisMessage message) {
        UUID ownerUUID = message.getPlayerUUIDAsUUID();
        int claimId = message.getDataAsInt("id_claim", -1);
        
        if (ownerUUID == null || claimId == -1) return;
        
        // Reload claim from MongoDB
        reloadClaimFromMongo(ownerUUID, claimId);
    }
    
    /**
     * Handles a member change message.
     */
    private void handleMemberChange(RedisMessage message) {
        UUID ownerUUID = message.getPlayerUUIDAsUUID();
        int claimId = message.getDataAsInt("id_claim", -1);
        
        if (ownerUUID == null || claimId == -1) return;
        
        // Reload claim from MongoDB
        reloadClaimFromMongo(ownerUUID, claimId);
    }
    
    /**
     * Handles a setting update message.
     */
    private void handleSettingUpdate(RedisMessage message) {
        UUID ownerUUID = message.getPlayerUUIDAsUUID();
        int claimId = message.getDataAsInt("id_claim", -1);
        
        if (ownerUUID == null || claimId == -1) return;
        
        // Reload claim from MongoDB
        reloadClaimFromMongo(ownerUUID, claimId);
    }
    
    /**
     * Handles a sale change message.
     */
    private void handleSaleChange(RedisMessage message) {
        UUID ownerUUID = message.getPlayerUUIDAsUUID();
        int claimId = message.getDataAsInt("id_claim", -1);
        
        if (ownerUUID == null || claimId == -1) return;
        
        // Reload claim from MongoDB
        reloadClaimFromMongo(ownerUUID, claimId);
    }
    
    /**
     * Handles a sync request message.
     */
    private void handleSyncRequest(RedisMessage message) {
        // Another server is starting up and requesting sync
        // We can send our data or acknowledge the request
        instance.info("Received sync request from server: " + message.getSourceServer());
    }
    
    /**
     * Handles a cache invalidation message.
     */
    private void handleCacheInvalidate(RedisMessage message) {
        // Reload all claims from MongoDB
        instance.info("Received cache invalidation from server: " + message.getSourceServer());
        reloadAllClaimsFromMongo();
    }
    
    /**
     * Reloads a specific claim from MongoDB.
     *
     * @param ownerUUID The owner's UUID
     * @param claimId The claim ID
     */
    private void reloadClaimFromMongo(UUID ownerUUID, int claimId) {
        mongoDBManager.getClaim(ownerUUID, claimId).thenAccept(doc -> {
            if (doc == null) return;
            
            instance.executeSync(() -> {
                try {
                    // Parse and update local cache
                    Claim existingClaim = instance.getMain().getClaimById(ownerUUID, claimId);
                    
                    if (existingClaim != null) {
                        // Update existing claim
                        updateClaimFromDocument(existingClaim, doc);
                    } else {
                        // Create new claim from document
                        createClaimFromDocument(doc);
                    }
                } catch (Exception e) {
                    instance.info("§cFailed to reload claim from MongoDB: " + e.getMessage());
                }
            });
        });
    }
    
    /**
     * Reloads all claims from MongoDB.
     */
    public void reloadAllClaimsFromMongo() {
        mongoDBManager.getAllClaims().thenAccept(docs -> {
            instance.executeSync(() -> {
                instance.getMain().clearAll();
                
                for (Document doc : docs) {
                    try {
                        createClaimFromDocument(doc);
                    } catch (Exception e) {
                        instance.info("§cFailed to load claim from MongoDB: " + e.getMessage());
                    }
                }
                
                instance.info("Loaded " + docs.size() + " claims from MongoDB.");
            });
        });
    }
    
    /**
     * Updates an existing claim from a MongoDB document.
     *
     * @param claim The claim to update
     * @param doc The MongoDB document
     */
    private void updateClaimFromDocument(Claim claim, Document doc) {
        claim.setName(doc.getString("claim_name"));
        claim.setDescription(doc.getString("claim_description"));
        claim.setSale(doc.getBoolean("for_sale", false));
        claim.setPrice(doc.getLong("sale_price"));
        
        // Update members
        String membersStr = doc.getString("members");
        Set<UUID> members = new HashSet<>();
        if (membersStr != null && !membersStr.isEmpty()) {
            for (String uuidStr : membersStr.split(";")) {
                try {
                    members.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        claim.setMembers(members);
        
        // Update bans
        String bansStr = doc.getString("bans");
        Set<UUID> bans = new HashSet<>();
        if (bansStr != null && !bansStr.isEmpty()) {
            for (String uuidStr : bansStr.split(";")) {
                try {
                    bans.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        claim.setBans(bans);
        
        // Update permissions
        String permissionsStr = doc.getString("permissions");
        if (permissionsStr != null) {
            claim.setPermissions(instance.getMain().deserializePermissions(permissionsStr));
        }
    }
    
    /**
     * Creates a new claim from a MongoDB document.
     *
     * @param doc The MongoDB document
     */
    private void createClaimFromDocument(Document doc) {
        String worldName = doc.getString("world_name");
        World world = Bukkit.getWorld(worldName);
        
        if (world == null) {
            // Try to create the world
            world = Bukkit.createWorld(new WorldCreator(worldName));
            if (world == null) {
                instance.info("§cCannot load claim: world " + worldName + " does not exist.");
                return;
            }
        }
        
        // Parse chunks
        String chunksStr = doc.getString("chunks");
        Set<Chunk> chunks = new HashSet<>();
        if (chunksStr != null && !chunksStr.isEmpty()) {
            for (String chunkStr : chunksStr.split(";")) {
                String[] parts = chunkStr.split(",");
                if (parts.length == 3) {
                    World chunkWorld = Bukkit.getWorld(parts[0]);
                    if (chunkWorld != null) {
                        int x = Integer.parseInt(parts[1]);
                        int z = Integer.parseInt(parts[2]);
                        chunks.add(chunkWorld.getChunkAt(x, z));
                    }
                }
            }
        }
        
        if (chunks.isEmpty()) return;
        
        // Parse location
        String locationStr = doc.getString("location");
        String[] locParts = locationStr.split(";");
        Location location = new Location(
            world,
            Double.parseDouble(locParts[1]),
            Double.parseDouble(locParts[2]),
            Double.parseDouble(locParts[3]),
            Float.parseFloat(locParts[4]),
            Float.parseFloat(locParts[5])
        );
        
        // Parse members
        String membersStr = doc.getString("members");
        Set<UUID> members = new HashSet<>();
        if (membersStr != null && !membersStr.isEmpty()) {
            for (String uuidStr : membersStr.split(";")) {
                try {
                    members.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        // Parse bans
        String bansStr = doc.getString("bans");
        Set<UUID> bans = new HashSet<>();
        if (bansStr != null && !bansStr.isEmpty()) {
            for (String uuidStr : bansStr.split(";")) {
                try {
                    bans.add(UUID.fromString(uuidStr));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        
        // Parse permissions
        String permissionsStr = doc.getString("permissions");
        Map<String, java.util.LinkedHashMap<String, Boolean>> permissions = 
            instance.getMain().deserializePermissions(permissionsStr);
        
        UUID ownerUUID = UUID.fromString(doc.getString("owner_uuid"));
        
        Claim claim = new Claim(
            ownerUUID,
            chunks,
            doc.getString("owner_name"),
            members,
            location,
            doc.getString("claim_name"),
            doc.getString("claim_description"),
            permissions,
            doc.getBoolean("for_sale", false),
            doc.getLong("sale_price"),
            bans,
            doc.getInteger("id_claim")
        );
        
        // Add to cache
        instance.getMain().addClaimToCache(claim);
    }
}
