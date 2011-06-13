package com.nilla.vanishnopickup;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerTeleportEvent;


/**
 * Handle events for all Player related events
 * 
 * @author Nodren
 * @updated EvilNilla
 */
public class VanishNoPickupPlayerListener extends PlayerListener
{
	private final VanishNoPickup plugin;

	public VanishNoPickupPlayerListener(VanishNoPickup instance)
	{
		plugin = instance;
	}

	@Override
	public void onPlayerJoin(PlayerJoinEvent event)
	{
		Player player = event.getPlayer();

		if(plugin.nopickups.contains(player.getName())){
			player.sendMessage(ChatColor.RED + "You have item pickups disabled!");
		}

		if(plugin.invisible.contains(player.getName())){
			player.sendMessage(ChatColor.RED + "You are currently invisible!");
			plugin.vanish(player);
		}
                
                //Make it so random players can relog to see vanished ppl
                plugin.updateInvisible(player);
	}

	public void onPlayerTeleport(PlayerTeleportEvent event)
	{
		if (event.isCancelled())
			return;
                

		Player player = event.getPlayer();
                //No more processing if the player isn't invisible
                if (!plugin.invisible.contains(player.getName())){
                  return;
                }
                
                Location locFrom = event.getFrom();
                Location locTo = event.getTo();
                
                
                long distance = plugin.getDistanceSquared(locFrom, locTo);
                boolean bDifferentWorld = false;
                
                if(event.getFrom().getWorld().getName() !=  locTo.getWorld().getName()){
                    bDifferentWorld = true;
                }
                
                VanishTeleportInfo tpi = null;
                int iLocation = -1;
                for(int i = 0; i < plugin.teleporting.size() - 1; i++){
                    if(plugin.teleporting.get(i).name == player.getName()){
                        tpi = plugin.teleporting.get(i);
                        iLocation = i;
                        break;
                    }
                }
                
                //if we don't find their teleport info, we should add it
                if ((tpi == null) && (bDifferentWorld || (distance >= plugin.RANGE_SQUARED / 2))){           
                    //plugin.log.info("Cancelling Teleport for player and moving to top:" + player.getName());
                    //Add them to our teleporting list 
                    plugin.teleporting.add(new VanishTeleportInfo(player.getName(), locTo));
                    
                    //Don't cancel the event, just move them to the top
                   // player.sendMessage(ChatColor.RED + "TP'd to 127y because you're invisible.");
                    Location new_location = new Location(locTo.getWorld(), locTo.getX(), 127, locTo.getZ());
                    event.setTo(new_location);
                    //event.setCancelled(true);
                    
                    //Fire a scheduled task to TP the player to the original location
                    plugin.scheduler.scheduleSyncDelayedTask(plugin, new TPInvisibleTimerTask(player, locTo), 15);
                    return;
                }                        
                else {
                    
                   // plugin.log.info("Should be at final destination:" + player.getName());
                    
                    //Remove the player from the TPing list and update their invisibile info
                    if(iLocation >= 0){
                        plugin.teleporting.remove(iLocation);
                    }
                    plugin.updateInvisibleForPlayerDelayed(player);
                
                }
                    
                
                
                
                //We could cancel the event and TP the player to above the world where they're TPing
                //Then WAIT 2 seconds and TP them to exactly where they wanted.
                //2 seconds is completely arbitrary, but we want them to be invisible to the 
                //Player that they're TPing to.
                //We might need some metadata to be stored(is being moved) because we're going to end 
                //up inside of this event again if we TP the player a 2nd time.

		// Make it so this player can't see anyone invisible around them
		// Make it so no one around this player will see them if they're invisible
	
	}
	
	
	@Override	
	public void onPlayerPickupItem(PlayerPickupItemEvent event){
		
		Player player = event.getPlayer();
	
		if(plugin.nopickups.contains(player.getName())) {
			event.setCancelled(true);
		}
		
	}
        
       private class TPInvisibleTimerTask implements Runnable
{
        protected Player m_player;
        protected Location m_loc;

        public TPInvisibleTimerTask(Player player, Location location)
        {
            m_player = player;
            m_loc = location;
        }


        public void run()
        {
            World world = m_player.getWorld();
            if(world.getPlayers().contains(m_player)){
                m_player.teleport(m_loc);
                plugin.updateInvisibleForPlayer(m_player);
            }
        }
        
} 
	
}
