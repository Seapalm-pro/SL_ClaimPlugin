package fr.mrbaguette07.SCS.Velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;

import fr.mrbaguette07.SCS.MultiServer.RedisMessage;
import fr.mrbaguette07.SCS.MultiServer.RedisMessage.MessageType;

/**
 * Event listener for the Velocity plugin.
 */
public class VelocityEventListener {
    
    private final SCSVelocityPlugin plugin;
    
    public VelocityEventListener(SCSVelocityPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Subscribe
    public void onPlayerLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        
        // Notify all servers about player connection
        RedisMessage message = new RedisMessage(MessageType.PLAYER_DATA_UPDATE, "velocity-proxy")
            .playerUUID(player.getUniqueId())
            .addData("event", "login")
            .addData("player_name", player.getUsername());
        
        plugin.getRedisManager().publish(message);
    }
    
    @Subscribe
    public void onPlayerDisconnect(DisconnectEvent event) {
        Player player = event.getPlayer();
        
        // Notify all servers about player disconnection
        RedisMessage message = new RedisMessage(MessageType.PLAYER_DATA_UPDATE, "velocity-proxy")
            .playerUUID(player.getUniqueId())
            .addData("event", "logout")
            .addData("player_name", player.getUsername());
        
        plugin.getRedisManager().publish(message);
    }
    
    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getServer().getServerInfo().getName();
        
        // Notify servers about player server change
        RedisMessage message = new RedisMessage(MessageType.PLAYER_DATA_UPDATE, "velocity-proxy")
            .playerUUID(player.getUniqueId())
            .addData("event", "server_change")
            .addData("player_name", player.getUsername())
            .addData("current_server", serverName)
            .addData("is_survival", plugin.getConfig().isSurvivalServer(serverName))
            .addData("is_lobby", plugin.getConfig().isLobbyServer(serverName));
        
        plugin.getRedisManager().publish(message);
    }
}
