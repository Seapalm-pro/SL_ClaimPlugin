package fr.mrbaguette07.SLclaim.API.Listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Event that is triggered when a player enters a claim.
 * This event is cancellable - cancelling it will prevent the player from entering.
 */
public class PlayerEnterClaimEvent extends Event implements Cancellable {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The player entering the claim */
    private final Player player;
    
    /** The claim being entered */
    private final Claim claim;
    
    /** Whether the event is cancelled */
    private boolean cancelled = false;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for PlayerEnterClaimEvent.
     *
     * @param player The player entering the claim.
     * @param claim The claim being entered.
     */
    public PlayerEnterClaimEvent(Player player, Claim claim) {
        this.player = player;
        this.claim = claim;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the player entering the claim.
     *
     * @return The player.
     */
    public Player getPlayer() {
        return player;
    }
    
    /**
     * Gets the claim being entered.
     *
     * @return The claim.
     */
    public Claim getClaim() {
        return claim;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }
    
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
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
