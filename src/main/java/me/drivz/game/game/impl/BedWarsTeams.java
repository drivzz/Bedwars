package me.drivz.game.game.impl;

import lombok.Getter;
import lombok.NonNull;
import me.drivz.game.PlayerCache;
import me.drivz.game.game.GameJoinMode;
import me.drivz.game.game.GameStopReason;
import me.drivz.game.game.GameType;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.mineacademy.fo.Common;
import org.mineacademy.fo.Messenger;
import org.mineacademy.fo.Valid;
import org.mineacademy.fo.collection.StrictList;
import org.mineacademy.fo.collection.StrictMap;
import org.mineacademy.fo.exception.EventHandledException;
import org.mineacademy.fo.model.BoxedMessage;
import org.mineacademy.fo.model.RandomNoRepeatPicker;
import org.mineacademy.fo.remain.CompEquipmentSlot;
import org.mineacademy.fo.remain.CompMaterial;
import org.mineacademy.fo.remain.Remain;

import java.util.*;

@Getter
public class BedWarsTeams extends BedWars {

    // the divider for players, i.e. if there are three times, then the amount of players is divided by 3 and split as evenly as possible
    private int teamAmount;
    private int playersPerTeam;

    private final RandomNoRepeatPicker<PlayerCache> allPlayersPicker = RandomNoRepeatPicker.newPicker(PlayerCache.class);
    private StrictMap<Team, Location> teamBeds = new StrictMap<>();

    private BedWarsTeams(String name) {
        super(name);
    }

    private BedWarsTeams(String name, @Nullable GameType type) {
        super(name, type);
    }

    @Override
    protected void onLoad() {
        this.teamAmount = this.getInteger("Teams", 2);
        this.playersPerTeam = this.getInteger("Players_Per_Team", 2);

        super.onLoad();

        // Cap
        this.setMaxPlayers(this.teamAmount * this.playersPerTeam);
        this.setMinPlayers(this.getMaxPlayers());
    }

    @Override
    protected void onSave() {
        super.onSave();

        // This is auto calculated
        this.set("Max_Players", null);
        this.set("Min_Players", null);

        this.set("Teams", this.teamAmount);
        this.set("Players_Per_Team", this.playersPerTeam);

        Valid.checkBoolean(this.teamAmount < Team.values().length, "Cannot have more than " + Team.values().length + " teams!");
    }

    // ------------------------------------------------------------------------------------------
    // Game logic
    // ------------------------------------------------------------------------------------------

    @Override
    protected void onGameLobbyStart() {
        super.onGameLobbyStart();

        this.teamBeds.clear();
    }

    @Override
    protected void onGameStart() {
        super.onGameStart();

        this.allPlayersPicker.setItems(this.getPlayers(GameJoinMode.PLAYING));

        Set<Location> usedBeds = new HashSet<>();
        Set<PlayerCache> assignedTeamPlayers = new HashSet<>();

        for (int teamId = 0; teamId < this.teamAmount; teamId++) {
            Team team = Team.values()[teamId];
            System.out.println("Loading team #" + (teamId + 1) + "/" + this.teamAmount
                    + " (remaining: " + allPlayersPicker.remaining() + ")");

            Location teamSpawnpoint = this.getPlayerSpawnpointPicker().pickRandom();
            Valid.checkNotNull(teamSpawnpoint, "Unable to pick spawnpoint for team " + team);

            // Find a bed
            Location closestBed = null;
            BlockFace closestFace = null;

            for (Map.Entry<Location, BlockFace> entry : this.getBedSpawnpoints().entrySet()) {
                Location bedLocation = entry.getKey();

                if (!usedBeds.contains(bedLocation) && (closestBed == null || bedLocation.distance(teamSpawnpoint) < closestBed.distance(teamSpawnpoint))) {
                    closestBed = bedLocation;
                    closestFace = entry.getValue();
                }
            }

            Valid.checkNotNull(closestFace, "Failed to assign a bed for team #" + teamId);
            this.teamBeds.put(team, closestBed);

            if (!CompMaterial.isBed(closestBed.getBlock()))
                Remain.setBed(closestBed, closestFace);

            usedBeds.add(closestBed);

            // Distribute players
            for (int teamPlayerIndex = 0; teamPlayerIndex < this.playersPerTeam; teamPlayerIndex++) {
                System.out.println("\tLoading player #" + (teamPlayerIndex + 1) + "/" + this.playersPerTeam);

                PlayerCache randomCache = this.allPlayersPicker.pickRandom();
                Valid.checkNotNull(randomCache, "Could not find a random player for " + team);

                Player randomPlayer = randomCache.toPlayer();

                randomCache.setPlayerTag("Team", team);
                randomCache.setPlayerTag("Spawnpoint", teamSpawnpoint);
                randomCache.setPlayerTag("BedLocation", closestBed);

                this.teleport(randomPlayer, teamSpawnpoint);

                // Give team players the same colorized equipment
                CompEquipmentSlot.applyArmor(randomPlayer, team.getColor());

                assignedTeamPlayers.add(randomCache);
            }
        }

        int noTeamPlayers = this.getPlayers(GameJoinMode.PLAYING).size() - assignedTeamPlayers.size();
        Valid.checkBoolean(noTeamPlayers == 0, "Failed to assign team to " + noTeamPlayers + " players!");
    }

    @Override
    protected void onGameStartFor(Player player, PlayerCache cache) {
        //super.onGameStartFor(player, cache); // We setup teams above
    }

    @Override
    protected void onGamePostStart() {
        //super.onGamePostStart(); // We setup beds above
    }

    @Override
    public void onBlockBreak(Player player, Block block, BlockBreakEvent event) {
        //super.onBlockBreak(player, block, event);

        Location enemyBedLocation = this.findBedStartLocation(block);
        Team enemyBedTeam = enemyBedLocation != null ? this.findTeam(enemyBedLocation) : null;

        if (enemyBedLocation == null || enemyBedTeam == null)
            this.cancelEvent();

        PlayerCache cache = PlayerCache.from(player);
        Team playerTeam = this.findTeam(cache);

        if (playerTeam == enemyBedTeam) {
            Messenger.error(player, "You cannot destroy your team's bed, fool!");

            this.cancelEvent();
        }

        this.teamBeds.remove(enemyBedTeam);

        for (Player enemyPlayer : this.findTeamPlayers(enemyBedTeam)) {
            PlayerCache.from(enemyPlayer).removePlayerTag("BedLocation");

            BoxedMessage.tell(enemyPlayer, "<center>&6&lBED DESTROYED\n\n",
                    "<center>&cYour team's bed has been destroyed\n" +
                            "<center>&cyou won't respawn on death!");
        }

        this.broadcastWarn("&e" + player.getName() + " has destroyed " + enemyBedTeam + " team's bed!");
        this.increaseBedsDestroyed();
    }

    @Override
    protected void onGameLeave(Player player) {
        //super.onGameLeave(player);

        if (this.isPlayed()) {
            Common.runLater(2, () -> {
                if (this.isStopped())
                    return;

                StrictList<Team> aliveTeams = this.findTeamsAlive();

                if (aliveTeams.size() == 1) {

                    for (Player lastPlayer : this.findTeamPlayers(aliveTeams.get(0))) {

                        if (this.getBedsDestroyed() >= this.teamAmount - 1) {
                            BoxedMessage.tell(lastPlayer, "<center>&a&lCONGRATULATIONS\n\n"
                                    + "<center>&7You are the last team standing\n"
                                    + "<center>&7and &awon &7the game!");

                            // Give rewards etc.
                        }
                    }

                    // Leave all players and stop the game
                    Common.runLater(2, () -> {
                        this.stop(GameStopReason.BEDWARS_TEAM_WIN);
                    });
                }
            });
        }
    }

    @Override
    public void onPvP(Player attacker, Player victim, EntityDamageByEntityEvent event) {
        super.onPvP(attacker, victim, event);

        Team attackerTeam = this.findTeam(attacker);
        Team victimTeam = this.findTeam(victim);

        if (attackerTeam == victimTeam) {
            Messenger.warn(attacker, "You cannot attack players in your own team (" + attackerTeam + ")");

            this.cancelEvent();
        }
    }

    @Override
    public void onPlayerInventoryClick(PlayerCache cache, InventoryClickEvent event) {
        super.onPlayerInventoryClick(cache, event);

        int slot = event.getSlot();
        Set<Integer> armorSlots = Common.newSet(36, 37, 38, 39);

        if (armorSlots.contains(slot))
            this.cancelEvent();
    }

    @Override
    public void onPlayerInventoryDrag(PlayerCache cache, InventoryDragEvent event) {
        super.onPlayerInventoryDrag(cache, event);
    }

    @Override
    public void onPlayerInteract(PlayerCache cache, PlayerInteractEvent event) {
        super.onPlayerInteract(cache, event);

        Player player = event.getPlayer();

        if (event.getAction().toString().contains("RIGHT_CLICK"))
            if (event.hasItem()) {
                ItemStack item = event.getItem();
                String type = item.getType().toString();
                EntityEquipment equipment = player.getEquipment();

                // Compatible with 1.8.8
                if ((type.contains("HELMET") && CompMaterial.isAir(equipment.getHelmet())) ||
                        (type.contains("CHESTPLATE") && CompMaterial.isAir(equipment.getChestplate())) ||
                        (type.contains("LEGGINGS") && CompMaterial.isAir(equipment.getLeggings())) ||
                        (type.contains("BOOTS") && CompMaterial.isAir(equipment.getBoots())))

                    this.cancelEvent();
            }
    }

    @Override
    public void onPlayerChat(PlayerCache cache, AsyncPlayerChatEvent event) throws EventHandledException {

        String message = event.getMessage();
        boolean globalChat = false;

        if (message.startsWith("!") && !message.equals("!")) {
            event.setMessage(message.substring(1));

            globalChat = true;
        }

        super.onPlayerChat(cache, event); // Handle editing

        // Global is handled in the super call above
        if (!globalChat) {
            final Team team = this.findTeam(cache);
            final List<Player> teamPlayers = this.findTeamPlayers(team);

            event.getRecipients().removeIf(recipient -> !teamPlayers.contains(recipient));
            event.setFormat(Common.colorize("&8[&7" + team.getChatColor() + team + " team&8] &f"
                    + cache.getPlayerName() + "&8: &f" + event.getMessage()));
        }
    }

    @Override
    public int getRemainingBedCount() {
        return this.teamBeds.size();
    }

    @Override
    protected boolean isBedWarsSetup() {
        return this.getPlayerSpawnpoints().size() >= this.teamAmount && this.getBedSpawnpoints().size() >= this.teamAmount;
    }

    private List<Player> findTeamPlayers(Team team) {
        List<Player> teamPlayers = new ArrayList<>();

        for (PlayerCache cache : this.getPlayers(GameJoinMode.PLAYING)) {
            Team playerTeam = this.findTeam(cache);

            if (playerTeam == team)
                teamPlayers.add(cache.toPlayer());
        }

        return teamPlayers;
    }

    private StrictList<Team> findTeamsAlive() {
        StrictList<Team> teams = new StrictList<>();

        for (PlayerCache cache : this.getPlayers(GameJoinMode.PLAYING)) {
            Team team = this.findTeam(cache);

            if (!teams.contains(team))
                teams.add(team);
        }

        return teams;
    }

    protected boolean hasTeam(Player player) {
        return PlayerCache.from(player).hasPlayerTag("Team");
    }

    @Nullable
    protected Team findTeam(Location bedStartLocation) {
        for (Map.Entry<Team, Location> entry : this.teamBeds.entrySet())
            if (Valid.locationEquals(bedStartLocation, entry.getValue()))
                return entry.getKey();

        return null;
    }

    @NonNull
    protected Team findTeam(Player player) {
        return this.findTeam(PlayerCache.from(player));
    }

    @NonNull
    protected Team findTeam(PlayerCache cache) {
        Team team = cache.getPlayerTag("Team");
        Valid.checkNotNull(team, cache.getPlayerName() + " has no team!");

        return team;
    }
}
