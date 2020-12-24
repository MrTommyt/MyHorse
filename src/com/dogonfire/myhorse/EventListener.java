package com.dogonfire.myhorse;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.vehicle.VehicleExitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

import java.util.Date;
import java.util.UUID;

public class EventListener implements Listener {
    private MyHorse plugin;

    EventListener(MyHorse p) {
        this.plugin = p;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        this.plugin.getOwnerManager().setLastLogin(event.getPlayer().getUniqueId(), new Date());
        if ((this.plugin.useUpdateNotifications) && ((event.getPlayer().isOp()) || (this.plugin.getPermissionsManager().hasPermission(event.getPlayer(), "myhorse.updates")))) {
            this.plugin.getServer().getScheduler().runTaskAsynchronously(this.plugin, new UpdateNotifier(this.plugin, event.getPlayer()));
        }
    }

    @EventHandler
    public void onDeathHorse(EntityDeathEvent event) {
        if (event.getEntityType().equals(EntityType.HORSE))
            event.getDrops().clear();
    }

    public void onEntityPortalEvent(EntityPortalEvent event) {
        if (event.getEntity().getType() != EntityType.HORSE) {
            return;
        }
        UUID horseIdentifier = event.getEntity().getUniqueId();
        if (!this.plugin.getHorseManager().isHorseOwned(horseIdentifier)) {
            return;
        }
        if (!this.plugin.isAllowedInWorld(event.getEntity().getWorld())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChunkUnload(ChunkUnloadEvent event) {
        if (!this.plugin.useHorseTeleportation) {
            return;
        }
        for (Entity entity : event.getChunk().getEntities()) {
            if (entity.getType() == EntityType.HORSE) {
                UUID horseIdentifier = entity.getUniqueId();
                if (this.plugin.getHorseManager().isHorseOwned(horseIdentifier)) {
                    this.plugin.logDebug("Saved horse onChunkUnload");

                    this.plugin.getHorseManager().setHorseLastSelectionPosition(horseIdentifier, entity.getLocation());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getVehicle() == null) {
            return;
        }
        Vehicle vehicle = (Vehicle) event.getPlayer().getVehicle();
        if (vehicle.getType() != EntityType.HORSE) {
            return;
        }
        if (!this.plugin.isAllowedInWorld(vehicle.getWorld())) {
            return;
        }
        UUID horseIdentifier = vehicle.getUniqueId();

        vehicle.eject();

        this.plugin.getHorseManager().setHorseLastSelectionPosition(horseIdentifier, vehicle.getLocation());
    }

    @EventHandler
    public void onVehicleExit(VehicleExitEvent event) {
        if (event.getExited().getType() != EntityType.PLAYER) {
            return;
        }
        if (event.getVehicle().getType() != EntityType.HORSE) {
            return;
        }
        if (!this.plugin.isAllowedInWorld(event.getExited().getWorld())) {
            return;
        }
        UUID horseIdentifier = event.getVehicle().getUniqueId();
        if (!(event.getExited() instanceof Player)) {
            return;
        }
        if (!this.plugin.getHorseManager().isHorseOwned(horseIdentifier)) {
            return;
        }
        this.plugin.getHorseManager().setHorseLastSelectionPosition(horseIdentifier, event.getExited().getLocation());
    }

    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered().getType() != EntityType.PLAYER) {
            return;
        }
        if (event.getVehicle().getType() != EntityType.HORSE) {
            this.plugin.logDebug(event.getVehicle().getType().name());
            return;
        }
        if (!this.plugin.isAllowedInWorld(event.getEntered().getWorld())) {
            return;
        }
        Player player = (Player) event.getEntered();

        UUID horseIdentifier = event.getVehicle().getUniqueId();

        String horseName = this.plugin.getHorseManager().getNameForHorse(horseIdentifier);
        if (horseName == null) {
            this.plugin.getHorseManager().setOwnerForHorse(horseIdentifier, null);
            return;
        }
        this.plugin.getHorseManager().setHorseLastSelectionPosition(horseIdentifier, event.getEntered().getLocation());
        if ((player.isOp()) || (this.plugin.getPermissionsManager().hasPermission(player, "myhorse.bypass.mount")) || (this.plugin.getPermissionsManager().hasPermission(player, "myhorse.admin"))) {
            this.plugin.getOwnerManager().setCurrentHorseIdentifierForPlayer(player.getUniqueId(), horseIdentifier);
            this.plugin.getLanguageManager().setName(horseName);

            player.sendMessage(ChatColor.AQUA + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouSelectedHorse, ChatColor.GREEN));

            return;
        }

        UUID ownerId = this.plugin.getHorseManager().getOwnerForHorse(horseIdentifier);

        if (this.plugin.getHorseManager().isHorseLocked(horseIdentifier)) {
            if (!this.plugin.getHorseManager().isHorseFriend(horseIdentifier, player.getUniqueId())) {
                this.plugin.logDebug("VehicleEnter(): Not a horse friend");

                if (!ownerId.equals(player.getUniqueId())) {
                    this.plugin.getLanguageManager().setName(player.getName());
                    player.sendMessage(ChatColor.RED + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CannotUseLockedHorse, ChatColor.DARK_RED));

                    event.setCancelled(true);

                    return;
                }
                this.plugin.getOwnerManager().setCurrentHorseIdentifierForPlayer(player.getUniqueId(), horseIdentifier);
                this.plugin.getLanguageManager().setName(horseName);
                player.sendMessage(ChatColor.AQUA + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouSelectedHorse, ChatColor.GREEN));

                return;
            }
        }

        this.plugin.getLanguageManager().setPlayerName(plugin.getServer().getOfflinePlayer(ownerId.toString()).getName());
        this.plugin.getLanguageManager().setName(horseName);
        player.sendMessage(ChatColor.AQUA + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouMountedOwnedHorse, ChatColor.GREEN));
    }

    @EventHandler
    public void onEntityTameEvent(EntityTameEvent event) {
        if (event.getEntity().getType() != EntityType.HORSE) {
            return;
        }
        if (!this.plugin.isAllowedInWorld(event.getEntity().getWorld())) {
            return;
        }
        if (this.plugin.getHorseManager().isHorseOwned(event.getEntity().getUniqueId())) {
            return;
        }
        Player player = this.plugin.getServer().getPlayer(event.getOwner().getName());

        int maxHorses = this.plugin.getMaximumHorsesForPlayer(player.getName());
        int numberOfHorses = this.plugin.getHorseManager().getHorsesForOwner(player.getUniqueId()).size();
        if ((maxHorses > 0) && (numberOfHorses >= maxHorses)) {
            this.plugin.getLanguageManager().setAmount("" + maxHorses);
            player.sendMessage(ChatColor.RED + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouCannotHaveMoreHorses, ChatColor.DARK_RED));
            return;
        }
        UUID horseIdentifier = this.plugin.getHorseManager().newHorse(event.getOwner().getName(), event.getEntity());
        String horseName;
        do {
            horseName = this.plugin.getHorseManager().getNewHorseName();
        }
        while (this.plugin.getHorseManager().ownedHorseWithName(event.getOwner().getName(), horseName).booleanValue());
        this.plugin.getHorseManager().setNameForHorse(horseIdentifier, horseName);

        Horse horse = (Horse) event.getEntity();

        horse.setCustomName(this.plugin.getHorseNameColorForPlayer(((Entity) event.getOwner()).getUniqueId()) + horseName);
        horse.setCustomNameVisible(true);

        this.plugin.getHorseManager().setLockedForHorse(horseIdentifier, true);
        this.plugin.getHorseManager().setOwnerForHorse(horseIdentifier, ((Entity) event.getOwner()).getUniqueId());
        this.plugin.getHorseManager().setHorseStatistics(horseIdentifier, horse.getStyle(), horse.getMaxHealth(), horse.getJumpStrength(), horse.getVariant());
        if (player != null) {
            this.plugin.getOwnerManager().setCurrentHorseIdentifierForPlayer(player.getUniqueId(), horseIdentifier);

            player.sendMessage(ChatColor.AQUA + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouTamedAHorse, ChatColor.GREEN));

            this.plugin.getLanguageManager().setAmount("/myhorse name <horsename>");
            player.sendMessage(ChatColor.AQUA + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UseCommandToNameYourHorse, ChatColor.AQUA));

            this.plugin.getLanguageManager().setAmount("/myhorse unlock");
            player.sendMessage(ChatColor.AQUA + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UseCommandToUnlockYourHorse, ChatColor.AQUA));

            if (plugin.economyEnabled && (player.isOp() || plugin.getPermissionsManager().hasPermission(player, "myhorse.sell"))) {
                this.plugin.getLanguageManager().setAmount("/myhorse sell <price>");
                player.sendMessage(ChatColor.AQUA + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.UseCommandToSellYourHorse, ChatColor.AQUA));
            }
        }
        this.plugin.getHorseManager().setHorseLastSelectionPosition(horseIdentifier, event.getEntity().getLocation());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().equals(Action.RIGHT_CLICK_AIR)) {
            return;
        }
        if (!this.plugin.isAllowedInWorld(event.getPlayer().getWorld())) {
            return;
        }
        this.plugin.getStableManager().handleBlockClick(event.getPlayer().getName(), event.getClickedBlock());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void OnSignChange(SignChangeEvent event) {
    }

    @EventHandler
    public void onPlayerHangingBreak(HangingBreakEvent event) {
        if (event.getEntity().getType() != EntityType.LEASH_HITCH) {
            return;
        }
        for (Entity nearbyEntity : event.getEntity().getNearbyEntities(5.0D, 3.0D, 5.0D)) {
            if (nearbyEntity.getType() == EntityType.HORSE) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Entity entity = event.getRightClicked();
        if (entity == null) {
            return;
        }
        if (event.getRightClicked().getType() == EntityType.LEASH_HITCH) {
            for (Entity nearbyEntity : event.getPlayer().getNearbyEntities(5.0D, 3.0D, 5.0D)) {
                if (nearbyEntity.getType() == EntityType.HORSE) {
                    Horse horse = (Horse) nearbyEntity;
                    if (horse.isLeashed()) {
                        if (this.plugin.getHorseManager().isHorseOwned(horse.getUniqueId())) {
                            UUID ownerId = this.plugin.getHorseManager().getOwnerForHorse(horse.getUniqueId());
                            if (!ownerId.equals(event.getPlayer().getUniqueId())) {
                                if (this.plugin.getHorseManager().isHorseFriend(horse.getUniqueId(), event.getPlayer().getUniqueId())) {
                                    return;
                                }

                                this.plugin.getLanguageManager().setName(plugin.getServer().getOfflinePlayer(ownerId.toString()).getName());
                                event.getPlayer().sendMessage(ChatColor.RED + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CannotOpenLockedHorse, ChatColor.DARK_RED));
                                event.setCancelled(true);
                            }
                        }
                    }
                }
            }
            return;
        }

        if (event.getRightClicked().getType() != EntityType.HORSE) {
            return;
        }

        if (!this.plugin.isAllowedInWorld(event.getPlayer().getWorld())) {
            return;
        }

        Player player = event.getPlayer();

        UUID horseIdentifier = event.getRightClicked().getUniqueId();

        UUID ownerId = this.plugin.getHorseManager().getOwnerForHorse(horseIdentifier);
        if (ownerId == null) {
            return;
        }

        // Buy & Sell
        this.plugin.getHorseManager().setHorseLastSelectionPosition(horseIdentifier, event.getRightClicked().getLocation());
        if (this.plugin.getEconomy() != null) {
            if (!ownerId.equals(player.getUniqueId())) {
                int price = this.plugin.getHorseManager().getHorsePrice(horseIdentifier);
                if (price > 0) {
                    String horseName = this.plugin.getHorseManager().getNameForHorse(horseIdentifier);
                    this.plugin.getLanguageManager().setPlayerName(player.getName());
                    this.plugin.getLanguageManager().setName(horseName);
                    this.plugin.getLanguageManager().setAmount(this.plugin.getEconomy().format(price));

                    if (!this.plugin.getOwnerManager().isBuying(player.getUniqueId(), horseIdentifier)) {
                        if (this.plugin.getEconomy().has(player.getName(), price)) {
                            player.sendMessage(this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.AreYouSureYouWantToBuyHorse, ChatColor.AQUA));
                            this.plugin.getOwnerManager().setBuying(player.getUniqueId(), horseIdentifier, true);
                        } else {
                            this.plugin.getLanguageManager().setAmount(this.plugin.getEconomy().format(price));
                            this.plugin.getLanguageManager().setPlayerName(plugin.getServer().getOfflinePlayer(ownerId.toString()).getName());
                            this.plugin.getLanguageManager().setName(horseName);
                            player.sendMessage(this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouDoNotHaveEnoughMoneyToBuyHorse, ChatColor.DARK_RED));
                        }
                    } else {
                        Player ownerPlayer = this.plugin.getServer().getPlayer(ownerId.toString());

                        Horse horse = (Horse) entity;
                        horse.setCustomName(this.plugin.getHorseNameColorForPlayer(player.getUniqueId()) + horseName);
                        horse.setOwner(player);

                        this.plugin.getOwnerManager().setBuying(player.getUniqueId(), horseIdentifier, false);
                        this.plugin.getHorseManager().setHorseForSale(horseIdentifier, 0);

                        this.plugin.getHorseManager().setLockedForHorse(horseIdentifier, true);
                        this.plugin.getHorseManager().setOwnerForHorse(horseIdentifier, player.getUniqueId());

                        this.plugin.getHorseManager().clearHorseFriends(horseIdentifier);

                        if (ownerPlayer != null) {
                            this.plugin.getOwnerManager().setCurrentHorseIdentifierForPlayer(ownerId, null);
                            ownerPlayer.sendMessage(this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.PlayerBoughtYourHorse, ChatColor.GREEN));
                        }

                        String ownerName = this.plugin.getServer().getOfflinePlayer(ownerId.toString()).getName();

                        this.plugin.getLanguageManager().setAmount("" + price);
                        this.plugin.getLanguageManager().setName(horseName);
                        this.plugin.getLanguageManager().setPlayerName(ownerName);
                        player.sendMessage(this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouBoughtHorse, ChatColor.GREEN));

                        this.plugin.getEconomy().withdrawPlayer(player.getName(), price);
                        this.plugin.getEconomy().depositPlayer(ownerName, price);
                    }
                    event.setCancelled(true);

                    return;
                }
            }
        }

        // Leash protection
        if (player.getItemInHand() != null) {
            if (player.getItemInHand().getType() == Material.LEASH) {
                if (this.plugin.getPermissionsManager().hasPermission(player, "myhorse.bypass.leash")) {
                    return;
                }
                if (this.plugin.getHorseManager().isHorseLocked(horseIdentifier)) {
                    if (!ownerId.equals(player.getUniqueId()) && !this.plugin.getHorseManager().isHorseFriend(horseIdentifier, player.getUniqueId())) {
                        String ownerName = this.plugin.getServer().getOfflinePlayer(ownerId.toString()).getName();
                        this.plugin.getLanguageManager().setName(ownerName);
                        player.sendMessage(ChatColor.RED + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CannotLeashLockedHorse, ChatColor.DARK_RED));

                        event.setCancelled(true);

                        return;
                    }
                }
            }
        }

        // Inventory protection
        if (player.isSneaking()) {
            if (!this.plugin.getPermissionsManager().hasPermission(player, "myhorse.bypass.inventory")) {
                if (this.plugin.getHorseManager().isHorseLocked(horseIdentifier)) {
                    if (!ownerId.equals(player.getUniqueId())) {
                        String ownerName = this.plugin.getServer().getOfflinePlayer(ownerId.toString()).getName();
                        this.plugin.getLanguageManager().setName(ownerName);
                        player.sendMessage(ChatColor.RED + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CannotOpenLockedHorse, ChatColor.DARK_RED));

                        event.setCancelled(true);

                        return;
                    }
                }
            }

            if (this.plugin.allowChestsOnAllHorses) {
                Horse horse = (Horse) entity;
                if ((player.getItemInHand() != null) && (player.getItemInHand().getType() == Material.CHEST)) {
                    if (!horse.isCarryingChest()) {
                        if (player.getItemInHand().getAmount() <= 1) {
                            horse.setCarryingChest(true);
                            player.setItemInHand(null);
                            this.plugin.getLanguageManager().setName(horse.getCustomName());
                            player.sendMessage(ChatColor.RED + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YouPutAChestOnHorse, ChatColor.GREEN));

                            event.setCancelled(true);

                            return;
                        }
                    }
                }
            }
            return;
        }

        if (this.plugin.getHorseManager().isHorseLocked(horseIdentifier)) {
            if (this.plugin.getPermissionsManager().hasPermission(player, "myhorse.bypass.mount")) {
                return;
            }

            if (this.plugin.getHorseManager().isHorseFriend(horseIdentifier, player.getUniqueId())) {
                return;
            }

            if (!ownerId.equals(player.getUniqueId())) {
                String ownerName = this.plugin.getServer().getOfflinePlayer(ownerId.toString()).getName();
                this.plugin.getLanguageManager().setName(ownerName);
                player.sendMessage(ChatColor.RED + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.CannotOpenLockedHorse, ChatColor.DARK_RED));

                event.setCancelled(true);

                return;
            }
        }
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if (event.getEntity().getType() != EntityType.HORSE) {
            return;
        }
        if (!this.plugin.isAllowedInWorld(event.getEntity().getWorld())) {
            return;
        }
        if (!this.plugin.getHorseManager().isHorseOwned(event.getEntity().getUniqueId())) {
            return;
        }
        if (this.plugin.horseDamageDisabled) {
            if (this.plugin.isDamageProtection(event.getCause())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onEntityDeathEvent(EntityDeathEvent event) {
        if (event.getEntity().getType() != EntityType.HORSE) {
            return;
        }
        if (!this.plugin.isAllowedInWorld(event.getEntity().getWorld())) {
            return;
        }
        if (!this.plugin.getHorseManager().isHorseOwned(event.getEntity().getUniqueId())) {
            return;
        }

        UUID ownerId = this.plugin.getHorseManager().getOwnerForHorse(event.getEntity().getUniqueId());
        if (ownerId == null) {
            return;
        }

        String horseName = this.plugin.getHorseManager().getNameForHorse(event.getEntity().getUniqueId());

        Player player = this.plugin.getServer().getPlayer(ownerId.toString());
        if (player != null) {
            this.plugin.getLanguageManager().setAmount(event.getEntity().getLastDamageCause().getCause().name());
            this.plugin.getLanguageManager().setName(horseName);
            player.sendMessage(ChatColor.RED + this.plugin.getLanguageManager().getLanguageString(LanguageManager.LANGUAGESTRING.YourHorseDied, ChatColor.DARK_RED));
        }
        this.plugin.getHorseManager().setOwnerForHorse(event.getEntity().getUniqueId(), null);
    }
}
