package fr.mrbaguette07.SLclaim.API.Listeners;

import java.util.UUID;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Event that is triggered when a member is removed from a claim.
 * This event allows other parts of the plugin to respond to member removals.
 */
public class ClaimMemberRemoveEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The claim */
    private final Claim claim;
    
    /** The UUID of the member removed */
    private final UUID memberUUID;
    
    /** The name of the member removed */
    private final String memberName;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for ClaimMemberRemoveEvent.
     *
     * @param claim The claim from which a member was removed.
     * @param memberUUID The UUID of the removed member.
     * @param memberName The name of the removed member.
     */
    public ClaimMemberRemoveEvent(Claim claim, UUID memberUUID, String memberName) {
        this.claim = claim;
        this.memberUUID = memberUUID;
        this.memberName = memberName;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claim from which a member was removed.
     *
     * @return The claim.
     */
    public Claim getClaim() {
        return claim;
    }
    
    /**
     * Gets the UUID of the removed member.
     *
     * @return The member's UUID.
     */
    public UUID getMemberUUID() {
        return memberUUID;
    }
    
    /**
     * Gets the name of the removed member.
     *
     * @return The member's name.
     */
    public String getMemberName() {
        return memberName;
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
