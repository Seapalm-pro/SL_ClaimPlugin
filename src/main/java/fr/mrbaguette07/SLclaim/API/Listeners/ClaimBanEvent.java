package fr.mrbaguette07.SLclaim.API.Listeners;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Event that is triggered when a player is banned or unbanned from a claim.
 * This event allows other parts of the plugin to respond to ban status changes.
 */
public class ClaimBanEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The claim */
    private final Claim claim;
    
    /** The UUID of the banned/unbanned player */
    private final UUID playerUUID;
    
    /** The name of the banned/unbanned player */
    private final String playerName;
    
    /** Whether the player was banned (true) or unbanned (false) */
    private final boolean banned;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for ClaimBanEvent.
     *
     * @param claim The claim where the ban status changed.
     * @param playerUUID The UUID of the player.
     * @param playerName The name of the player.
     * @param banned true if the player was banned, false if unbanned.
     */
    public ClaimBanEvent(Claim claim, UUID playerUUID, String playerName, boolean banned) {
        this.claim = claim;
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.banned = banned;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claim where the ban status changed.
     *
     * @return The claim.
     */
    public Claim getClaim() {
        return claim;
    }
    
    /**
     * Gets the UUID of the banned/unbanned player.
     *
     * @return The player's UUID.
     */
    public UUID getPlayerUUID() {
        return playerUUID;
    }
    
    /**
     * Gets the name of the banned/unbanned player.
     *
     * @return The player's name.
     */
    public String getPlayerName() {
        return playerName;
    }
    
    /**
     * Checks if the player was banned or unbanned.
     *
     * @return true if the player was banned, false if unbanned.
     */
    public boolean isBanned() {
        return banned;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list.
     */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the static handler list for this event.
     *
     * @return The static handler list.
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
