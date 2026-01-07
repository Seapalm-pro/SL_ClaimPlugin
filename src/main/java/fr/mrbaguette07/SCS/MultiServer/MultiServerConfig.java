package fr.mrbaguette07.SCS.MultiServer;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration class for multi-server setup.
 */
public class MultiServerConfig {
    
    // ***************
    // *  Variables  *
    // ***************
    
    /** Whether multi-server mode is enabled */
    private boolean enabled;
    
    /** The name of this server */
    private String serverName;
    
    /** The type of this server */
    private ServerType serverType;
    
    /** List of survival servers */
    private List<String> survivalServers;
    
    /** List of lobby servers */
    private List<String> lobbyServers;
    
    // Redis configuration
    private String redisHost;
    private int redisPort;
    private String redisPassword;
    private boolean redisSSL;
    private int redisDatabase;
    private String redisChannel;
    
    // MongoDB configuration
    private String mongoConnectionString;
    private String mongoDatabaseName;
    private String mongoClaimsCollection;
    private String mongoPlayersCollection;
    
    // ******************
    // *  Constructors  *
    // ******************
    
    public MultiServerConfig() {
        this.enabled = false;
        this.serverName = "server-1";
        this.serverType = ServerType.STANDALONE;
        this.survivalServers = new ArrayList<>();
        this.lobbyServers = new ArrayList<>();
        
        // Default Redis config
        this.redisHost = "localhost";
        this.redisPort = 6379;
        this.redisPassword = "";
        this.redisSSL = false;
        this.redisDatabase = 0;
        this.redisChannel = "SLclaim";
        
        // Default MongoDB config
        this.mongoConnectionString = "mongodb://localhost:27017";
        this.mongoDatabaseName = "SLclaim";
        this.mongoClaimsCollection = "claims";
        this.mongoPlayersCollection = "players";
    }
    
    // *********************
    // *  Getters/Setters  *
    // *********************
    
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getServerName() { return serverName; }
    public void setServerName(String serverName) { this.serverName = serverName; }
    
    public ServerType getServerType() { return serverType; }
    public void setServerType(ServerType serverType) { this.serverType = serverType; }
    
    public List<String> getSurvivalServers() { return survivalServers; }
    public void setSurvivalServers(List<String> survivalServers) { this.survivalServers = survivalServers; }
    
    public List<String> getLobbyServers() { return lobbyServers; }
    public void setLobbyServers(List<String> lobbyServers) { this.lobbyServers = lobbyServers; }
    
    public String getRedisHost() { return redisHost; }
    public void setRedisHost(String redisHost) { this.redisHost = redisHost; }
    
    public int getRedisPort() { return redisPort; }
    public void setRedisPort(int redisPort) { this.redisPort = redisPort; }
    
    public String getRedisPassword() { return redisPassword; }
    public void setRedisPassword(String redisPassword) { this.redisPassword = redisPassword; }
    
    public boolean isRedisSSL() { return redisSSL; }
    public void setRedisSSL(boolean redisSSL) { this.redisSSL = redisSSL; }
    
    public int getRedisDatabase() { return redisDatabase; }
    public void setRedisDatabase(int redisDatabase) { this.redisDatabase = redisDatabase; }
    
    public String getRedisChannel() { return redisChannel; }
    public void setRedisChannel(String redisChannel) { this.redisChannel = redisChannel; }
    
    public String getMongoConnectionString() { return mongoConnectionString; }
    public void setMongoConnectionString(String mongoConnectionString) { this.mongoConnectionString = mongoConnectionString; }
    
    public String getMongoDatabaseName() { return mongoDatabaseName; }
    public void setMongoDatabaseName(String mongoDatabaseName) { this.mongoDatabaseName = mongoDatabaseName; }
    
    public String getMongoClaimsCollection() { return mongoClaimsCollection; }
    public void setMongoClaimsCollection(String mongoClaimsCollection) { this.mongoClaimsCollection = mongoClaimsCollection; }
    
    public String getMongoPlayersCollection() { return mongoPlayersCollection; }
    public void setMongoPlayersCollection(String mongoPlayersCollection) { this.mongoPlayersCollection = mongoPlayersCollection; }
    
    /**
     * Checks if this server is a survival server.
     * @return true if survival server
     */
    public boolean isSurvivalServer() {
        return serverType == ServerType.SURVIVAL;
    }
    
    /**
     * Checks if this server is a lobby server.
     * @return true if lobby server
     */
    public boolean isLobbyServer() {
        return serverType == ServerType.LOBBY;
    }
    
    /**
     * Checks if claiming is allowed on this server.
     * @return true if claiming is allowed (survival servers only)
     */
    public boolean canClaim() {
        return serverType == ServerType.SURVIVAL || serverType == ServerType.STANDALONE;
    }
}
