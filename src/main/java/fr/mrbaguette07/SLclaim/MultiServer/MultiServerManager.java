package fr.mrbaguette07.SLclaim.MultiServer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import fr.mrbaguette07.SLclaim.ClaimMain;
import fr.mrbaguette07.SLclaim.SLclaim;
import fr.mrbaguette07.SLclaim.MultiServer.RedisMessage.MessageType;
import fr.mrbaguette07.SLclaim.Types.Claim;
import fr.mrbaguette07.SLclaim.Types.CustomSet;

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
    
    /** Map des serveurs en ligne et leur dernier heartbeat */
    private final Map<String, Long> onlineServers = new ConcurrentHashMap<>();
    
    /** Map des joueurs en attente de RTP après connexion */
    private final Map<UUID, Long> pendingRtpPlayers = new ConcurrentHashMap<>();
    
    /** Map des joueurs en attente de téléportation à un claim après connexion (UUID -> ClaimTeleportData) */
    private final Map<UUID, ClaimTeleportData> pendingClaimTpPlayers = new ConcurrentHashMap<>();
    
    /** Délai maximum pour considérer un serveur comme hors ligne (30 secondes) */
    private static final long HEARTBEAT_TIMEOUT = 30000;
    
    /**
     * Data class for pending claim teleport.
     */
    public static class ClaimTeleportData {
        public final String ownerName;
        public final String claimName;
        public final long timestamp;
        
        public ClaimTeleportData(String ownerName, String claimName) {
            this.ownerName = ownerName;
            this.claimName = claimName;
            this.timestamp = System.currentTimeMillis();
        }
    }
    
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
                instance.info("Le mode multi-serveur est désactivé.");
                initialized = true;
                return true;
            }
            
            instance.info("Initialisation du mode multi-serveur...");
            instance.info("Nom du serveur : " + config.getServerName());
            instance.info("Type de serveur : " + config.getServerType().name());
            
            // Initialize MongoDB
            mongoDBManager = new MongoDBManager(instance);
            boolean mongoConnected = mongoDBManager.connect().join();
            
            if (!mongoConnected) {
                instance.info("§cÉchec de la connexion à MongoDB. Mode multi-serveur désactivé.");
                config.setEnabled(false);
                return false;
            }
            
            // Initialize Redis
            redisManager = new RedisManager(instance);
            boolean redisConnected = redisManager.connect().join();
            
            if (!redisConnected) {
                instance.info("§cÉchec de la connexion à Redis. Mode multi-serveur désactivé.");
                mongoDBManager.disconnect();
                config.setEnabled(false);
                return false;
            }
            
            // Set up message handler
            redisManager.setMessageHandler(this::handleMessage);
            
            // Send sync request to other servers
            sendSyncRequest();
            
            // Note: Server status is now managed by Velocity via SERVER_HEARTBEAT messages
            
            initialized = true;
            instance.info("Mode multi-serveur initialisé avec succès !");
            
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
    
    // ************************
    // *  Server Status System (via Velocity)  *
    // ************************
    
    /**
     * Updates the online status of a server (called when receiving status from Velocity).
     *
     * @param serverName The server name
     * @param isOnline Whether the server is online
     */
    public void updateServerStatus(String serverName, boolean isOnline) {
        boolean wasOnline = onlineServers.containsKey(serverName);
        
        if (isOnline) {
            onlineServers.put(serverName, System.currentTimeMillis());
            // Only log if status changed from offline to online
            if (!wasOnline) {
                instance.info("§aServeur " + serverName + " est maintenant EN LIGNE");
            }
        } else {
            if (onlineServers.remove(serverName) != null) {
                instance.info("§cServeur " + serverName + " est maintenant HORS LIGNE");
            }
        }
    }
    
    /**
     * Checks if a server is online (based on Velocity status).
     *
     * @param serverName The server name to check
     * @return true if the server is online
     */
    public boolean isServerOnline(String serverName) {
        if (serverName.equals(config.getServerName())) {
            return true; // Ce serveur est toujours en ligne
        }
        Long lastUpdate = onlineServers.get(serverName);
        if (lastUpdate == null) {
            return false;
        }
        // Si pas de mise à jour depuis plus de 30 secondes, considérer hors ligne
        return (System.currentTimeMillis() - lastUpdate) < HEARTBEAT_TIMEOUT;
    }
    
    /**
     * Gets a list of online survival servers.
     *
     * @return List of online survival server names
     */
    public List<String> getOnlineSurvivalServers() {
        List<String> survivalServers = config.getSurvivalServers();
        List<String> onlineSurvival = new ArrayList<>();
        
        for (String server : survivalServers) {
            if (isServerOnline(server)) {
                onlineSurvival.add(server);
            }
        }
        
        return onlineSurvival;
    }
    
    /**
     * Gets a list of online lobby servers.
     *
     * @return List of online lobby server names
     */
    public List<String> getOnlineLobbyServers() {
        List<String> lobbyServers = config.getLobbyServers();
        List<String> onlineLobbies = new ArrayList<>();
        
        for (String server : lobbyServers) {
            if (isServerOnline(server)) {
                onlineLobbies.add(server);
            }
        }
        
        return onlineLobbies;
    }
    
    /**
     * Gets a map of all known server statuses.
     *
     * @return Map of server name to last update timestamp
     */
    public Map<String, Long> getOnlineServers() {
        return new HashMap<>(onlineServers);
    }
    
    /**
     * Gets a set of all player names currently online on any server in the network.
     * This is used to determine which claim owners are online in multi-server mode.
     *
     * @return Set of online player names across all servers
     */
    public Set<String> getAllOnlinePlayerNames() {
        Set<String> onlinePlayerNames = new HashSet<>();
        
        Bukkit.getOnlinePlayers().forEach(p -> onlinePlayerNames.add(p.getName()));

        return onlinePlayerNames;
    }
    
    // ************************
    // *  Pending RTP System  *
    // ************************
    
    /**
     * Marks a player for RTP after they connect to this server.
     *
     * @param playerUUID The player's UUID
     * @param targetServer The server name where the player will be sent
     */
    public void sendPendingRtp(UUID playerUUID, String targetServer) {
        if (!isEnabled()) return;
        
        RedisMessage message = new RedisMessage(MessageType.PLAYER_RTP_PENDING, config.getServerName())
            .playerUUID(playerUUID)
            .targetServer(targetServer)
            .addData("timestamp", System.currentTimeMillis());
        
        redisManager.publish(message);
        instance.info("RTP: Envoi de pending RTP pour " + playerUUID + " vers " + targetServer);
    }
    
    /**
     * Checks if a player has a pending RTP.
     *
     * @param playerUUID The player's UUID
     * @return true if the player has a pending RTP
     */
    public boolean hasPendingRtp(UUID playerUUID) {
        Long timestamp = pendingRtpPlayers.get(playerUUID);
        if (timestamp == null) {
            return false;
        }
        // Le pending RTP expire après 60 secondes
        if (System.currentTimeMillis() - timestamp > 60000) {
            pendingRtpPlayers.remove(playerUUID);
            return false;
        }
        return true;
    }
    
    /**
     * Consumes the pending RTP for a player.
     *
     * @param playerUUID The player's UUID
     * @return true if there was a pending RTP
     */
    public boolean consumePendingRtp(UUID playerUUID) {
        return pendingRtpPlayers.remove(playerUUID) != null;
    }
    
    /**
     * Adds a pending RTP for a player (called when receiving Redis message).
     *
     * @param playerUUID The player's UUID
     */
    private void addPendingRtp(UUID playerUUID) {
        pendingRtpPlayers.put(playerUUID, System.currentTimeMillis());
        instance.info("RTP: Pending RTP ajouté pour " + playerUUID);
    }

    // *******************************
    // *  Pending Claim TP System    *
    // *******************************
    
    /**
     * Sends a pending claim teleport to the target server.
     *
     * @param playerUUID The player's UUID
     * @param targetServer The server name where the player will be sent
     * @param ownerName The claim owner's name
     * @param claimName The claim's name
     */
    public void sendPendingClaimTp(UUID playerUUID, String targetServer, String ownerName, String claimName) {
        if (!isEnabled()) return;
        
        RedisMessage message = new RedisMessage(MessageType.PLAYER_CLAIM_TP_PENDING, config.getServerName())
            .playerUUID(playerUUID)
            .targetServer(targetServer)
            .claimName(claimName)
            .addData("owner_name", ownerName)
            .addData("timestamp", System.currentTimeMillis());
        
        redisManager.publish(message);
        instance.info("ClaimTP: Envoi de pending claim TP pour " + playerUUID + " vers " + targetServer + " (claim: " + claimName + ")");
    }
    
    /**
     * Checks if a player has a pending claim teleport.
     *
     * @param playerUUID The player's UUID
     * @return true if the player has a pending claim teleport
     */
    public boolean hasPendingClaimTp(UUID playerUUID) {
        ClaimTeleportData data = pendingClaimTpPlayers.get(playerUUID);
        if (data == null) {
            return false;
        }
        // Le pending claim TP expire après 60 secondes
        if (System.currentTimeMillis() - data.timestamp > 60000) {
            pendingClaimTpPlayers.remove(playerUUID);
            return false;
        }
        return true;
    }
    
    /**
     * Gets and consumes the pending claim teleport for a player.
     *
     * @param playerUUID The player's UUID
     * @return The claim teleport data, or null if none
     */
    public ClaimTeleportData consumePendingClaimTp(UUID playerUUID) {
        return pendingClaimTpPlayers.remove(playerUUID);
    }
    
    /**
     * Adds a pending claim teleport for a player (called when receiving Redis message).
     *
     * @param playerUUID The player's UUID
     * @param ownerName The claim owner's name
     * @param claimName The claim's name
     */
    private void addPendingClaimTp(UUID playerUUID, String ownerName, String claimName) {
        pendingClaimTpPlayers.put(playerUUID, new ClaimTeleportData(ownerName, claimName));
        instance.info("ClaimTP: Pending claim TP ajouté pour " + playerUUID + " (claim: " + claimName + " de " + ownerName + ")");
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
        
        instance.info("§aBroadcasting claim creation: " + claim.getName() + " for owner " + claim.getOwner());
        
        // Save to MongoDB
        mongoDBManager.saveClaim(claim, ownerUUID).thenAccept(success -> {
            if (success) {
                instance.info("§aClaim " + claim.getName() + " saved to MongoDB successfully");
            } else {
                instance.info("§cFailed to save claim " + claim.getName() + " to MongoDB");
            }
        });
        
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
        
        updateMultiServerConfigWithDefaults(configFile);
        
        FileConfiguration fileConfig = YamlConfiguration.loadConfiguration(configFile);
        
        // Load general settings
        config.setEnabled(fileConfig.getBoolean("enabled", false));
        config.setServerName(fileConfig.getString("server-name", "server-1"));
        
        String serverTypeStr = fileConfig.getString("server-type", "STANDALONE");
        try {
            config.setServerType(ServerType.valueOf(serverTypeStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            instance.info("§cType de serveur invalide : " + serverTypeStr + ". Utilisation de STANDALONE.");
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
        
        // Load expulsion configuration
        config.setExpulsionTeleportToLobby(fileConfig.getBoolean("expulsion.teleport-to-lobby", false));
        config.setExpulsionMessage(fileConfig.getString("expulsion.expulsion-message", "&cVous avez été expulsé du claim et transféré au lobby."));
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
        // Gérer les heartbeats en premier (ils ne sont pas ciblés)
        if (message.getType() == MessageType.SERVER_HEARTBEAT) {
            handleHeartbeat(message);
            return;
        }
        
        // Gérer les pending RTP (ciblés sur ce serveur)
        if (message.getType() == MessageType.PLAYER_RTP_PENDING) {
            handlePendingRtp(message);
            return;
        }
        
        // Gérer les pending claim TP (ciblés sur ce serveur)
        if (message.getType() == MessageType.PLAYER_CLAIM_TP_PENDING) {
            handlePendingClaimTp(message);
            return;
        }
        
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
     * Handles a heartbeat message from another server or status from Velocity.
     */
    private void handleHeartbeat(RedisMessage message) {
        String sourceServer = message.getSourceServer();
        
        // Check if this is a server status message from Velocity
        if ("velocity-proxy".equals(sourceServer)) {
            String serverName = (String) message.getData("server_name");
            Object isOnlineObj = message.getData("is_online");
            
            if (serverName != null && isOnlineObj != null) {
                boolean isOnline = Boolean.parseBoolean(isOnlineObj.toString());
                updateServerStatus(serverName, isOnline);
            }
        } else {
            // Legacy heartbeat from another server
            onlineServers.put(sourceServer, System.currentTimeMillis());
        }
    }
    
    /**
     * Handles a pending RTP message.
     */
    private void handlePendingRtp(RedisMessage message) {
        // Vérifier si ce message est destiné à ce serveur
        String targetServer = message.getTargetServer();
        if (targetServer != null && !targetServer.equals(config.getServerName())) {
            return;
        }
        
        UUID playerUUID = message.getPlayerUUIDAsUUID();
        if (playerUUID != null) {
            addPendingRtp(playerUUID);
        }
    }
    
    /**
     * Handles a pending claim teleport message.
     */
    private void handlePendingClaimTp(RedisMessage message) {
        // Vérifier si ce message est destiné à ce serveur
        String targetServer = message.getTargetServer();
        if (targetServer != null && !targetServer.equals(config.getServerName())) {
            return;
        }
        
        UUID playerUUID = message.getPlayerUUIDAsUUID();
        String claimName = message.getClaimName();
        String ownerName = (String) message.getData("owner_name");
        
        if (playerUUID != null && claimName != null && ownerName != null) {
            addPendingClaimTp(playerUUID, ownerName, claimName);
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
        instance.info("Demande de synchronisation reçue du serveur : " + message.getSourceServer());
    }
    
    /**
     * Handles a cache invalidation message.
     */
    private void handleCacheInvalidate(RedisMessage message) {
        // Reload all claims from MongoDB
        instance.info("Invalidation du cache reçue du serveur : " + message.getSourceServer());
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
                    instance.info("§cÉchec du rechargement du claim depuis MongoDB : " + e.getMessage());
                }
            });
        });
    }
    
    /**
     * Reloads all claims from MongoDB.
     * This is used by LOBBY servers to get claims from survival servers.
     */
    public void reloadAllClaimsFromMongo() {
        if (config.isLobbyServer()) {
            mongoDBManager.getAllClaims().thenAccept(docs -> {
                instance.executeSync(() -> {
                    instance.info(docs.size() + " claims disponibles dans MongoDB.");
                });
            });
        } else {
            instance.info("§eSynchronisation des claims locaux vers MongoDB...");
            syncLocalClaimsToMongo();
        }
    }
    
    /**
     * Synchronizes all local claims to MongoDB.
     * This is called at startup for survival servers.
     */
    public void syncLocalClaimsToMongo() {
        instance.executeAsync(() -> {
            CustomSet<Claim> allClaims = instance.getMain().getAllClaims();
            int count = 0;
            
            for (Claim claim : allClaims) {
                try {
                    UUID ownerUUID = claim.getUUID();
                    mongoDBManager.saveClaim(claim, ownerUUID).join();
                    count++;
                } catch (Exception e) {
                    instance.info("§cÉchec de la synchronisation du claim " + claim.getName() + " : " + e.getMessage());
                }
            }
            
            int finalCount = count;
            instance.executeSync(() -> {
                instance.info("§a" + finalCount + " claims synchronisés vers MongoDB.");
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
                instance.info("§cImpossible de charger le claim : le monde " + worldName + " n'existe pas.");
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
    
    /**
     * Updates the multiserver.yml config file with missing keys from default.
     * This ensures new config options are added without overwriting user settings.
     *
     * @param configFile The multiserver.yml file
     */
    private void updateMultiServerConfigWithDefaults(File configFile) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            java.io.InputStream defConfigStream = instance.getResource("multiserver.yml");
            if (defConfigStream == null) return;
            
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(defConfigStream));
            
            boolean changed = false;
            
            // Add missing keys
            for (String key : defConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defConfig.get(key));
                    changed = true;
                }
            }
            
            if (changed) {
                config.save(configFile);
            }
        } catch (Exception e) {
            instance.info("§cÉchec de la mise à jour de multiserver.yml avec les valeurs par défaut : " + e.getMessage());
        }
    }
}
