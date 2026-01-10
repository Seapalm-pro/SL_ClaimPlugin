package fr.mrbaguette07.SLclaim.API.Listeners;

import org.bukkit.Chunk;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Event that is triggered when a chunk is added to a claim.
 * This event allows other parts of the plugin to respond to chunk additions.
 */
public class ClaimChunkAddEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The claim */
    private final Claim claim;
    
    /** The chunk added */
    private final Chunk chunk;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for ClaimChunkAddEvent.
     *
     * @param claim The claim to which a chunk was added.
     * @param chunk The added chunk.
     */
    public ClaimChunkAddEvent(Claim claim, Chunk chunk) {
        this.claim = claim;
        this.chunk = chunk;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claim to which a chunk was added.
     *
     * @return The claim.
     */
    public Claim getClaim() {
        return claim;
    }
    
    /**
     * Gets the chunk that was added.
     *
     * @return The added chunk.
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
