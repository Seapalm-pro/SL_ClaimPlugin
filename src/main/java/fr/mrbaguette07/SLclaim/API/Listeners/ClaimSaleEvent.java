package fr.mrbaguette07.SLclaim.API.Listeners;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Event that is triggered when a claim's sale status changes.
 * This event allows other parts of the plugin to respond to sale status modifications.
 */
public class ClaimSaleEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The claim */
    private final Claim claim;
    
    /** Whether the claim is now for sale */
    private final boolean forSale;
    
    /** The sale price (0 if not for sale) */
    private final long price;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for ClaimSaleEvent.
     *
     * @param claim The claim whose sale status has changed.
     * @param forSale Whether the claim is now for sale.
     * @param price The sale price (0 if not for sale).
     */
    public ClaimSaleEvent(Claim claim, boolean forSale, long price) {
        this.claim = claim;
        this.forSale = forSale;
        this.price = price;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claim whose sale status has changed.
     *
     * @return The claim.
     */
    public Claim getClaim() {
        return claim;
    }
    
    /**
     * Checks if the claim is now for sale.
     *
     * @return true if the claim is for sale, false otherwise.
     */
    public boolean isForSale() {
        return forSale;
    }
    
    /**
     * Gets the sale price.
     *
     * @return The price, or 0 if not for sale.
     */
    public long getPrice() {
        return price;
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
