package fr.mrbaguette07.SLclaim.Velocity;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Commands for the Velocity plugin.
 */
public class VelocityCommands implements SimpleCommand {
    
    private final SCSVelocityPlugin plugin;
    
    public VelocityCommands(SCSVelocityPlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();
        
        if (args.length == 0) {
            sendHelp(source);
            return;
        }
        
        switch (args[0].toLowerCase()) {
            case "reload":
                if (!source.hasPermission("slclaim.velocity.reload")) {
                    source.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                    return;
                }
                plugin.getConfig().load();
                source.sendMessage(Component.text("Configuration reloaded!").color(NamedTextColor.GREEN));
                break;
                
            case "status":
                if (!source.hasPermission("slclaim.velocity.status")) {
                    source.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                    return;
                }
                sendStatus(source);
                break;
                
            case "servers":
                if (!source.hasPermission("slclaim.velocity.servers")) {
                    source.sendMessage(Component.text("You don't have permission to use this command.").color(NamedTextColor.RED));
                    return;
                }
                sendServerList(source);
                break;
                
            default:
                sendHelp(source);
                break;
        }
    }
    
    private void sendHelp(CommandSource source) {
        source.sendMessage(Component.text("=== SLclaim Velocity ===").color(NamedTextColor.GOLD));
        source.sendMessage(Component.text("/slclaimvelocity reload - Recharger la configuration").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/slclaimvelocity status - Afficher le statut du plugin").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/slclaimvelocity servers - Lister les serveurs configurés").color(NamedTextColor.YELLOW));
    }
    
    private void sendStatus(CommandSource source) {
        source.sendMessage(Component.text("=== SLclaim Velocity Status ===").color(NamedTextColor.GOLD));
        
        boolean redisConnected = plugin.getRedisManager() != null && plugin.getRedisManager().isConnected();
        source.sendMessage(Component.text("Redis: ")
            .append(redisConnected 
                ? Component.text("Connected").color(NamedTextColor.GREEN)
                : Component.text("Disconnected").color(NamedTextColor.RED)));
        
        source.sendMessage(Component.text("Survival servers: " + plugin.getConfig().getSurvivalServers().size()).color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("Lobby servers: " + plugin.getConfig().getLobbyServers().size()).color(NamedTextColor.YELLOW));
    }
    
    private void sendServerList(CommandSource source) {
        source.sendMessage(Component.text("=== Configured Servers ===").color(NamedTextColor.GOLD));
        
        source.sendMessage(Component.text("Survival servers:").color(NamedTextColor.GREEN));
        for (String server : plugin.getConfig().getSurvivalServers()) {
            boolean online = plugin.getServer().getServer(server).isPresent();
            source.sendMessage(Component.text("  - " + server + " ")
                .append(online 
                    ? Component.text("[ONLINE]").color(NamedTextColor.GREEN)
                    : Component.text("[OFFLINE]").color(NamedTextColor.RED)));
        }
        
        source.sendMessage(Component.text("Lobby servers:").color(NamedTextColor.AQUA));
        for (String server : plugin.getConfig().getLobbyServers()) {
            boolean online = plugin.getServer().getServer(server).isPresent();
            source.sendMessage(Component.text("  - " + server + " ")
                .append(online 
                    ? Component.text("[ONLINE]").color(NamedTextColor.GREEN)
                    : Component.text("[OFFLINE]").color(NamedTextColor.RED)));
        }
    }
    
    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        if (invocation.arguments().length <= 1) {
            return CompletableFuture.completedFuture(List.of("reload", "status", "servers"));
        }
        return CompletableFuture.completedFuture(List.of());
    }
    
    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("slclaim.velocity.admin");
    }
}
