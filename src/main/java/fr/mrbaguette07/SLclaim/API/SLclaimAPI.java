package fr.mrbaguette07.SLclaim.API;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.mrbaguette07.SLclaim.Types.CPlayer;
import fr.mrbaguette07.SLclaim.Types.Claim;
import fr.mrbaguette07.SLclaim.Types.CustomSet;

/**
 * Interface providing API methods for the Simple Claim System.
 */
public interface SLclaimAPI {
    
	
    // *********************
    // *  Players Methods  *
    // *********************


    /**
     * Retrieves a player's claim by name.
     *
     * @param player the player
     * @param claimName the name of the claim
     * @return the player's claim, or null if not found
     */
    Claim getPlayerClaim(Player player, String claimName);
    
    /**
     * Retrieves player's claims.
     * 
     * @param player the player
     * @return a set with all the player's claims
     */
    Set<Claim> getPlayerClaims(Player player);
    
    /**
     * Retrieves all the claims where the player is member of.
     * 
     * @param player the player
     * @return a set with all the claims the player is member of
     */
    Set<Claim> getPlayerClaimsWhereMember(Player player);

    /**
     * Retrieves a CPlayer object by player name.
     *
     * @param playerName the name of the player
     * @return the CPlayer object, or null if not found
     */
    CPlayer getCPlayer(String playerName);

    /**
     * Retrieves a CPlayer object by Player instance.
     *
     * @param player the player
     * @return the CPlayer object, or null if not found
     */
    CPlayer getCPlayer(Player player);
    

    // ********************
    // *  Claims Methods  *
    // ********************
    

    /**
     * Unclaims the specified claim.
     *
     * @param claim the claim to unclaim
     * @return true if the claim was successfully unclaimed, false otherwise
     */
    boolean unclaim(Claim claim);

    /**
     * Unclaims all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @return true if the claims were successfully unclaimed, false otherwise
     */
    boolean unclaimAll(String owner);

    /**
     * Checks if a chunk is claimed.
     *
     * @param chunk the chunk to check
     * @return true if the chunk is claimed, false otherwise
     */
    boolean isClaimed(Chunk chunk);

    /**
     * Retrieves the claim at the specified chunk.
     *
     * @param chunk the chunk to check
     * @return the claim at the chunk, or null if not found
     */
    Claim getClaimAtChunk(Chunk chunk);
    
    /**
     * Gets all the claims
     * @return A set of all the claims
     */
    Set<Claim> getAllClaims();
    
    /**
     * Gets the claims in a target world
     * @return A set of all the claims in a specific world
     */
    Set<Claim> getClaims(World targetWorld);

    /**
     * Applies settings from the specified claim to all claims.
     *
     * @param claim the claim with settings to apply
     * @return true if the settings were successfully applied, false otherwise
     */
    boolean applySettingsToAllClaims(Claim claim);

    /**
     * Resets settings for all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @return true if the settings were successfully reset, false otherwise
     */
    boolean resetClaimsSettings(String owner);

    /**
     * Resets settings for all claims.
     *
     * @return true if the settings were successfully reset, false otherwise
     */
    boolean resetAllClaimsSettings();

    /**
     * Merges multiple claims into the main claim.
     *
     * @param mainClaim the main claim
     * @param claimsToMerge the set of claims to merge
     * @return true if the claims were successfully merged, false otherwise
     */
    boolean mergeMultipleClaims(Claim mainClaim, CustomSet<Claim> claimsToMerge);
    
    /**
     * Kicks a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayer the target player
     */
    void kickPlayerFromClaim(Claim claim, String targetPlayer);
    
    /**
     * Kicks a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayer the target player
     */
    void kickPlayerFromAllClaims(String owner, String targetPlayer);

    /**
     * Bans a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayer the target player
     * @return true if the player was successfully banned, false otherwise
     */
    boolean banPlayerFromClaim(Claim claim, String targetPlayer);

    /**
     * Unbans a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayer the target player
     * @return true if the player was successfully unbanned, false otherwise
     */
    boolean unbanPlayerFromClaim(Claim claim, String targetPlayer);

    /**
     * Bans a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayer the target player
     * @return true if the player was successfully banned, false otherwise
     */
    boolean banPlayerFromAllClaims(String owner, String targetPlayer);

    /**
     * Unbans a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayer the target player
     * @return true if the player was successfully unbanned, false otherwise
     */
    boolean unbanPlayerFromAllClaims(String owner, String targetPlayer);

    /**
     * Adds a player to a specific claim.
     *
     * @param claim the claim
     * @param targetPlayer the target player
     * @return true if the player was successfully added, false otherwise
     */
    boolean addPlayerToClaim(Claim claim, String targetPlayer);

    /**
     * Removes a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayer the target player
     * @return true if the player was successfully removed, false otherwise
     */
    boolean removePlayerFromClaim(Claim claim, String targetPlayer);

    /**
     * Adds a player to all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayer the target player
     * @return true if the player was successfully added, false otherwise
     */
    boolean addPlayerToAllClaims(String owner, String targetPlayer);

    /**
     * Removes a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayer the target player
     * @return true if the player was successfully removed, false otherwise
     */
    boolean removePlayerFromAllClaims(String owner, String targetPlayer);

    /**
     * Resets the permissions of a specific claim.
     *
     * @param claim the claim
     * @return true if the permissions were successfully reset, false otherwise
     */
    boolean resetClaimPerm(Claim claim);

    /**
     * Sets a permission for a specific claim.
     *
     * @param claim the claim
     * @param permission the permission to set
     * @param value the value of the permission
     * @param role the role to update permission for
     * @return true if the permission was successfully set, false otherwise
     */
    boolean setClaimPerm(Claim claim, String permission, boolean value, String role);

    /**
     * Sets the name of a specific claim.
     *
     * @param claim the claim
     * @param newName the new name
     * @return true if the name was successfully set, false otherwise
     */
    boolean setClaimName(Claim claim, String newName);

    /**
     * Sets the location of a specific claim.
     *
     * @param claim the claim
     * @param newLoc the new location
     * @return true if the location was successfully set, false otherwise
     */
    boolean setClaimLocation(Claim claim, Location newLoc);

    /**
     * Sets the description of a specific claim.
     *
     * @param claim the claim
     * @param newDesc the new description
     * @return true if the description was successfully set, false otherwise
     */
    boolean setClaimDescription(Claim claim, String newDesc);

    /**
     * Adds a claim for sale.
     *
     * @param claim the claim
     * @param claimPrice the price of the claim
     * @return true if the claim was successfully added for sale, false otherwise
     */
    boolean addClaimSale(Claim claim, long claimPrice);

    /**
     * Removes a claim from sale.
     *
     * @param claim the claim
     * @return true if the claim was successfully removed from sale, false otherwise
     */
    boolean removeClaimSale(Claim claim);

    /**
     * Sets the owner of a specific claim.
     *
     * @param claim the claim
     * @param newOwner the new owner
     * @return true if the owner was successfully set, false otherwise
     */
    boolean setClaimOwner(Claim claim, String newOwner);

    /**
     * Adds a chunk to a specific claim.
     *
     * @param claim the claim
     * @param chunk the chunk to add
     * @return true if the chunk was successfully added, false otherwise
     */
    boolean addClaimChunk(Claim claim, Chunk chunk);

    /**
     * Removes a chunk from a specific claim.
     *
     * @param claim the claim
     * @param chunk the chunk to remove
     * @return true if the chunk was successfully removed, false otherwise
     */
    boolean removeClaimChunk(Claim claim, Chunk chunk);
    
    
    // *******************
    // *  Other Methods  *
    // *******************
    

    /**
     * Retrieves the map for a player at a specific chunk.
     *
     * @param player the player
     * @param chunk the chunk
     */
    void getMap(Player player, Chunk chunk);
    
    /**
     * Updates the player bossbar.
     * 
     * @param player The player.
     * @param chunk The chunk.
     */
    void updateBossBar(Player player, Chunk chunk);

    /**
     * Creates a new claim asynchronously.
     *
     * @param world The world where the claim will be created.
     * @param owner The owner of the claim.
     * @param name The name of the claim.
     * @param description The description of the claim.
     * @param location The location of the claim.
     * @param price The price of the claim.
     * @return A CompletableFuture that will complete with the created Claim, or exceptionally if the claim could not be created.
     */
    CompletableFuture<Claim> createClaimAsync(World world, String owner, String name, String description, Location location, long price);

    /**
     * Creates a new claim.
     *
     * @param world The world where the claim will be created.
     * @param owner The owner of the claim.
     * @param name The name of the claim.
     * @param description The description of the claim.
     * @param location The location of the claim.
     * @param price The price of the claim.
     * @return The created Claim, or null if the claim could not be created.
     */
    Claim createClaim(World world, String owner, String name, String description, Location location, long price);

    /**
     * Retrieves a list of all servers.
     *
     * @return A list of server names.
     */
    List<String> getAllServers();

    /**
     * Retrieves the claims of a player on a specific server.
     *
     * @param playerName The name of the player.
     * @param serverName The name of the server.
     * @return A set of claims belonging to the player on the specified server.
     */
    Set<Claim> getPlayerClaimsOnServer(String playerName, String serverName);

    /**
     * Retrieves the claim data from the database.
     *
     * @param claim The claim to retrieve data for.
     * @return A map containing the claim data.
     */
    Map<String, Object> getClaimData(Claim claim);

    /**
     * Loads claim data from the database.
     *
     * @param claim The claim to load data for.
     * @return True if the data was successfully loaded, false otherwise.
     */
    boolean loadClaimData(Claim claim);

    /**
     * Saves claim data to the database.
     *
     * @param claim The claim to save data for.
     * @return True if the data was successfully saved, false otherwise.
     */
    boolean saveClaimData(Claim claim);

    /**
     * Deletes claim data from the database.
     *
     * @param claim The claim to delete data for.
     * @return True if the data was successfully deleted, false otherwise.
     */
    boolean deleteClaimData(Claim claim);
    
    
    // ***************************
    // *  Multi-Server Methods   *
    // ***************************
    
    
    /**
     * Checks if multi-server mode is enabled.
     *
     * @return true if multi-server mode is enabled, false otherwise.
     */
    boolean isMultiServerEnabled();
    
    /**
     * Checks if the current server is a lobby server.
     *
     * @return true if the current server is a lobby server, false otherwise.
     */
    boolean isLobbyServer();
    
    /**
     * Checks if claiming is allowed on this server.
     *
     * @return true if claiming is allowed, false otherwise.
     */
    boolean canClaimOnThisServer();
    
    /**
     * Gets the name of the current server.
     *
     * @return The server name, or null if multi-server is not enabled.
     */
    String getServerName();
    
    /**
     * Checks if a specific server is online.
     *
     * @param serverName The name of the server to check.
     * @return true if the server is online, false otherwise.
     */
    boolean isServerOnline(String serverName);
    
    /**
     * Gets a list of all online survival servers.
     *
     * @return A list of online survival server names.
     */
    List<String> getOnlineSurvivalServers();
    
    /**
     * Gets a list of all online lobby servers.
     *
     * @return A list of online lobby server names.
     */
    List<String> getOnlineLobbyServers();
    
    /**
     * Gets claims from MongoDB for a specific owner (multi-server mode).
     * This is useful for lobby servers that don't have local claims.
     *
     * @param ownerName The name of the owner.
     * @return A CompletableFuture with a list of claim data maps.
     */
    CompletableFuture<List<Map<String, Object>>> getClaimsFromMongo(String ownerName);
    
    /**
     * Gets a specific claim from MongoDB by owner and name (multi-server mode).
     *
     * @param ownerName The name of the owner.
     * @param claimName The name of the claim.
     * @return A CompletableFuture with the claim data map, or null if not found.
     */
    CompletableFuture<Map<String, Object>> getClaimFromMongo(String ownerName, String claimName);
    
    /**
     * Gets all claim owners with their claim counts from MongoDB.
     *
     * @return A CompletableFuture with a map of owner names to claim counts.
     */
    CompletableFuture<Map<String, Integer>> getClaimOwnersFromMongo();
    
    /**
     * Teleports a player to a claim on another server (cross-server teleport).
     *
     * @param player The player to teleport.
     * @param ownerName The name of the claim owner.
     * @param claimName The name of the claim.
     */
    void teleportToClaimCrossServer(Player player, String ownerName, String claimName);
    
    /**
     * Teleports a player to a claim on a specific server.
     *
     * @param player The player to teleport.
     * @param ownerName The name of the claim owner.
     * @param claimName The name of the claim.
     * @param targetServer The target server name.
     */
    void teleportToClaimOnServer(Player player, String ownerName, String claimName, String targetServer);
    
    /**
     * Transfers a player to another server.
     *
     * @param player The player to transfer.
     * @param serverName The name of the target server.
     */
    void transferPlayerToServer(Player player, String serverName);
    
    
    // ***************************
    // *  Claim Query Methods    *
    // ***************************
    
    
    /**
     * Gets a claim by owner name and claim name.
     *
     * @param ownerName The owner's name.
     * @param claimName The claim's name.
     * @return The claim, or null if not found.
     */
    Claim getClaimByName(String ownerName, String claimName);
    
    /**
     * Gets a claim by owner UUID and claim name.
     *
     * @param ownerUUID The owner's UUID.
     * @param claimName The claim's name.
     * @return The claim, or null if not found.
     */
    Claim getClaimByName(UUID ownerUUID, String claimName);
    
    /**
     * Gets a player's claims by their name.
     *
     * @param ownerName The player's name.
     * @return A set of claims belonging to the player.
     */
    Set<Claim> getPlayerClaimsByName(String ownerName);
    
    /**
     * Gets a player's claims by their UUID.
     *
     * @param ownerUUID The player's UUID.
     * @return A set of claims belonging to the player.
     */
    Set<Claim> getPlayerClaimsByUUID(UUID ownerUUID);
    
    /**
     * Gets all claims where a player is a member (but not the owner).
     *
     * @param playerUUID The player's UUID.
     * @return A set of claims where the player is a member.
     */
    Set<Claim> getClaimsWhereMember(UUID playerUUID);
    
    /**
     * Gets the total number of claims.
     *
     * @return The total claim count.
     */
    int getTotalClaimCount();
    
    /**
     * Gets all claims that are for sale.
     *
     * @return A set of claims that are for sale.
     */
    Set<Claim> getClaimsForSale();
    
    /**
     * Gets all claim owners with their claim counts.
     *
     * @return A map of owner names to claim counts.
     */
    Map<String, Integer> getClaimOwners();
    
    /**
     * Gets all online claim owners with their claim counts.
     *
     * @return A map of online owner names to claim counts.
     */
    Map<String, Integer> getOnlineClaimOwners();
    
    /**
     * Gets all offline claim owners with their claim counts.
     *
     * @return A map of offline owner names to claim counts.
     */
    Map<String, Integer> getOfflineClaimOwners();
}
