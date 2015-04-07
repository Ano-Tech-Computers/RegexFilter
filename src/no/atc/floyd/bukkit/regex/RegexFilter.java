package no.atc.floyd.bukkit.regex;


import java.io.*;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.*;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
//import org.bukkit.Server;
//import org.bukkit.event.Event.Priority;
//import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChatEvent;
//import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
//import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

//import com.nijikokun.bukkit.Permissions.Permissions;

import java.util.logging.Logger;


/**
* Regular Expression chat filter plugin for Bukkit
*
* @author FloydATC
*/
public class RegexFilter extends JavaPlugin implements Listener {
//    private final RegexFilterPlayerListener playerListener = new RegexFilterPlayerListener(this);

    
    String baseDir = "plugins/RegexFilter";
    String configFile = "settings.txt";

    public CopyOnWriteArrayList<String> rules = new CopyOnWriteArrayList<String>();
    private ConcurrentHashMap<String, Pattern> patterns = new ConcurrentHashMap<String, Pattern>(); 
	public static final Logger logger = Logger.getLogger("Minecraft.RegexFilter");

    public void onDisable() {
        // TODO: Place any custom disable code here
    	rules.clear();
    	patterns.clear();

        // NOTE: All registered events are automatically unregistered when a plugin is disabled
    	
        // EXAMPLE: Custom code, here we just output some info so we can check all is well
    	PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is disabled!" );
    }

    public void onEnable() {
        // TODO: Place any custom enable code here including the registration of any events
    	loadRules();
//    	setupPermissions();
    	
        // Register our events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(this, this);
//        pm.registerEvent(Event.Type.PLAYER_CHAT, playerListener, Priority.Lowest, this);

        // EXAMPLE: Custom code, here we just output some info so we can check all is well
        PluginDescriptionFile pdfFile = this.getDescription();
		logger.info( pdfFile.getName() + " version " + pdfFile.getVersion() + " is enabled!" );
    }


    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args ) {
    	String cmdname = cmd.getName().toLowerCase();
        Player player = null;
        if (sender instanceof Player) {
        	player = (Player)sender;
        }
        
        if (cmdname.equals("regex") && args.length > 0) {
        	if (player == null || player.isOp()) {
	        	if (args[0].equalsIgnoreCase("reload")) {
	        		if (player != null) {
	        			player.sendMessage("[Filter] Reloading rules.txt");
		        		logger.info("[Filter] rules.txt reloaded by " + player.getName());
	        		} else {
		        		logger.info("[Filter] rules.txt reloaded from server console");
	        		}
	        		rules.clear();
	        		patterns.clear();
	        		loadRules();
	        	}
        	} else {
        		logger.info("[Filter] Command access denied for " + player.getName() + " (Not an operator)");
        	}
    		return true;
        }
        
        return false;
    }
    
    private void loadRules() {
    	String fname = "plugins/RegexFilter/rules.txt";
    	File f;
    	
    	// Ensure that directory exists
    	String pname = "plugins/RegexFilter";
    	f = new File(pname);
    	if (!f.exists()) {
    		if (f.mkdir()) {
    			logger.info( "[Filter] Created directory '" + pname + "'" );
    		}
    	}
    	// Ensure that rules.txt exists
    	f = new File(fname);
    	if (!f.exists()) {
			BufferedWriter output;
			String newline = System.getProperty("line.separator");
			try {
				output = new BufferedWriter(new FileWriter(fname));
				output.write("# Each rule must have one 'match' statement and atleast one 'then' statement" + newline);
				output.write("# match <regular expression>" + newline);
				output.write("# then replace <string>|warn [<string>]|log|deny|debug" + newline);
				output.write("" + newline);
				output.write("# Example 1:" + newline);
				output.write("match f+u+c+k+" + newline);
				output.write("then replace cluck" + newline);
				output.write("then warn Watch your language please" + newline);
				output.write("then log" + newline);
				output.write("" + newline);
				output.write("# Example 2:" + newline);
				output.write("match dick" + newline);
				output.write("then replace duck" + newline);
				output.write("" + newline);
				output.write("# Emulate DotFilter" + newline);
				output.write("match ^\\.[a-z]+" + newline);
				output.write("then warn" + newline);
				output.write("then deny" + newline);
				output.write("" + newline);
				output.write("# Emulate 7Filter" + newline);
				output.write("match ^7[a-z]+" + newline);
				output.write("then warn" + newline);
				output.write("then deny" + newline);
				output.close();
    			logger.info( "[Filter] Created config file '" + fname + "'" );
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}

    	
    	try {
        	BufferedReader input =  new BufferedReader(new FileReader(fname));
    		String line = null;
    		while (( line = input.readLine()) != null) {
    			line = line.trim();
    			if (!line.matches("^#.*") && !line.matches("")) {
    				rules.add(line);
    				if (line.startsWith("match ") || line.startsWith("replace ")) {
    					String[] parts = line.split(" ", 2);
    					compilePattern(parts[1]);
    				}
    			}
    		}
    		input.close();
    	}
    	catch (FileNotFoundException e) {
    		logger.warning("[Filter] Error reading config file '" + fname + "': " + e.getLocalizedMessage());
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}

    }
   
    private void compilePattern(String re) {
    	// Do not re-compile if we already have this pattern 
    	if (patterns.get(re) == null) {
    		try {
    			Pattern pattern = Pattern.compile(re, Pattern.CASE_INSENSITIVE);
    			patterns.put(re, pattern);
    			logger.fine("[Filter] Successfully compiled regex: " + re);
    		}
    		catch (PatternSyntaxException e) {
    			logger.warning("[Filter] Failed to compile regex: " + re);
    			logger.warning("[Filter] " + e.getMessage());
    		}
    		catch (Exception e) {
    			logger.severe("[Filter] Unexpected error while compiling expression '" + re + "'");
    			e.printStackTrace();
    		}
    	}
    }
    
    public Boolean matchPattern(String msg, String re_from) {
    	Pattern pattern_from = patterns.get(re_from);
    	if (pattern_from == null) {
    		// Pattern failed to compile, ignore
			logger.info("[Filter] Ignoring invalid regex: " + re_from);
    		return false;
    	}
    	Matcher matcher = pattern_from.matcher(msg);
    	return matcher.find();
    }
    
    public String replacePattern(String msg, String re_from, String to) {
    	Pattern pattern_from = patterns.get(re_from);
    	if (pattern_from == null) {
    		// Pattern failed to compile, ignore
    		return msg;
    	}
    	Matcher matcher = pattern_from.matcher(msg);
    	return matcher.replaceAll(to);
    }
    

    //Insert Player related code here
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerChat( PlayerChatEvent event) {
        String message = event.getMessage();
        Player player = event.getPlayer();
        String pname = player.getName();
        String world = player.getLocation().getWorld().getName();

    	Boolean cancel = false;
    	Boolean kick = false;
    	String reason = "chat filter";
    	Boolean command = false;
    	Boolean matched = false;
    	String regex = "";
    	String matched_msg = "";
    	Boolean log = false;
    	String warning = "";
    	Boolean aborted = false;

    	Boolean valid;

    	// Apply rules
    	for (String line : rules) {
    		if (aborted) { break; } 
    		valid = false;
    		if (line.startsWith("match ")) {
    			regex = line.substring(6);
    			matched = matchPattern(message, regex);
    			if (matched) {
    				matched_msg = message;
    			}
    			valid = true;
    		}
    		if (matched) {
        		if (line.startsWith("ignore user ")) {
        			String users = line.substring(12);
    				valid = true;
        			for (String check : users.split(" ")) {
        				if (pname.equalsIgnoreCase(check)) {
        					matched = false;
        					break;
        				}
        			}
        		}
        		if (line.startsWith("ignore group ")) {
        			String groups = line.substring(13);
    				valid = true;
        			for (String check : groups.split(" ")) {
        				String group = groupByPlayername(pname, world);
//TODO:        				String group = plugin.Permissions.Security.getGroup(world, pname);
        				if (group.equalsIgnoreCase(check)) {
        					matched = false;
        					break;
        				}
        			}
        		}
        		if (line.startsWith("ignore permission ")) {
        			String check = line.substring(18);
    				valid = true;
        			if (player.hasPermission(check) == true ) {
        				matched = false;
        			}
        		}
        		if (line.startsWith("require user ")) {
        			String users = line.substring(13);
    				valid = true;
    				Boolean found = false;
        			for (String check : users.split(" ")) {
        				if (pname.equalsIgnoreCase(check)) {
        					found = true;
        					break;
        				}
        			}
        			matched = found;
        		}
        		if (line.startsWith("require group ")) {
        			String groups = line.substring(14);
    				valid = true;
    				Boolean found = false;
        			for (String check : groups.split(" ")) {
        				String group = groupByPlayername(pname, world);
//TODO:        				String group = plugin.Permissions.Security.getGroup(world, pname);
        				if (group.equalsIgnoreCase(check)) {
        					found = true;
        					break;
        				}
        			}
        			matched = found;
        		}
        		if (line.startsWith("require permission ")) {
        			String check = line.substring(19);
    				valid = true;
        			if (player.hasPermission(check) == false) {
        				matched = false;
        			}
        		}
				if (line.startsWith("then replace ") || line.startsWith("then rewrite ")) {
					message = replacePattern(message, regex, line.substring(13));
	    			valid = true;
				}
				if (line.matches("then replace")) {
					message = replacePattern(message, regex, "");
	    			valid = true;
				}
				if (line.startsWith("then warn ")) {
					warning = line.substring(10);
	    			valid = true;
				}
				if (line.matches("then warn")) {
					warning = event.getMessage();
	    			valid = true;
				}
				if (line.matches("then log")) {
					log = true;
	    			valid = true;
				}
				if (line.startsWith("then command ")) {
					message = line.substring(13).concat(" " + message);
					command = true;
	    			valid = true;
				}
				if (line.matches("then command")) {
					command = true;
	    			valid = true;
				}
				if (line.matches("then debug")) {
					System.out.println("[Filter] Debug match: " + regex);
					System.out.println("[Filter] Debug original: " + event.getMessage());
					System.out.println("[Filter] Debug matched: " + matched_msg);
					System.out.println("[Filter] Debug current: " + message);
					System.out.println("[Filter] Debug warning: " + (warning.equals("")?"(none)":warning));
					System.out.println("[Filter] Debug log: " + (log?"yes":"no"));
					System.out.println("[Filter] Debug deny: " + (cancel?"yes":"no"));
	    			valid = true;
				}
				if (line.startsWith("then deny")) {
					cancel = true;
	    			valid = true;
				}
				if (line.startsWith("then kick ")) {
					reason = line.substring(10);
	    			valid = true;
				}
				if (line.startsWith("then kick")) {
					kick = true;
	    			valid = true;
				}
				if (line.startsWith("then abort")) {
					aborted = true;
	    			valid = true;
				}
	    		if (valid == false) {
	    			logger.warning("[Filter] Ignored syntax error in rules.txt: " + line);    			
	    		}
    		}
    	}
    	
    	// Perform flagged actions
    	if (log) {
    		logger.info("[Filter] " +  player.getName() + "> " + event.getMessage());
    	}
    	if (!warning.matches("")) {
    		player.sendMessage("§4[Filter] " + warning);
    	}

    	if (cancel == true) {
    		event.setCancelled(true);
    	}

    	if (command == true) {
			// Convert chat message to command
			event.setCancelled(true);
			logger.info("[Filter] Helped " + player.getName() + " execute command: " + message);
			player.chat("/" + message);
		} else {
			event.setMessage(message);
		}
    	
    	if (kick) {
    		player.kickPlayer(reason);
    		logger.info("[Filter] Kicked " + player.getName() + ": " + reason);
    	}
    }

    private String groupByPlayername(String playername, String worldname) {
//    	return Permissions.Security.getGroup(worldname, playername);
    	return "default"; // TODO
    }
    
}

