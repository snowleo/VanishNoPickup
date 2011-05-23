package com.nilla.vanishnopickup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
// import java.util.Timer;
// import java.util.TimerTask;
import java.util.logging.Logger;

import net.minecraft.server.Packet20NamedEntitySpawn;
import net.minecraft.server.Packet29DestroyEntity;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;


/**
 * Vanish for Bukkit
 * 
 * @author Nodren
 * @updated by EvilNilla 
 */
public class VanishNoPickup extends JavaPlugin
{
	public static PermissionHandler Permissions = null;
	
	public Configuration config;
	public int RANGE;
	public int TOTAL_REFRESHES;
	public int REFRESH_TIMER;

	//private Timer timer = new Timer();

	public Set<String> invisible = new HashSet<String>();
	public Set<String> nopickups = new HashSet<String>();

	private final VanishNoPickupEntityListener entityListener = new VanishNoPickupEntityListener(this);
	private final VanishNoPickupPlayerListener playerListener = new VanishNoPickupPlayerListener(this);
	protected final Logger log = Logger.getLogger("Minecraft");

	public void onDisable()
	{
		//timer.cancel();
		log.info("[" + getDescription().getName() + "] " + getDescription().getVersion() + " disabled.");
	}

	public void onEnable()
	{
		//Setup Permissions 
		setupPermissions();
		
		
		//Create new configuration file
		config = new Configuration(new File(getDataFolder() ,  "config.yml"));
		
		//Load the config if it's there
		try{
			config.load();	
		}
		catch(Exception ex){
			//Ignore the errors
		}
		
		//Load our variables from configuration
		RANGE = config.getInt("range", 512);
		TOTAL_REFRESHES = config.getInt("total_refreshes", 10);
		REFRESH_TIMER = config.getInt("refresh_timer", 2);
		
		//Save the configuration(especially if it wasn't before)
		config.save();

		PluginManager pm = getServer().getPluginManager();
		pm.registerEvent(Event.Type.PLAYER_JOIN, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.PLAYER_TELEPORT, playerListener, Priority.High, this);
		pm.registerEvent(Event.Type.PLAYER_PICKUP_ITEM, playerListener, Priority.Normal, this);
		pm.registerEvent(Event.Type.ENTITY_TARGET, entityListener, Priority.Normal, this);
		
		log.info("[" + getDescription().getName() + "] " + getDescription().getVersion() + " enabled.");

		//timer.schedule(new UpdateInvisibleTimerTask(), 10, (1000 * 60) * REFRESH_TIMER);
	}

	public void setupPermissions()
	{
		Plugin test = this.getServer().getPluginManager().getPlugin("Permissions");

		if (this.Permissions == null)
		{
			if (test != null)
			{
				this.getServer().getPluginManager().enablePlugin(test);
				this.Permissions = ((Permissions) test).getHandler();
			}
			else
			{
				log.info("[" + getDescription().getName() + "] Permissions not detected.");
			}
		}
	}

	public boolean check(CommandSender sender, String permNode)
	{
		if (sender instanceof Player)
		{
			if (Permissions == null)
			{
				if (sender.isOp()) { return true; }
				return false;
			}
			else
			{
				Player player = (Player) sender;
				return Permissions.has(player, permNode);
			}
		}
		else if (sender instanceof ConsoleCommandSender)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/* Returns up-to-date Player objects for each name given to it */
	public List<Player> PlayersFromNames(Iterable<String> names) {
		List<Player> players = new ArrayList<Player>();
		for (String name : names) {
			Player player = getServer().getPlayer(name);
			players.add(player);
		}
		return players;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args)
	{
		if ((command.getName().equalsIgnoreCase("vanish")) || (command.getName().equalsIgnoreCase("poof")))
		{
			if ((args.length == 1) && (args[0].equalsIgnoreCase("list")))
			{
				list(sender);
				return true;
			}
			vanishCommand(sender);
			return true;
		}
		else if (command.getName().equalsIgnoreCase("np") || command.getName().equalsIgnoreCase("nopickup"))
		{
			if (check(sender, "vanish.nopickup")){
				if ((args.length == 1) && (args[0].equalsIgnoreCase("list")))
				{
					nopickup_list(sender);
					return true;
				}
				toggleNoPickup((Player)sender);
				return true;
				
			}
		}
		return false;
	}

	private void list(CommandSender sender)
	{
		if (!check(sender, "vanish.vanish.list")) { return; }
		if (invisible.size() == 0)
		{
			sender.sendMessage(ChatColor.RED + "No invisible players found");
			return;
		}
		String message = "List of Invisible Players: ";
		int i = 0;
		List<Player> invisiblePlayers = PlayersFromNames(invisible);
		for (Player InvisiblePlayer : invisiblePlayers)
		{
			message += InvisiblePlayer.getDisplayName();
			i++;
			if (i != invisible.size())
			{
				message += " ";
			}
		}
		sender.sendMessage(ChatColor.RED + message + ChatColor.WHITE + " ");
	}

	private void vanishCommand(CommandSender sender)
	{
		if (!check(sender, "vanish.vanish")) { return; }
		if (sender instanceof Player)
		{
			Player player = (Player) sender;

			if (!invisible.contains(player.getName())) {
				DisablePickups(player);
				vanish(player);
			} else {
				EnablePickups(player);
				reappear(player);
			}
		} else {
			sender.sendMessage("That doesn't work from here");
		}
	}

	private void invisible(Player p1, Player p2)
	{
		invisible(p1, p2, false);
	}

	/* Makes p1 invisible to p2 */
	private void invisible(Player p1, Player p2, boolean force)
	{
		if (p1.equals(p2))
			return;

		if ((!force) && (check(p2, "vanish.dont.hide")))
			return;

		if (getDistance(p1, p2) > RANGE)
			return;

		CraftPlayer hide = (CraftPlayer) p1;
		CraftPlayer hideFrom = (CraftPlayer) p2;
		hideFrom.getHandle().netServerHandler.sendPacket(new Packet29DestroyEntity(hide.getEntityId()));
	}

	/* Makes p1 visible to p2 */
	private void uninvisible(Player p1, Player p2)
	{
		if (p1.equals(p2))
			return;

		if (getDistance(p1, p2) > RANGE)
			return;

		CraftPlayer unHide = (CraftPlayer) p1;
		CraftPlayer unHideFrom = (CraftPlayer) p2;
		unHideFrom.getHandle().netServerHandler.sendPacket(new Packet29DestroyEntity(unHide.getEntityId()));
		unHideFrom.getHandle().netServerHandler.sendPacket(new Packet20NamedEntitySpawn(unHide.getHandle()));
	}

	/* Sets a player to be invisible */
	public void vanish(Player player)
	{
		invisible.add(player.getName());
		updateInvisibleForPlayer(player);
		log.info(player.getName() + " disappeared.");
		player.sendMessage(ChatColor.RED + "Poof!");
	}

	/* Sets a player to be visible again */
	public void reappear(Player player)
	{
		if (invisible.contains(player.getName()))
		{
			invisible.remove(player.getName());
			Player[] playerList = getServer().getOnlinePlayers();
			for (Player p : playerList)
			{
				uninvisible(player, p);
			}
			log.info(player.getName() + " reappeared.");
			player.sendMessage(ChatColor.RED + "You have reappeared!");
		}
	}

	/* Makes everyone visible again */
	public void reappearAll()
	{
		log.info("Everyone is going reappear.");
		List<Player> invisiblePlayers = PlayersFromNames(invisible);
		for (Player InvisiblePlayer : invisiblePlayers)
		{
			reappear(InvisiblePlayer);
		}
		invisible.clear();
	}

	public void updateInvisibleForPlayer(Player player)
	{
		updateInvisibleForPlayer(player, false);
	}

	/* Makes it so no one can see a specific player */
	public void updateInvisibleForPlayer(Player player, boolean force)
	{
		if (player == null || !player.isOnline())
			return;

		Player[] playerList = getServer().getOnlinePlayers();
		for (Player p : playerList)
		{
			invisible(player, p, force);
		}
	}

	/* Makes it so no one can see any invisible players */
	public void updateInvisibleForAll()
	{
		List<Player> invisiblePlayers = PlayersFromNames(invisible);
		for (Player invisiblePlayer : invisiblePlayers)
		{
			updateInvisibleForPlayer(invisiblePlayer);
		}
	}

	/* Makes it so a specific player can't see any invisible people */
	public void updateInvisible(Player player)
	{
		List<Player> invisiblePlayers = PlayersFromNames(invisible);
		for (Player invisiblePlayer : invisiblePlayers)
		{
			invisible(invisiblePlayer, player);
		}
	}

	public double getDistance(Player player1, Player player2)
	{
		if (player1.getWorld() != player2.getWorld())
			return Double.POSITIVE_INFINITY;

		Location loc1 = player1.getLocation();
		Location loc2 = player2.getLocation();
		return Math.sqrt(Math.pow(loc1.getX() - loc2.getX(), 2) + Math.pow(loc1.getY() - loc2.getY(), 2) + Math.pow(loc1.getZ() - loc2.getZ(), 2));
	}

	public void updateInvisibleOnTimer()
	{
		updateInvisibleForAll();
		// for (int i=0; i < TOTAL_REFRESHES; i++)
		// {
		// 	timer.schedule(new UpdateInvisibleTimerTask(), i * 1000 + 50);
		// }
	}

	// public class UpdateInvisibleTimerTask extends TimerTask
	// {
	// 	public UpdateInvisibleTimerTask()
	// 	{
	// 	}

	// 	public void run()
	// 	{
	// 		updateInvisibleForAll();
	// 	}
	// }
	
	
	public void toggleNoPickup(Player player){
		if (nopickups.contains(player.getName())) {
			EnablePickups(player);
		} else {
			DisablePickups(player);			
		}
	}

	public void DisablePickups(Player player){
		player.sendMessage(ChatColor.RED + "Disabling Picking Up of Items");
		nopickups.add(player.getName());
	}

	public void EnablePickups(Player player){
		player.sendMessage(ChatColor.RED + "Enabling Picking Up of Items");
		nopickups.remove(player.getName());
	}

	private void nopickup_list(CommandSender sender)
	{
		if (!check(sender, "vanish.nopickup.list")) { return; }
		if (nopickups.size() == 0)
		{
			sender.sendMessage(ChatColor.RED + "No players found");
			return;
		}
		String message = "List of Players with Pickups Disabled: ";
		int i = 0;
		List<Player> nopickupsPlayers = PlayersFromNames(nopickups);
		for (Player thisPlayer : nopickupsPlayers)
		{
			message += thisPlayer.getDisplayName();
			i++;
			if (i != nopickups.size())
			{
				message += " ";
			}
		}
		sender.sendMessage(ChatColor.RED + message + ChatColor.WHITE + " ");
	}
}
