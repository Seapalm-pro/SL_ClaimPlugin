package fr.mrbaguette07.SCS.MultiServer;

/**
 * Represents the type of server in a multi-server setup.
 */
public enum ServerType {
    
    /**
     * Survival server - Full claim functionality available.
     * Players can claim chunks, manage claims, and use all features.
     */
    SURVIVAL,
    
    /**
     * Lobby server - Limited claim functionality.
     * Players can view and manage existing claims, but cannot claim new chunks.
     */
    LOBBY,
    
    /**
     * Standalone server - Traditional single server mode.
     * No multi-server synchronization.
     */
    STANDALONE
}
