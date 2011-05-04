package com.nilla.vanishnopickup;

import org.bukkit.entity.Chicken;
import org.bukkit.entity.Cow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.Giant;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Pig;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.entity.Skeleton;
import org.bukkit.entity.Slime;
import org.bukkit.entity.Spider;
import org.bukkit.entity.Squid;
import org.bukkit.entity.Wolf;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.event.entity.EntityTargetEvent;

/**
 * Handle events for Entities
 * 
 * @author EvilNilla
 * Code borrowed from github.com/fullwall/Friendlies
 */
public class VanishNoPickupEntityListener extends EntityListener 
{
	private final VanishNoPickup plugin;

	public VanishNoPickupEntityListener(VanishNoPickup instance)
	{
		plugin = instance;
	}

	/*
	 * This code graciously ripped off of the Friendlies plugin by fullwall:
	 * github.com/fullwall/Friendlies
	 * 
	 * We disregard all his permissions checks and 
	 *    just cancel if allowed and invisible.
	 */
	@Override
	public void onEntityTarget(EntityTargetEvent e) {
		
		//Don't bother with non players
		if (!(e.getTarget() instanceof Player))
			return;
		
		Player player = (Player)e.getTarget();
		
		//Make sure this player is invisible
		if (!plugin.invisible.contains(player)){return;}
		//Check the permissions
		if (!plugin.check(player, "vanish.noaggromobs")) { return; }
		
		//Make sure it's a hostile mob
		LivingEntity le = (LivingEntity) e.getEntity();
		
		//Get the name
		String name = checkMonsters(le);
		
		//If it's not in our list, exit
		if (name.isEmpty())
			return;
		
		//only cancel if they're attacking for a good reason
		//if (e.getReason() == TargetReason.) {
		
		//We've passed all checks, cancel the event
		e.setCancelled(true);
		return;
		//}
		//return;
	}

	public String checkMonsters(LivingEntity le) {
		String name = "";
		if (le instanceof Chicken) {
			name = "chicken";
		} else if (le instanceof Cow) {
			name = "cow";
		} else if (le instanceof Creeper) {
			name = "creeper";
		} else if (le instanceof Ghast) {
			name = "ghast";
		} else if (le instanceof Giant) {
			name = "giant";
		} else if (le instanceof Pig) {
			name = "pig";
		} else if (le instanceof PigZombie) {
			name = "pigzombie";
		} else if (le instanceof Monster) {
			name = "monster";
		} else if (le instanceof Sheep) {
			name = "sheep";
		} else if (le instanceof Skeleton) {
			name = "skeleton";
		} else if (le instanceof Slime) {
			name = "slime";
		} else if (le instanceof Spider) {
			name = "spider";
		} else if (le instanceof Squid) {
			name = "squid";
		} else if (le instanceof Wolf) {
			name = "wolf";
		} else if (le instanceof Zombie) {
			name = "zombie";
		}
		return name;
	}
}
