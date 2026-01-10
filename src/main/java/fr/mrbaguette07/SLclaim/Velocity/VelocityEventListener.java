package fr.mrbaguette07.SLclaim.Velocity;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerPing;

import fr.mrbaguette07.SLclaim.MultiServer.RedisMessage;
import fr.mrbaguette07.SLclaim.MultiServer.RedisMessage.MessageType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Event listener for the Velocity plugin.
 */
public class VelocityEventListener {
    
    private final SCSVelocityPlugin plugin;
    
    public VelocityEventListener(SCSVelocityPlugin plugin) {
        this.plugin = plugin;
        
        // Start server status checker
        startServerStatusChecker();
    }
    
    /**
     * Starts a periodic task to check server status and broadcast to all servers.
     */
    private void startServerStatusChecker() {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            checkAndBroadcastServerStatus();
        }).repeat(10, TimeUnit.SECONDS).schedule();
        
        plugin.getLogger().info("Server status checker started (every 10 seconds)");
    }
    
    /**
     * Checks all registered servers and broadcasts their status via Redis.
     */
    private void checkAndBroadcastServerStatus() {
        List<String> onlineServers = new ArrayList<>();
        List<String> offlineServers = new ArrayList<>();
        
        for (RegisteredServer server : plugin.getServer().getAllServers()) {
            String serverName = server.getServerInfo().getName();
            
            // Ping the server to check if it's online
            server.ping().orTimeout(5, TimeUnit.SECONDS).whenComplete((ping, error) -> {
                if (error != null || ping == null) {
                    // Server is offline
                    broadcastServerStatus(serverName, false);
                } else {
                    // Server is online
                    broadcastServerStatus(serverName, true);
                }
            });
        }
    }
    
    /**
     * Broadcasts a server's online/offline status via Redis.
     */
    private void broadcastServerStatus(String serverName, boolean isOnline) {
        RedisMessage message = new RedisMessage(MessageType.SERVER_HEARTBEAT, "velocity-proxy")
            .addData("server_name", serverName)
            .addData("is_online", isOnline)
            .addData("timestamp", System.currentTimeMillis());
        
        plugin.getRedisManager().publish(message);
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
