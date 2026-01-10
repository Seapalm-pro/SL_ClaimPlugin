package fr.mrbaguette07.SLclaim.Velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Velocity plugin for SLclaim multi-server support.
 * This plugin handles player transfers and cross-server communication.
 */
@Plugin(
    id = "slclaim-velocity",
    name = "SLclaim Velocity",
    version = "1.12.0.0",
    description = "Multi-server support for SLclaim",
    authors = {"MrBaguette07"}
)
public class SCSVelocityPlugin {
    
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    
    private VelocityRedisManager redisManager;
    private VelocityConfig config;
    
    @Inject
    public SCSVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }
    
    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        logger.info("SLclaim Velocity démarre...");
        
        // Create data directory
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("Échec de la création du répertoire de données", e);
            }
        }
        
        // Load configuration
        config = new VelocityConfig(dataDirectory, logger);
        config.load();
        
        if (!config.isEnabled()) {
            logger.info("SLclaim Velocity est désactivé dans la configuration.");
            return;
        }
        
        // Initialize Redis connection
        redisManager = new VelocityRedisManager(this, config);
        if (!redisManager.connect()) {
            logger.error("Échec de la connexion à Redis. Plugin désactivé.");
            return;
        }
        
        // Register commands
        server.getCommandManager().register("scsvelocity", new VelocityCommands(this));
        
        // Register event listeners
        server.getEventManager().register(this, new VelocityEventListener(this));
        
        logger.info("SLclaim Velocity a été activé !");
    }
    
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (redisManager != null) {
            redisManager.disconnect();
        }
        logger.info("SLclaim Velocity a été désactivé !");
    }
    
    public ProxyServer getServer() {
        return server;
    }
    
    public Logger getLogger() {
        return logger;
    }
    
    public Path getDataDirectory() {
        return dataDirectory;
    }
    
    public VelocityRedisManager getRedisManager() {
        return redisManager;
    }
    
    public VelocityConfig getConfig() {
        return config;
    }
}
