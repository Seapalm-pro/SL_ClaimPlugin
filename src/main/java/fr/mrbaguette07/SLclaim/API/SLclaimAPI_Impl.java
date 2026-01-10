package fr.mrbaguette07.SLclaim.API;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.mrbaguette07.SLclaim.SLclaim;
import fr.mrbaguette07.SLclaim.MultiServer.MultiServerManager;
import fr.mrbaguette07.SLclaim.Types.CPlayer;
import fr.mrbaguette07.SLclaim.Types.Claim;
import fr.mrbaguette07.SLclaim.Types.CustomSet;

/**
 * Implementation of the SLclaimAPI interface.
 * Provides methods to interact with the claim system and player data.
 */
public class SLclaimAPI_Impl implements SLclaimAPI {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Instance of SLclaim */
	private SLclaim instance;
	
	
    // ******************
    // *  Constructors  *
    // ******************
	
	
    /**
     * Constructs a new SLclaimAPI_Impl.
     *
     * @param instance the instance of the SLclaim
     */
	public SLclaimAPI_Impl(SLclaim instance) {
		this.instance = instance;
	}
	
	
    // *******************
    // *  Other methods  *
    // *******************
	

	// Player methods

	@Override
	public Claim getPlayerClaim(Player player, String claimName) {
		return instance.getMain().getClaimByName(claimName, player);
	}
	
	@Override
	public Set<Claim> getPlayerClaims(Player player){
		return instance.getMain().getPlayerClaims(player.getUniqueId());
	}
	
	@Override
	public Set<Claim> getPlayerClaimsWhereMember(Player player){
		return instance.getMain().getClaimsWhereMemberNotOwner(player);
	}

	@Override
	public CPlayer getCPlayer(String playerName) {
		return instance.getPlayerMain().getCPlayer(Bukkit.getOfflinePlayer(playerName).getUniqueId());
	}

	@Override
	public CPlayer getCPlayer(Player player) {
		return instance.getPlayerMain().getCPlayer(player.getUniqueId());
	}
	
	// Claim methods

	@Override
	public boolean unclaim(Claim claim) {
		return instance.getMain().deleteClaim(claim).join();
	}

	@Override
	public boolean unclaimAll(String owner) {
		return instance.getMain().deleteAllClaims(owner).join();
	}

	@Override
	public boolean isClaimed(Chunk chunk) {
		return instance.getMain().checkIfClaimExists(chunk);
	}

	@Override
	public Claim getClaimAtChunk(Chunk chunk) {
		return instance.getMain().getClaim(chunk);
	}
	
	@Override
	public Set<Claim> getAllClaims(){
		return instance.getMain().getAllClaims();
	}
	
	@Override
	public Set<Claim> getClaims(World targetWorld){
		return instance.getMain().getAllClaims().parallelStream()
				.filter(claim -> claim.getLocation().getWorld().equals(targetWorld))
				.collect(Collectors.toSet());
	}

	@Override
	public boolean applySettingsToAllClaims(Claim claim) {
		return instance.getMain().applyAllSettings(claim).join();
	}

	@Override
	public boolean resetClaimsSettings(String owner) {
		return instance.getMain().resetAllOwnerClaimsSettings(owner).join();
	}

	@Override
	public boolean resetAllClaimsSettings() {
		return instance.getMain().resetAllPlayerClaimsSettings().join();
	}

	@Override
	public boolean mergeMultipleClaims(Claim mainClaim, CustomSet<Claim> claimsToMerge) {
		return instance.getMain().mergeClaims(mainClaim, claimsToMerge).join();
	}
	
	@Override
	public void kickPlayerFromClaim(Claim claim, String targetPlayerName) {
		Player target = Bukkit.getPlayer(targetPlayerName);
		if(target != null && target.isOnline()) {
			if(claim.getChunks().contains(target.getLocation().getChunk())) {
				instance.getMain().teleportPlayer(target, Bukkit.getWorlds().get(0).getSpawnLocation());
			}
		}
	}
	
	@Override
	public void kickPlayerFromAllClaims(String owner, String targetPlayerName) {
		Player target = Bukkit.getPlayer(targetPlayerName);
		if(target != null && target.isOnline()) {
			Set<Chunk> chunks = instance.getMain().getAllChunksFromAllClaims(owner);
			if(chunks.contains(target.getLocation().getChunk())) {
				instance.getMain().teleportPlayer(target, Bukkit.getWorlds().get(0).getSpawnLocation());
			}
		}
	}

	@Override
	public boolean banPlayerFromClaim(Claim claim, String targetPlayerName) {
		return instance.getMain().addClaimBan(claim, targetPlayerName).join();
	}

	@Override
	public boolean unbanPlayerFromClaim(Claim claim, String targetPlayerName) {
		return instance.getMain().removeClaimBan(claim, targetPlayerName).join();
	}

	@Override
	public boolean banPlayerFromAllClaims(String owner, String targetPlayerName) {
		return instance.getMain().addAllClaimBan(owner, targetPlayerName).join();
	}

	@Override
	public boolean unbanPlayerFromAllClaims(String owner, String targetPlayerName) {
		return instance.getMain().removeAllClaimBan(owner, targetPlayerName).join();
	}

	@Override
	public boolean addPlayerToClaim(Claim claim, String targetPlayerName) {
		return instance.getMain().addClaimMember(claim, targetPlayerName).join();
	}

	@Override
	public boolean removePlayerFromClaim(Claim claim, String targetPlayerName) {
		return instance.getMain().removeClaimMember(claim, targetPlayerName).join();
	}

	@Override
	public boolean addPlayerToAllClaims(String owner, String targetPlayerName) {
		return instance.getMain().addAllClaimsMember(owner, targetPlayerName).join();
	}

	@Override
	public boolean removePlayerFromAllClaims(String owner, String targetPlayerName) {
		return instance.getMain().removeAllClaimsMember(owner, targetPlayerName).join();
	}

	@Override
	public boolean resetClaimPerm(Claim claim) {
		return instance.getMain().resetClaimSettings(claim).join();
	}

	@Override
	public boolean setClaimPerm(Claim claim, String permission, boolean value, String role) {
		return instance.getMain().updatePerm(claim, permission, value, role).join();
	}

	@Override
	public boolean setClaimName(Claim claim, String newName) {
		return instance.getMain().setClaimName(claim, newName).join();
	}

	@Override
	public boolean setClaimLocation(Claim claim, Location newLoc) {
		return instance.getMain().setClaimLocation(claim, newLoc).join();
	}

	@Override
	public boolean setClaimDescription(Claim claim, String newDesc) {
		return instance.getMain().setClaimDescription(claim, newDesc).join();
	}

	@Override
	public boolean addClaimSale(Claim claim, long claimPrice) {
		return instance.getMain().setChunkSale(claim, claimPrice).join();
	}

	@Override
	public boolean removeClaimSale(Claim claim) {
		return instance.getMain().delChunkSale(claim).join();
	}

	@Override
	public boolean setClaimOwner(Claim claim, String newOwner) {
		return instance.getMain().setOwner(newOwner, claim).join();
	}

	@Override
	public boolean addClaimChunk(Claim claim, Chunk chunk) {
		return instance.getMain().addClaimChunk(claim, chunk).join();
	}

	@Override
	public boolean removeClaimChunk(Claim claim, Chunk chunk) {
		return instance.getMain().removeClaimChunk(claim, String.valueOf(chunk.getWorld().getName()+";"+chunk.getX()+";"+chunk.getZ())).join();
	}
	
	// Other methods

	@Override
	public void getMap(Player player, Chunk chunk) {
		instance.getMain().getMap(player, chunk, false);
	}
	
	@Override
	public void updateBossBar(Player player, Chunk chunk) {
		instance.getBossBars().activeBossBar(player, chunk);
	}
	
	@Override
	public CompletableFuture<Claim> createClaimAsync(World world, String owner, String name, String description, Location location, long price) {
		return CompletableFuture.completedFuture(null);
	}
	
	@Override
	public Claim createClaim(World world, String owner, String name, String description, Location location, long price) {
		return createClaimAsync(world, owner, name, description, location, price).join();
	}
	
	@Override
	public List<String> getAllServers() {
		MultiServerManager msm = instance.getMultiServerManager();
		if (msm == null || !msm.isEnabled()) {
			return Collections.emptyList();
		}
		return new ArrayList<>(msm.getConfig().getSurvivalServers());
	}
	
	@Override
	public Set<Claim> getPlayerClaimsOnServer(String playerName, String serverName) {
		return instance.getMain().getPlayerClaims(playerName);
	}
	
	@Override
	public Map<String, Object> getClaimData(Claim claim) {
		Map<String, Object> data = new HashMap<>();
		data.put("name", claim.getName());
		data.put("owner", claim.getOwner());
		data.put("description", claim.getDescription());
		data.put("for_sale", claim.getSale());
		data.put("price", claim.getPrice());
		data.put("location", claim.getLocation());
		data.put("members", claim.getMembers());
		data.put("bans", claim.getBans());
		return data;
	}
	
	@Override
	public boolean loadClaimData(Claim claim) {
		return true;
	}
	
	@Override
	public boolean saveClaimData(Claim claim) {
		return true;
	}
	
	@Override
	public boolean deleteClaimData(Claim claim) {
		return unclaim(claim);
	}

	@Override
	public boolean isMultiServerEnabled() {
		MultiServerManager msm = instance.getMultiServerManager();
		return msm != null && msm.isEnabled();
	}
	
	@Override
	public boolean isLobbyServer() {
		MultiServerManager msm = instance.getMultiServerManager();
		return msm != null && msm.isEnabled() && msm.isLobbyServer();
	}
	
	@Override
	public boolean canClaimOnThisServer() {
		MultiServerManager msm = instance.getMultiServerManager();
		if (msm == null || !msm.isEnabled()) {
			return true;
		}
		return msm.canClaim();
	}
	
	@Override
	public String getServerName() {
		MultiServerManager msm = instance.getMultiServerManager();
		if (msm == null || !msm.isEnabled()) {
			return null;
		}
		return msm.getConfig().getServerName();
	}
	
	@Override
	public boolean isServerOnline(String serverName) {
		MultiServerManager msm = instance.getMultiServerManager();
		if (msm == null || !msm.isEnabled()) {
			return false;
		}
		return msm.isServerOnline(serverName);
	}
	
	@Override
	public List<String> getOnlineSurvivalServers() {
		MultiServerManager msm = instance.getMultiServerManager();
		if (msm == null || !msm.isEnabled()) {
			return Collections.emptyList();
		}
		return msm.getConfig().getSurvivalServers().stream()
				.filter(msm::isServerOnline)
				.collect(Collectors.toList());
	}
	
	@Override
	public List<String> getOnlineLobbyServers() {
		MultiServerManager msm = instance.getMultiServerManager();
		if (msm == null || !msm.isEnabled()) {
			return Collections.emptyList();
		}
		return msm.getConfig().getLobbyServers().stream()
				.filter(msm::isServerOnline)
				.collect(Collectors.toList());
	}
	
	@Override
	public CompletableFuture<List<Map<String, Object>>> getClaimsFromMongo(String ownerName) {
		return instance.getMain().getClaimsFromMongoByOwner(ownerName);
	}
	
	@Override
	public CompletableFuture<Map<String, Object>> getClaimFromMongo(String ownerName, String claimName) {
		return instance.getMain().getClaimFromMongoByName(ownerName, claimName);
	}
	
	@Override
	public CompletableFuture<Map<String, Integer>> getClaimOwnersFromMongo() {
		return instance.getMain().getClaimsOwnersGuiFromMongo();
	}
	
	@Override
	public void teleportToClaimCrossServer(Player player, String ownerName, String claimName) {
		instance.getMain().goClaimCrossServer(player, ownerName, claimName);
	}
	
	@Override
	public void teleportToClaimOnServer(Player player, String ownerName, String claimName, String targetServer) {
		instance.getMain().goClaimCrossServer(player, ownerName, claimName, targetServer);
	}
	
	@Override
	public void transferPlayerToServer(Player player, String serverName) {
		instance.getMain().transferPlayerToServer(player, serverName);
	}

	@Override
	public Claim getClaimByName(String ownerName, String claimName) {
		return instance.getMain().getClaimByName(claimName, ownerName);
	}
	
	@Override
	public Claim getClaimByName(UUID ownerUUID, String claimName) {
		return instance.getMain().getClaimByName(claimName, ownerUUID);
	}
	
	@Override
	public Set<Claim> getPlayerClaimsByName(String ownerName) {
		return instance.getMain().getPlayerClaims(ownerName);
	}
	
	@Override
	public Set<Claim> getPlayerClaimsByUUID(UUID ownerUUID) {
		return instance.getMain().getPlayerClaims(ownerUUID);
	}
	
	@Override
	public Set<Claim> getClaimsWhereMember(UUID playerUUID) {
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null) {
			return Collections.emptySet();
		}
		return instance.getMain().getClaimsWhereMemberNotOwner(player);
	}
	
	@Override
	public int getTotalClaimCount() {
		return instance.getMain().getAllClaimsCount();
	}
	
	@Override
	public Set<Claim> getClaimsForSale() {
		return instance.getMain().getAllClaims().stream()
				.filter(Claim::getSale)
				.collect(Collectors.toSet());
	}
	
	@Override
	public Map<String, Integer> getClaimOwners() {
		return instance.getMain().getClaimsOwnersGui();
	}
	
	@Override
	public Map<String, Integer> getOnlineClaimOwners() {
		return instance.getMain().getClaimsOnlineOwners();
	}
	
	@Override
	public Map<String, Integer> getOfflineClaimOwners() {
		return instance.getMain().getClaimsOfflineOwners();
	}
	
}
