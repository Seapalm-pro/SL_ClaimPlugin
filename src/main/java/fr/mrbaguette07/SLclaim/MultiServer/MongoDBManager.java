package fr.mrbaguette07.SLclaim.MultiServer;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.*;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;

import fr.mrbaguette07.SLclaim.ClaimMain;
import fr.mrbaguette07.SLclaim.SLclaim;
import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Manages MongoDB connections and operations for multi-server data storage.
 */
public class MongoDBManager {
    
    // ***************
    // *  Variables  *
    // ***************
    
    /** Instance of SLclaim */
    private final SLclaim instance;
    
    /** MongoDB client */
    private MongoClient mongoClient;
    
    /** MongoDB database */
    private MongoDatabase database;
    
    /** Claims collection */
    private MongoCollection<Document> claimsCollection;
    
    /** Players collection */
    private MongoCollection<Document> playersCollection;
    
    /** Executor for async operations */
    private final ExecutorService executor;
    
    /** Whether MongoDB is connected */
    private boolean connected;
    
    // ******************
    // *  Constructors  *
    // ******************
    
    /**
     * Constructor for MongoDBManager.
     *
     * @param instance The SLclaim instance.
     */
    public MongoDBManager(SLclaim instance) {
        this.instance = instance;
        this.executor = Executors.newCachedThreadPool();
        this.connected = false;
    }
    
    // ********************
    // *  Public Methods  *
    // ********************
    
    /**
     * Connects to MongoDB.
     *
     * @return CompletableFuture that completes when connected
     */
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                MultiServerConfig config = instance.getMultiServerManager().getConfig();
                
                ConnectionString connectionString = new ConnectionString(config.getMongoConnectionString());
                MongoClientSettings settings = MongoClientSettings.builder()
                        .applyConnectionString(connectionString)
                        .build();
                
                mongoClient = MongoClients.create(settings);
                database = mongoClient.getDatabase(config.getMongoDatabaseName());
                
                claimsCollection = database.getCollection(config.getMongoClaimsCollection());
                playersCollection = database.getCollection(config.getMongoPlayersCollection());
                
                // Create indexes
                createIndexes();
                
                // Test connection
                database.runCommand(new Document("ping", 1));
                
                connected = true;
                instance.info("Connexion MongoDB établie.");
                
                return true;
            } catch (Exception e) {
                instance.info("§cÉchec de la connexion à MongoDB : " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    /**
     * Disconnects from MongoDB.
     */
    public void disconnect() {
        connected = false;
        
        if (mongoClient != null) {
            mongoClient.close();
        }
        
        executor.shutdown();
        
        instance.info("Connexion MongoDB fermée.");
    }
    
    /**
     * Checks if MongoDB is connected.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }
    
    // *********************
    // *  Claim Operations *
    // *********************
    
    /**
     * Saves a claim to MongoDB.
     *
     * @param claim The claim to save
     * @param ownerUUID The owner's UUID
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Boolean> saveClaim(Claim claim, UUID ownerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return false;
            
            try {
                Document doc = claimToDocument(claim, ownerUUID);
                
                Bson filter = Filters.and(
                    Filters.eq("owner_uuid", ownerUUID.toString()),
                    Filters.eq("id_claim", claim.getId())
                );
                
                ReplaceOptions options = new ReplaceOptions().upsert(true);
                claimsCollection.replaceOne(filter, doc, options);
                
                return true;
            } catch (Exception e) {
                instance.info("§cFailed to save claim to MongoDB: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    /**
     * Deletes a claim from MongoDB.
     *
     * @param ownerUUID The owner's UUID
     * @param claimId The claim ID
     * @return CompletableFuture that completes when deleted
     */
    public CompletableFuture<Boolean> deleteClaim(UUID ownerUUID, int claimId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return false;
            
            try {
                Bson filter = Filters.and(
                    Filters.eq("owner_uuid", ownerUUID.toString()),
                    Filters.eq("id_claim", claimId)
                );
                
                DeleteResult result = claimsCollection.deleteOne(filter);
                return result.getDeletedCount() > 0;
            } catch (Exception e) {
                instance.info("§cFailed to delete claim from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    /**
     * Deletes all claims for an owner from MongoDB.
     *
     * @param ownerUUID The owner's UUID
     * @return CompletableFuture with the number of deleted claims
     */
    public CompletableFuture<Long> deleteAllClaims(UUID ownerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return 0L;
            
            try {
                Bson filter = Filters.eq("owner_uuid", ownerUUID.toString());
                DeleteResult result = claimsCollection.deleteMany(filter);
                return result.getDeletedCount();
            } catch (Exception e) {
                instance.info("§cFailed to delete claims from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return 0L;
            }
        }, executor);
    }
    
    /**
     * Gets a claim from MongoDB.
     *
     * @param ownerUUID The owner's UUID
     * @param claimId The claim ID
     * @return CompletableFuture with the claim document (or null)
     */
    public CompletableFuture<Document> getClaim(UUID ownerUUID, int claimId) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return null;
            
            try {
                Bson filter = Filters.and(
                    Filters.eq("owner_uuid", ownerUUID.toString()),
                    Filters.eq("id_claim", claimId)
                );
                
                return claimsCollection.find(filter).first();
            } catch (Exception e) {
                instance.info("§cFailed to get claim from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, executor);
    }
    
    /**
     * Gets all claims from MongoDB.
     *
     * @return CompletableFuture with list of claim documents
     */
    public CompletableFuture<List<Document>> getAllClaims() {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return new ArrayList<>();
            
            try {
                List<Document> claims = new ArrayList<>();
                claimsCollection.find().into(claims);
                return claims;
            } catch (Exception e) {
                instance.info("§cFailed to get claims from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        }, executor);
    }
    
    /**
     * Gets all claims for an owner from MongoDB.
     *
     * @param ownerUUID The owner's UUID
     * @return CompletableFuture with list of claim documents
     */
    public CompletableFuture<List<Document>> getPlayerClaims(UUID ownerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return new ArrayList<>();
            
            try {
                List<Document> claims = new ArrayList<>();
                Bson filter = Filters.eq("owner_uuid", ownerUUID.toString());
                claimsCollection.find(filter).into(claims);
                return claims;
            } catch (Exception e) {
                instance.info("§cFailed to get player claims from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        }, executor);
    }
    
    /**
     * Updates a specific field of a claim.
     *
     * @param ownerUUID The owner's UUID
     * @param claimId The claim ID
     * @param field The field to update
     * @param value The new value
     * @return CompletableFuture that completes when updated
     */
    public CompletableFuture<Boolean> updateClaimField(UUID ownerUUID, int claimId, String field, Object value) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return false;
            
            try {
                Bson filter = Filters.and(
                    Filters.eq("owner_uuid", ownerUUID.toString()),
                    Filters.eq("id_claim", claimId)
                );
                
                Bson update = Updates.set(field, value);
                UpdateResult result = claimsCollection.updateOne(filter, update);
                
                return result.getModifiedCount() > 0;
            } catch (Exception e) {
                instance.info("§cFailed to update claim field in MongoDB: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    // **********************
    // *  Player Operations *
    // **********************
    
    /**
     * Saves player data to MongoDB.
     *
     * @param uuid The player's UUID
     * @param name The player's name
     * @param uuidMojang The player's Mojang UUID
     * @param textures The player's skin textures
     * @return CompletableFuture that completes when saved
     */
    public CompletableFuture<Boolean> savePlayer(UUID uuid, String name, String uuidMojang, String textures) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return false;
            
            try {
                Document doc = new Document()
                    .append("uuid_server", uuid.toString())
                    .append("uuid_mojang", uuidMojang != null ? uuidMojang : "none")
                    .append("player_name", name)
                    .append("player_textures", textures != null ? textures : "none")
                    .append("last_updated", System.currentTimeMillis());
                
                Bson filter = Filters.eq("uuid_server", uuid.toString());
                ReplaceOptions options = new ReplaceOptions().upsert(true);
                playersCollection.replaceOne(filter, doc, options);
                
                return true;
            } catch (Exception e) {
                instance.info("§cFailed to save player to MongoDB: " + e.getMessage());
                e.printStackTrace();
                return false;
            }
        }, executor);
    }
    
    /**
     * Gets player data from MongoDB.
     *
     * @param uuid The player's UUID
     * @return CompletableFuture with the player document (or null)
     */
    public CompletableFuture<Document> getPlayer(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return null;
            
            try {
                Bson filter = Filters.eq("uuid_server", uuid.toString());
                return playersCollection.find(filter).first();
            } catch (Exception e) {
                instance.info("§cFailed to get player from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, executor);
    }
    
    /**
     * Gets all players from MongoDB.
     *
     * @return CompletableFuture with list of player documents
     */
    public CompletableFuture<List<Document>> getAllPlayers() {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return new ArrayList<>();
            
            try {
                List<Document> players = new ArrayList<>();
                playersCollection.find().into(players);
                return players;
            } catch (Exception e) {
                instance.info("§cFailed to get players from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        }, executor);
    }
    
    /**
     * Gets all claim owners with their claim counts from MongoDB.
     *
     * @return CompletableFuture with map of owner names to claim counts
     */
    public CompletableFuture<Map<String, Integer>> getAllClaimOwners() {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) {
                instance.info("§cMongoDB not connected, cannot get claim owners");
                return new HashMap<>();
            }
            
            try {
                Map<String, Integer> owners = new HashMap<>();
                
                // Aggregate to count claims per owner
                int totalClaims = 0;
                for (Document doc : claimsCollection.find()) {
                    String ownerName = doc.getString("owner_name");
                    if (ownerName != null && !ownerName.equals("*")) {
                        owners.merge(ownerName, 1, Integer::sum);
                        totalClaims++;
                    }
                }
                
                instance.info("§aMongoDB: Found " + totalClaims + " claims from " + owners.size() + " owners");
                
                return owners;
            } catch (Exception e) {
                instance.info("§cFailed to get claim owners from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return new HashMap<>();
            }
        }, executor);
    }
    
    /**
     * Gets all claims for an owner by name from MongoDB.
     *
     * @param ownerName The owner's name
     * @return CompletableFuture with list of claim documents
     */
    public CompletableFuture<List<Document>> getClaimsByOwnerName(String ownerName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) {
                instance.info("§cMongoDB not connected, cannot get claims for owner: " + ownerName);
                return new ArrayList<>();
            }
            
            try {
                List<Document> claims = new ArrayList<>();
                Bson filter = Filters.eq("owner_name", ownerName);
                claimsCollection.find(filter).into(claims);
                instance.info("§aMongoDB: Found " + claims.size() + " claims for owner: " + ownerName);
                return claims;
            } catch (Exception e) {
                instance.info("§cFailed to get claims by owner name from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        }, executor);
    }
    
    /**
     * Gets a claim by owner name and claim name from MongoDB.
     *
     * @param ownerName The owner's name
     * @param claimName The claim's name
     * @return CompletableFuture with the claim document (or null)
     */
    public CompletableFuture<Document> getClaimByOwnerAndName(String ownerName, String claimName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return null;
            
            try {
                Bson filter = Filters.and(
                    Filters.eq("owner_name", ownerName),
                    Filters.eq("claim_name", claimName)
                );
                return claimsCollection.find(filter).first();
            } catch (Exception e) {
                instance.info("§cFailed to get claim from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        }, executor);
    }
    
    /**
     * Gets all claims where a player is a member from MongoDB.
     *
     * @param playerUUID The player's UUID
     * @return CompletableFuture with list of claim documents
     */
    public CompletableFuture<List<Document>> getClaimsWhereMember(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return new ArrayList<>();
            
            try {
                List<Document> claims = new ArrayList<>();
                String uuidStr = playerUUID.toString();
                
                // Find claims where the player is in members list
                for (Document doc : claimsCollection.find()) {
                    String members = doc.getString("members");
                    if (members != null && members.contains(uuidStr)) {
                        claims.add(doc);
                    }
                }
                
                return claims;
            } catch (Exception e) {
                instance.info("§cFailed to get claims where member from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        }, executor);
    }
    
    /**
     * Gets all claims with sale enabled from MongoDB.
     *
     * @return CompletableFuture with map of owner names to claim counts
     */
    public CompletableFuture<Map<String, Integer>> getClaimOwnersWithSales() {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return new HashMap<>();
            
            try {
                Map<String, Integer> owners = new HashMap<>();
                
                Bson filter = Filters.eq("for_sale", true);
                for (Document doc : claimsCollection.find(filter)) {
                    String ownerName = doc.getString("owner_name");
                    if (ownerName != null && !ownerName.equals("*")) {
                        owners.merge(ownerName, 1, Integer::sum);
                    }
                }
                
                return owners;
            } catch (Exception e) {
                instance.info("§cFailed to get claim owners with sales from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return new HashMap<>();
            }
        }, executor);
    }
    
    /**
     * Gets all claim owners who are currently online on any server.
     *
     * @return CompletableFuture with map of owner names to claim counts
     */
    public CompletableFuture<Map<String, Integer>> getOnlineClaimOwners() {
        return CompletableFuture.supplyAsync(() -> {
            if (!connected) return new HashMap<>();
            
            try {
                Map<String, Integer> owners = new HashMap<>();
                
                Set<String> onlinePlayers = new HashSet<>();
                org.bukkit.Bukkit.getOnlinePlayers().forEach(p -> onlinePlayers.add(p.getName()));
                
                for (Document doc : claimsCollection.find()) {
                    String ownerName = doc.getString("owner_name");
                    if (ownerName != null && !ownerName.equals("*") && onlinePlayers.contains(ownerName)) {
                        owners.merge(ownerName, 1, Integer::sum);
                    }
                }
                
                return owners;
            } catch (Exception e) {
                instance.info("§cFailed to get online claim owners from MongoDB: " + e.getMessage());
                e.printStackTrace();
                return new HashMap<>();
            }
        }, executor);
    }
    
    // *********************
    // *  Private Methods  *
    // *********************
    
    /**
     * Creates indexes for collections.
     */
    private void createIndexes() {
        // Claims indexes
        claimsCollection.createIndex(Indexes.ascending("owner_uuid"));
        claimsCollection.createIndex(Indexes.ascending("owner_uuid", "id_claim"));
        claimsCollection.createIndex(Indexes.ascending("claim_name"));
        claimsCollection.createIndex(Indexes.ascending("world_name"));
        claimsCollection.createIndex(Indexes.ascending("for_sale"));
        
        // Players indexes
        playersCollection.createIndex(Indexes.ascending("uuid_server"));
        playersCollection.createIndex(Indexes.ascending("player_name"));
    }
    
    /**
     * Converts a Claim object to a MongoDB Document.
     *
     * @param claim The claim to convert
     * @param ownerUUID The owner's UUID
     * @return The MongoDB Document
     */
    private Document claimToDocument(Claim claim, UUID ownerUUID) {
        // Serialize chunks
        StringBuilder chunksBuilder = new StringBuilder();
        claim.getChunks().forEach(chunk -> {
            if (chunksBuilder.length() > 0) chunksBuilder.append(";");
            chunksBuilder.append(chunk.getWorld().getName())
                        .append(",")
                        .append(chunk.getX())
                        .append(",")
                        .append(chunk.getZ());
        });
        
        // Serialize members
        StringBuilder membersBuilder = new StringBuilder();
        claim.getMembers().forEach(uuid -> {
            if (membersBuilder.length() > 0) membersBuilder.append(";");
            membersBuilder.append(uuid.toString());
        });
        
        // Serialize bans
        StringBuilder bansBuilder = new StringBuilder();
        claim.getBans().forEach(uuid -> {
            if (bansBuilder.length() > 0) bansBuilder.append(";");
            bansBuilder.append(uuid.toString());
        });
        
        // Serialize permissions
        String permissions = instance.getMain().serializePermissions(claim.getPermissions());
        
        // Serialize location
        String location = claim.getLocation().getWorld().getName() + ";" +
                         claim.getLocation().getX() + ";" +
                         claim.getLocation().getY() + ";" +
                         claim.getLocation().getZ() + ";" +
                         claim.getLocation().getYaw() + ";" +
                         claim.getLocation().getPitch();
        
        return new Document()
            .append("id_claim", claim.getId())
            .append("owner_uuid", ownerUUID.toString())
            .append("owner_name", claim.getOwner())
            .append("claim_name", claim.getName())
            .append("claim_description", claim.getDescription())
            .append("chunks", chunksBuilder.toString())
            .append("world_name", claim.getLocation().getWorld().getName())
            .append("location", location)
            .append("members", membersBuilder.toString())
            .append("permissions", permissions)
            .append("for_sale", claim.getSale())
            .append("sale_price", claim.getPrice())
            .append("bans", bansBuilder.toString())
            .append("last_updated", System.currentTimeMillis())
            .append("server_origin", instance.getMultiServerManager().getConfig().getServerName());
    }
    
    /**
     * Gets the claims collection.
     *
     * @return The claims collection
     */
    public MongoCollection<Document> getClaimsCollection() {
        return claimsCollection;
    }
    
    /**
     * Gets the players collection.
     *
     * @return The players collection
     */
    public MongoCollection<Document> getPlayersCollection() {
        return playersCollection;
    }
}
