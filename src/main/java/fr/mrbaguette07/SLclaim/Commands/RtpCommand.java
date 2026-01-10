package fr.mrbaguette07.SLclaim.Commands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import fr.mrbaguette07.SLclaim.ClaimMain;
import fr.mrbaguette07.SLclaim.SLclaim;
import fr.mrbaguette07.SLclaim.Config.ClaimLanguage;
import fr.mrbaguette07.SLclaim.MultiServer.MultiServerManager;
import fr.mrbaguette07.SLclaim.MultiServer.ServerType;
import fr.mrbaguette07.SLclaim.Support.ClaimVault;
import net.md_5.bungee.api.ChatColor;

import java.io.File;
import java.io.IOException;

/**
 * Commande /rtp pour téléporter aléatoirement un joueur
 */
public class RtpCommand implements CommandExecutor, TabCompleter {

    // ***************
    // *  Variables  *
    // ***************

    /** Instance de SLclaim */
    private SLclaim instance;
    
    /** Configuration RTP */
    private FileConfiguration rtpConfig;
    
    /** Fichier de configuration */
    private File rtpFile;
    
    /** Map des joueurs en cours de téléportation (warmup) */
    private Map<UUID, BukkitTask> warmupTasks = new ConcurrentHashMap<>();
    
    /** Map des cooldowns des joueurs */
    private Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    
    /** Générateur de nombres aléatoires */
    private Random random = new Random();
    
    /** Set des blocs dangereux */
    private Set<Material> unsafeBlocks = ConcurrentHashMap.newKeySet();

    // ******************
    // *  Constructors  *
    // ******************

    /**
     * Constructeur de RtpCommand
     *
     * @param instance L'instance du plugin SLclaim
     */
    public RtpCommand(SLclaim instance) {
        this.instance = instance;
        loadConfig();
    }

    // ******************
    // *  Configuration *
    // ******************

    /**
     * Charge la configuration RTP
     */
    public void loadConfig() {
        rtpFile = new File(instance.getDataFolder(), "rtp.yml");
        
        if (!rtpFile.exists()) {
            instance.saveResource("rtp.yml", false);
        }
        
        // Update config with missing keys
        updateRtpConfigWithDefaults(rtpFile);
        
        rtpConfig = YamlConfiguration.loadConfiguration(rtpFile);
        
        // Charger les blocs dangereux
        unsafeBlocks.clear();
        List<String> unsafeBlocksList = rtpConfig.getStringList("unsafe-blocks");
        for (String blockName : unsafeBlocksList) {
            try {
                Material mat = Material.valueOf(blockName.toUpperCase());
                unsafeBlocks.add(mat);
            } catch (IllegalArgumentException e) {
                instance.getLogger().warning("Bloc dangereux invalide dans rtp.yml: " + blockName);
            }
        }
    }

    /**
     * Recharge la configuration RTP
     */
    public void reloadConfig() {
        loadConfig();
    }

    // ******************
    // *  Commande      *
    // ******************

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Vérifier si c'est un joueur
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Cette commande ne peut être utilisée que par un joueur.");
            return true;
        }

        Player player = (Player) sender;
        ClaimLanguage lang = instance.getLanguage();

        // Vérifier si RTP est activé
        if (!rtpConfig.getBoolean("enabled", true)) {
            player.sendMessage(lang.getMessage("rtp-disabled"));
            return true;
        }

        // Vérifier la permission
        if (!player.hasPermission("slclaim.rtp.use")) {
            player.sendMessage(lang.getMessage("rtp-no-permission"));
            return true;
        }

        // Vérifier le mode multi-serveur via multiserver.yml
        // Si on est sur un serveur LOBBY, transférer vers un serveur SURVIVAL
        MultiServerManager multiManager = instance.getMultiServerManager();
        if (multiManager != null && multiManager.isEnabled()) {
            ServerType serverType = multiManager.getConfig().getServerType();
            if (serverType == ServerType.LOBBY) {
                // Récupérer uniquement les serveurs SURVIVAL en ligne
                List<String> onlineSurvivalServers = multiManager.getOnlineSurvivalServers();
                
                if (onlineSurvivalServers == null || onlineSurvivalServers.isEmpty()) {
                    player.sendMessage(lang.getMessage("rtp-no-survival-server-available"));
                    return true;
                }
                
                // Choisir un serveur survival aléatoire parmi ceux en ligne
                String targetServer = onlineSurvivalServers.get(random.nextInt(onlineSurvivalServers.size()));
                
                // Informer le joueur du transfert
                player.sendMessage(lang.getMessage("rtp-transferring-to-server"));
                
                // Log pour debug
                instance.info("RTP: Transfert du joueur " + player.getName() + " vers " + targetServer + " (serveurs en ligne: " + onlineSurvivalServers.size() + ")");
                
                // Envoyer le pending RTP via Redis pour que le serveur cible sache qu'il doit faire un RTP
                multiManager.sendPendingRtp(player.getUniqueId(), targetServer);
                
                // Transférer le joueur vers le serveur survival via BungeeCord
                instance.getMain().transferPlayerToServer(player, targetServer);
                return true;
            }
        }

        // Vérifier si le joueur est dans un monde autorisé
        List<String> allowedWorlds = rtpConfig.getStringList("allowed-worlds");
        if (!allowedWorlds.isEmpty() && !allowedWorlds.contains(player.getWorld().getName())) {
            player.sendMessage(lang.getMessage("rtp-world-not-allowed")
                    .replace("%world%", player.getWorld().getName()));
            return true;
        }

        // Vérifier si le joueur est déjà en cours de téléportation
        if (warmupTasks.containsKey(player.getUniqueId())) {
            player.sendMessage(lang.getMessage("rtp-already-teleporting"));
            return true;
        }

        // Vérifier le cooldown
        if (!player.hasPermission("slclaim.rtp.nocooldown")) {
            long cooldownEnd = cooldowns.getOrDefault(player.getUniqueId(), 0L);
            long now = System.currentTimeMillis();
            if (now < cooldownEnd) {
                long remaining = (cooldownEnd - now) / 1000;
                player.sendMessage(lang.getMessage("rtp-cooldown")
                        .replace("%time%", formatTime(remaining)));
                return true;
            }
        }

        // Vérifier le coût
        if (rtpConfig.getBoolean("cost-enabled", false)) {
            ClaimVault vault = instance.getVault();
            if (vault != null) {
                double cost = getCostForPlayer(player);
                if (cost > 0) {
                    double balance = vault.getPlayerBalance(player.getName());
                    if (balance < cost) {
                        player.sendMessage(lang.getMessage("rtp-not-enough-money")
                                .replace("%cost%", String.format("%.2f", cost))
                                .replace("%balance%", String.format("%.2f", balance)));
                        return true;
                    }
                }
            }
        }

        // Rechercher un emplacement sûr
        player.sendMessage(lang.getMessage("rtp-searching-location"));
        
        findSafeLocation(player).thenAccept(location -> {
            if (location == null) {
                Bukkit.getScheduler().runTask(instance, () -> {
                    player.sendMessage(lang.getMessage("rtp-no-location-found"));
                });
                return;
            }

            Bukkit.getScheduler().runTask(instance, () -> {
                startTeleportation(player, location);
            });
        });

        return true;
    }

    // ******************
    // *  Téléportation *
    // ******************

    /**
     * Démarre le processus de téléportation avec warmup
     */
    private void startTeleportation(Player player, Location destination) {
        ClaimLanguage lang = instance.getLanguage();
        int warmup = getWarmupForPlayer(player);

        // Bypass du warmup
        if (player.hasPermission("slclaim.rtp.bypass") || warmup <= 0) {
            completeTeleportation(player, destination);
            return;
        }

        // Sauvegarder la position initiale
        Location startLocation = player.getLocation().clone();
        double moveTolerance = rtpConfig.getDouble("move-tolerance", 1.5);
        boolean cancelOnMove = rtpConfig.getBoolean("cancel-on-move", true);
        boolean cancelOnDamage = rtpConfig.getBoolean("cancel-on-damage", true);

        // Afficher le message de warmup
        player.sendMessage(lang.getMessage("rtp-warmup-start")
                .replace("%time%", String.valueOf(warmup)));

        // Jouer le son de début
        if (rtpConfig.getBoolean("sound-enabled", true)) {
            try {
                Sound sound = Sound.valueOf(rtpConfig.getString("warmup-sound", "BLOCK_NOTE_BLOCK_PLING"));
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                // Son invalide, ignorer
            }
        }

        // Créer la tâche de warmup
        BukkitTask task = new BukkitRunnable() {
            int countdown = warmup;
            double lastHealth = player.getHealth();

            @Override
            public void run() {
                // Vérifier si le joueur est toujours en ligne
                if (!player.isOnline()) {
                    cancelTeleportation(player, false);
                    return;
                }

                // Vérifier le mouvement
                if (cancelOnMove) {
                    double distance = player.getLocation().distance(startLocation);
                    if (distance > moveTolerance) {
                        cancelTeleportation(player, true);
                        player.sendMessage(lang.getMessage("rtp-cancelled-move"));
                        return;
                    }
                }

                // Vérifier les dégâts
                if (cancelOnDamage && player.getHealth() < lastHealth) {
                    cancelTeleportation(player, true);
                    player.sendMessage(lang.getMessage("rtp-cancelled-damage"));
                    return;
                }
                lastHealth = player.getHealth();

                // Afficher le titre
                if (rtpConfig.getBoolean("title-enabled", true)) {
                    player.sendTitle(
                            ChatColor.GOLD + "Téléportation",
                            ChatColor.YELLOW + String.valueOf(countdown) + " seconde" + (countdown > 1 ? "s" : ""),
                            0, 25, 5
                    );
                }

                // Particules
                if (rtpConfig.getBoolean("particles-enabled", true)) {
                    try {
                        Particle particle = Particle.valueOf(rtpConfig.getString("particle-type", "PORTAL"));
                        player.getWorld().spawnParticle(particle, player.getLocation().add(0, 1, 0), 30, 0.5, 1, 0.5, 0.1);
                    } catch (IllegalArgumentException e) {
                        // Particule invalide, ignorer
                    }
                }

                countdown--;

                // Téléportation terminée
                if (countdown < 0) {
                    warmupTasks.remove(player.getUniqueId());
                    this.cancel();
                    completeTeleportation(player, destination);
                }
            }
        }.runTaskTimer(instance, 0L, 20L);

        warmupTasks.put(player.getUniqueId(), task);
    }

    /**
     * Complète la téléportation
     */
    private void completeTeleportation(Player player, Location destination) {
        ClaimLanguage lang = instance.getLanguage();

        // Prélever le coût
        if (rtpConfig.getBoolean("cost-enabled", false)) {
            ClaimVault vault = instance.getVault();
            if (vault != null) {
                double cost = getCostForPlayer(player);
                if (cost > 0) {
                    vault.removePlayerBalance(player.getName(), cost);
                    player.sendMessage(lang.getMessage("rtp-cost-deducted")
                            .replace("%cost%", String.format("%.2f", cost)));
                }
            }
        }

        // Téléporter le joueur
        player.teleport(destination);

        // Jouer le son
        if (rtpConfig.getBoolean("sound-enabled", true)) {
            try {
                Sound sound = Sound.valueOf(rtpConfig.getString("teleport-sound", "ENTITY_ENDERMAN_TELEPORT"));
                player.playSound(destination, sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                // Son invalide, ignorer
            }
        }

        // Message de succès
        player.sendMessage(lang.getMessage("rtp-success")
                .replace("%x%", String.valueOf(destination.getBlockX()))
                .replace("%y%", String.valueOf(destination.getBlockY()))
                .replace("%z%", String.valueOf(destination.getBlockZ())));

        // Appliquer le cooldown
        if (!player.hasPermission("slclaim.rtp.nocooldown")) {
            int cooldown = getCooldownForPlayer(player);
            if (cooldown > 0) {
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldown * 1000L));
            }
        }
    }

    /**
     * Annule la téléportation en cours
     */
    public void cancelTeleportation(Player player, boolean notify) {
        BukkitTask task = warmupTasks.remove(player.getUniqueId());
        if (task != null) {
            task.cancel();
        }
    }

    // **************************
    // *  Recherche emplacement *
    // **************************

    /**
     * Recherche un emplacement sûr de manière asynchrone
     */
    private CompletableFuture<Location> findSafeLocation(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            World world = player.getWorld();
            int minX = rtpConfig.getInt("coordinates.min-x", -10000);
            int maxX = rtpConfig.getInt("coordinates.max-x", 10000);
            int minZ = rtpConfig.getInt("coordinates.min-z", -10000);
            int maxZ = rtpConfig.getInt("coordinates.max-z", 10000);
            int minY = rtpConfig.getInt("height.min-y", 60);
            int maxY = rtpConfig.getInt("height.max-y", 320);
            int maxAttempts = rtpConfig.getInt("max-attempts", 50);
            boolean allowInClaims = rtpConfig.getBoolean("allow-tp-in-claims", false);

            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int x = minX + random.nextInt(maxX - minX + 1);
                int z = minZ + random.nextInt(maxZ - minZ + 1);

                // Trouver un emplacement sûr
                Location safe = findSafeY(world, x, z, minY, maxY);
                if (safe == null) continue;

                // Vérifier si c'est dans un claim
                if (!allowInClaims) {
                    ClaimMain claimMain = instance.getMain();
                    org.bukkit.Chunk chunk = world.getChunkAt(safe);
                    if (claimMain.getClaim(chunk) != null) {
                        continue;
                    }
                }

                // Centrer sur le bloc
                safe.setX(safe.getBlockX() + 0.5);
                safe.setZ(safe.getBlockZ() + 0.5);
                safe.setYaw(player.getLocation().getYaw());
                safe.setPitch(player.getLocation().getPitch());

                return safe;
            }

            return null;
        });
    }

    /**
     * Trouve une hauteur Y sûre pour les coordonnées données
     */
    private Location findSafeY(World world, int x, int z, int minY, int maxY) {
        // Commencer par le haut et descendre
        for (int y = maxY; y >= minY; y--) {
            Block block = world.getBlockAt(x, y, z);
            Block below = world.getBlockAt(x, y - 1, z);
            Block above = world.getBlockAt(x, y + 1, z);

            // Vérifier si le bloc du dessous est solide et sûr
            if (below.getType().isSolid() && 
                !unsafeBlocks.contains(below.getType()) &&
                !unsafeBlocks.contains(block.getType()) &&
                !unsafeBlocks.contains(above.getType()) &&
                block.isPassable() && 
                above.isPassable()) {
                
                return new Location(world, x, y, z);
            }
        }
        return null;
    }

    // ******************
    // *  Utilitaires   *
    // ******************

    /**
     * Obtient le warmup pour un joueur selon ses permissions
     */
    private int getWarmupForPlayer(Player player) {
        // Vérifier le bypass
        if (player.hasPermission("slclaim.rtp.bypass")) {
            return 0;
        }

        // Vérifier les permissions personnalisées
        if (rtpConfig.contains("warmup-permissions")) {
            for (String perm : rtpConfig.getConfigurationSection("warmup-permissions").getKeys(false)) {
                if (player.hasPermission(perm)) {
                    return rtpConfig.getInt("warmup-permissions." + perm);
                }
            }
        }

        return rtpConfig.getInt("default-warmup", 5);
    }

    /**
     * Obtient le cooldown pour un joueur selon ses permissions
     */
    private int getCooldownForPlayer(Player player) {
        // Vérifier le bypass
        if (player.hasPermission("slclaim.rtp.nocooldown")) {
            return 0;
        }

        // Vérifier les permissions personnalisées
        if (rtpConfig.contains("cooldown-permissions")) {
            for (String perm : rtpConfig.getConfigurationSection("cooldown-permissions").getKeys(false)) {
                if (player.hasPermission(perm)) {
                    return rtpConfig.getInt("cooldown-permissions." + perm);
                }
            }
        }

        return rtpConfig.getInt("default-cooldown", 300);
    }

    /**
     * Obtient le coût pour un joueur selon ses permissions
     */
    private double getCostForPlayer(Player player) {
        // Vérifier les permissions personnalisées
        if (rtpConfig.contains("cost-permissions")) {
            for (String perm : rtpConfig.getConfigurationSection("cost-permissions").getKeys(false)) {
                if (player.hasPermission(perm)) {
                    return rtpConfig.getDouble("cost-permissions." + perm);
                }
            }
        }

        return rtpConfig.getDouble("default-cost", 100.0);
    }

    /**
     * Formate le temps en format lisible
     */
    private String formatTime(long seconds) {
        if (seconds < 60) {
            return seconds + "s";
        } else if (seconds < 3600) {
            long minutes = seconds / 60;
            long secs = seconds % 60;
            return minutes + "m " + secs + "s";
        } else {
            long hours = seconds / 3600;
            long minutes = (seconds % 3600) / 60;
            return hours + "h " + minutes + "m";
        }
    }

    // ******************
    // *  Tab Complete  *
    // ******************

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return Collections.emptyList();
    }

    // ******************
    // *  Getters       *
    // ******************

    /**
     * @return La configuration RTP
     */
    public FileConfiguration getRtpConfig() {
        return rtpConfig;
    }

    /**
     * Vérifie si un joueur a une téléportation en cours
     */
    public boolean hasPendingTeleport(UUID playerId) {
        return warmupTasks.containsKey(playerId);
    }
    
    /**
     * Met à jour rtp.yml avec les clés manquantes du fichier par défaut.
     * Cela garantit que les nouvelles options de configuration sont ajoutées
     * sans écraser les paramètres de l'utilisateur.
     *
     * @param configFile Le fichier rtp.yml
     */
    private void updateRtpConfigWithDefaults(File configFile) {
        try {
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            java.io.InputStream defConfigStream = instance.getResource("rtp.yml");
            if (defConfigStream == null) return;
            
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(
                new java.io.InputStreamReader(defConfigStream));
            
            boolean changed = false;
            
            // Ajouter les clés manquantes
            for (String key : defConfig.getKeys(true)) {
                if (!config.contains(key)) {
                    config.set(key, defConfig.get(key));
                    changed = true;
                }
            }
            
            if (changed) {
                config.save(configFile);
            }
        } catch (Exception e) {
            instance.getLogger().warning("Échec de la mise à jour de rtp.yml: " + e.getMessage());
        }
    }
}
