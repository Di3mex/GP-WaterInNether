package de.diemex.gp.nether.water;


import me.ryanhamshire.GriefPrevention.Claim;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Allows Usage of water/ice in claimed areas in the nether
 */
//TODO remove water if area gets unclaimed
//TODO block forming of obsidian through water in the nether
public class GpWaterInNether extends JavaPlugin implements Listener
{

    @Override
    public void onEnable ()
    {
        this.getServer().getPluginManager().registerEvents(this, this);
    }

    /**
     * return an instance of GriefPrevention or null if currently not running
     * @return GriefPrevention or null
     */
    private GriefPrevention getGriefPrevention ()
    {
        Plugin plugin = this.getServer().getPluginManager().getPlugin("GriefPrevention");

        if (plugin == null &! (plugin instanceof GriefPrevention))
            return null;

        return (GriefPrevention) plugin;
    }

    /**
     * If the player is currently in his own claim
     * @param player to check
     * @return
     */
    private boolean isPlayerInClaim (Player player)
    {
        Claim claim = getGriefPrevention().dataStore.getClaimAt(player.getLocation(), false, null);
        return (claim != null && claim.ownerName.equals(player.getName()));
    }

    /**
     * If the given location is in the nether
     * @param loc Location to check
     * @return
     */
    public boolean isInNether (Location loc)
    {
        return (loc.getWorld().getEnvironment().equals(World.Environment.NETHER));
    }

    /**
     * When an ice block is broken
     */
    @EventHandler
    public void onBlockBreak (BlockBreakEvent event)
    {
        Player player = event.getPlayer();
        //Only care about ice
        if (event.getBlock().getType().equals(Material.ICE))
        {
            if (isPlayerInClaim(player) && isInNether(player.getLocation()))
            {
                //turn it into water
                event.setCancelled(true);
                Block block = event.getBlock();
                block.setType(Material.WATER);
            }
        }
    }

    /**
     * When a waterbucket is emptied
     */
    @EventHandler
    public void onWaterEmpty (PlayerBucketEmptyEvent event)
    {
        Player player = event.getPlayer();
        if (event.getBucket().equals(Material.WATER_BUCKET))
        {
            if (isPlayerInClaim(player) && isInNether(player.getLocation()))
            {
                event.setCancelled(true);
                //Get the block next to the block where the player right clicked with a water bucket
                Block block = event.getBlockClicked().getRelative(event.getBlockFace(), 1);
                block.setType(Material.WATER);
                //empty the bucket
                player.getItemInHand().setType(Material.BUCKET);
            }
        }
    }

    /**
     * When a bucket is filled
     */
    @EventHandler
    public void onWaterFill (PlayerBucketFillEvent event)
    {
        Player player = event.getPlayer();
        /**the block where the sourceBlock is located*/
        Block block =  event.getBlockClicked().getRelative(event.getBlockFace(), 1);
        //there is flowing water and stationary water
        if (block.getType().equals(Material.WATER) || block.getType().equals(Material.STATIONARY_WATER))
        {
            if (isPlayerInClaim(player) && isInNether(player.getLocation()))
            {
                //player might have stacked empty buckets
                ItemStack itemInHand = player.getItemInHand();
                /**If the watersource got picked up**/
                boolean isBucketFull = false;

                if (itemInHand.getAmount() > 1)
                {
                    int firstEmpty = player.getInventory().firstEmpty();
                    //Check if there is a free slot to add a waterbucket
                    if (firstEmpty > 0)
                    {
                        //interestingly this won't add anything when in creative mode
                        PlayerInventory inv = player.getInventory();
                        inv.setItem(firstEmpty, new ItemStack(Material.WATER_BUCKET));
                        isBucketFull = true;
                    }
                }
                else
                {
                    player.setItemInHand(new ItemStack(Material.WATER_BUCKET));
                    isBucketFull = true;
                }
                //clear the water source
                if (isBucketFull)
                {
                    event.setCancelled(true);
                    block.setType(Material.AIR);
                }
            }
        }
    }
}