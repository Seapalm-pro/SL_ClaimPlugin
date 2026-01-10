package fr.mrbaguette07.SLclaim.Guis.Bedrock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage.Type;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.mrbaguette07.SLclaim.SLclaim;
import fr.mrbaguette07.SLclaim.Types.Claim;

/**
 * Class representing the Claim GUI.
 */
public class BClaimMainGui {

	
    // ***************
    // *  Variables  *
    // ***************

    
    /** Instance of SLclaim */
    private final SLclaim instance;
    
    /** Floodgate Player */
    private final FloodgatePlayer floodgatePlayer;

    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the BClaimMainGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SLclaim plugin.
     */
    public BClaimMainGui(Player player, Claim claim, SLclaim instance) {
    	this.instance = instance;
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
    	
        // Création d'un formulaire simple
    	SimpleForm form = SimpleForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-main-title")
	    			.replace("%name%", claim.getName()))
	        .button(instance.getLanguage().getMessage("bedrock-manage-bans-title").replace("%bans-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getBans().size()))), Type.URL, "https://i.ibb.co/VWH3qdRs/banned.png")
	        .button(instance.getLanguage().getMessage("bedrock-manage-members-title").replace("%members-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getMembers().size()))), Type.URL, "https://i.ibb.co/YTh2zjBT/members.png")
	        .button(instance.getLanguage().getMessage("bedrock-manage-chunks-title").replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getChunks().size()))), Type.URL, "https://i.ibb.co/kg1gN8V3/chunks.png")
	        .button(instance.getLanguage().getMessage("bedrock-manage-settings-title"), Type.URL, "https://i.ibb.co/NgvGqQYt/settings.png")
	        .button(instance.getLanguage().getMessage("bedrock-teleport-claim-title"), Type.URL, "https://i.ibb.co/jkxBH09F/tp.png")
	        .button(instance.getLanguage().getMessage("bedrock-unclaim-title"), Type.URL, "https://i.ibb.co/PGqsh65n/unclaim.png")
	        .validResultHandler(response -> {
	        	int buttonId = response.clickedButtonId();
	        	switch(buttonId) {
		        	case 0:
		        		if(checkPermButton(player,"manage-bans")) {
		        			new BClaimBansGui(player,claim,instance);
		        		}
		        		break;
		        	case 1:
		        		if(checkPermButton(player,"manage-members")) {
		        			new BClaimMembersGui(player,claim,instance);
		        		}
		        		break;
		        	case 2:
		        		if(checkPermButton(player,"manage-chunks")) {
		        			new BClaimChunksGui(player,claim,instance);
		        		}
		        		break;
		        	case 3:
		        		if(checkPermButton(player,"manage-settings")) {
		        			new BClaimSettingsGui(player,claim,instance);
		        		}
		        		break;
		        	case 4:
		        		if(checkPermButton(player,"teleport-claim")) {
		        			instance.getMain().goClaim(player, claim.getLocation());
		        		}
		        		break;
		        	case 5:
		        		if(checkPermButton(player,"unclaim")) {
		        			Bukkit.dispatchCommand(player, "unclaim "+claim.getName());
		        		}
		        		break;
	        	}
	        })
	        .build();
        floodgatePlayer.sendForm(form);
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Checks if the player has the permission for the specified key.
     *
     * @param player The player to check.
     * @param key    The key to check permission for.
     * @return True if the player has the permission, otherwise false.
     */
    public boolean checkPermButton(Player player, String key) {
        switch (key) {
        	case "unclaim":
        		return instance.getPlayerMain().checkPermPlayer(player, "slclaim.command.unclaim");
            case "manage-members":
                return instance.getPlayerMain().checkPermPlayer(player, "slclaim.command.claim.members");
            case "manage-bans":
                return instance.getPlayerMain().checkPermPlayer(player, "slclaim.command.claim.bans");
            case "manage-settings":
                return instance.getPlayerMain().checkPermPlayer(player, "slclaim.command.claim.settings");
            case "manage-chunks":
                return instance.getPlayerMain().checkPermPlayer(player, "slclaim.command.claim.chunks");
            case "claim-info":
                return instance.getPlayerMain().checkPermPlayer(player, "slclaim.command.claim.list");
            case "teleport-claim":
            	return instance.getPlayerMain().checkPermPlayer(player, "slclaim.command.claim.tp");
            default:
                return false;
        }
    }

}
