package fr.mrbaguette07.SLclaim.API.Listeners;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Event that is triggered when a claim's owner is changed.
 * This event allows other parts of the plugin to respond to ownership transfers.
 */
public class ClaimOwnerChangeEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The claim */
    private final Claim claim;
    
    /** The old owner's UUID */
    private final UUID oldOwnerUUID;
    
    /** The old owner's name */
    private final String oldOwnerName;
    
    /** The new owner's UUID */
    private final UUID newOwnerUUID;
    
    /** The new owner's name */
    private final String newOwnerName;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for ClaimOwnerChangeEvent.
     *
     * @param claim The claim whose owner has changed.
     * @param oldOwnerUUID The old owner's UUID.
     * @param oldOwnerName The old owner's name.
     * @param newOwnerUUID The new owner's UUID.
     * @param newOwnerName The new owner's name.
     */
    public ClaimOwnerChangeEvent(Claim claim, UUID oldOwnerUUID, String oldOwnerName, UUID newOwnerUUID, String newOwnerName) {
        this.claim = claim;
        this.oldOwnerUUID = oldOwnerUUID;
        this.oldOwnerName = oldOwnerName;
        this.newOwnerUUID = newOwnerUUID;
        this.newOwnerName = newOwnerName;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claim whose owner has changed.
     *
     * @return The claim.
     */
    public Claim getClaim() {
        return claim;
    }
    
    /**
     * Gets the old owner's UUID.
     *
     * @return The old owner's UUID.
     */
    public UUID getOldOwnerUUID() {
        return oldOwnerUUID;
    }
    
    /**
     * Gets the old owner's name.
     *
     * @return The old owner's name.
     */
    public String getOldOwnerName() {
        return oldOwnerName;
    }
    
    /**
     * Gets the new owner's UUID.
     *
     * @return The new owner's UUID.
     */
    public UUID getNewOwnerUUID() {
        return newOwnerUUID;
    }
    
    /**
     * Gets the new owner's name.
     *
     * @return The new owner's name.
     */
    public String getNewOwnerName() {
        return newOwnerName;
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
