package fr.mrbaguette07.SLclaim.API.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Event that is triggered when a player leaves a claim.
 */
public class PlayerLeaveClaimEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The player leaving the claim */
    private final Player player;
    
    /** The claim being left */
    private final Claim claim;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for PlayerLeaveClaimEvent.
     *
     * @param player The player leaving the claim.
     * @param claim The claim being left.
     */
    public PlayerLeaveClaimEvent(Player player, Claim claim) {
        this.player = player;
        this.claim = claim;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the player leaving the claim.
     *
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the claim being left.
     *
     * @return The claim.
     */
    public Claim getClaim() {
        return claim;
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
