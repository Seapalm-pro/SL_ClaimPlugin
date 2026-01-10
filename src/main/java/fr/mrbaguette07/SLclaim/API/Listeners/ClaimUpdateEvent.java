package fr.mrbaguette07.SLclaim.API.Listeners;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Event that is triggered when a claim is updated.
 * This event allows other parts of the plugin to respond to claim modifications.
 */
public class ClaimUpdateEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The claim */
    private final Claim claim;
    
    /** The type of update */
    private final UpdateType updateType;
    
    /** The old value (may be null) */
    private final Object oldValue;
    
    /** The new value (may be null) */
    private final Object newValue;
    
    /**
     * Enum representing the type of update made to a claim.
     */
    public enum UpdateType {
        NAME,
        DESCRIPTION,
        LOCATION,
        OWNER,
        SALE_STATUS,
        SALE_PRICE,
        PERMISSION,
        OTHER
    }
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for ClaimUpdateEvent.
     *
     * @param claim The claim that has been updated.
     * @param updateType The type of update.
     * @param oldValue The old value before update.
     * @param newValue The new value after update.
     */
    public ClaimUpdateEvent(Claim claim, UpdateType updateType, Object oldValue, Object newValue) {
        this.claim = claim;
        this.updateType = updateType;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }
    
    /**
     * Constructor for ClaimUpdateEvent with only the claim.
     *
     * @param claim The claim that has been updated.
     * @param updateType The type of update.
     */
    public ClaimUpdateEvent(Claim claim, UpdateType updateType) {
        this(claim, updateType, null, null);
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claim that has been updated.
     *
     * @return The updated claim.
     */
    public Claim getClaim() {
        return claim;
    }
    
    /**
     * Gets the type of update.
     *
     * @return The update type.
     */
    public UpdateType getUpdateType() {
        return updateType;
    }
    
    /**
     * Gets the old value before update.
     *
     * @return The old value, may be null.
     */
    public Object getOldValue() {
        return oldValue;
    }
    
    /**
     * Gets the new value after update.
     *
     * @return The new value, may be null.
     */
    public Object getNewValue() {
        return newValue;
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
