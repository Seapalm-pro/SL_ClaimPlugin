package fr.mrbaguette07.SLclaim.API.Listeners;

import org.bukkit.Chunk;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Event that is triggered when a chunk is removed from a claim.
 * This event allows other parts of the plugin to respond to chunk removals.
 */
public class ClaimChunkRemoveEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The claim */
    private final Claim claim;
    
    /** The chunk removed */
    private final Chunk chunk;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for ClaimChunkRemoveEvent.
     *
     * @param claim The claim from which a chunk was removed.
     * @param chunk The removed chunk.
     */
    public ClaimChunkRemoveEvent(Claim claim, Chunk chunk) {
        this.claim = claim;
        this.chunk = chunk;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claim from which a chunk was removed.
     *
     * @return The claim.
     */
    public Claim getClaim() {
        return claim;
    }
    
    /**
     * Gets the chunk that was removed.
     *
     * @return The removed chunk.
     */
    public Chunk getChunk() {
        return chunk;
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
