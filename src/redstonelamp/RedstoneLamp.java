package redstonelamp;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import raknet.ProtocolSession;
import redstonelamp.logger.Logger;
import redstonelamp.utils.RedstoneLampProperties;
import redstonelamp.utils.StringCast;

public class RedstoneLamp implements Runnable {
	public static String MC_VERSION = "0.11.0 build 9";
	public static String SOFTWARE = "RedstoneLamp";
	public static String VERSION = "1.1.1";
	public static String CODENAME = "Pumpkin Seeds";
	public static String STAGE = "DEVELOPMENT";
	public static double API_VERSION = 1.3;
	public static String LICENSE = "GNU GENERAL PUBLIC LICENSE";
	
	private static RedstoneLamp redstone;
	public static Logger logger = new Logger();
	private static RedstoneLampProperties rlp = new RedstoneLampProperties();
	public static Server server;
	private boolean running;
	public boolean stopped;
	public ArrayList<Player> players;
	public HashMap<Integer, Long> entityIDList = new HashMap<Integer, Long>();
	public int connectedPlayers;
	
	public static boolean DEBUG;
	public static boolean DEVELOPER;
	
	public RedstoneLamp() {
		running = true;
		stopped = false;
		connectedPlayers = 0;
		players = new ArrayList<Player>();
	}
	
	public static void main(String[] args) {
		rlp.load();
		DEBUG = StringCast.toBoolean(rlp.get("DEBUG_MODE"));
		DEVELOPER = StringCast.toBoolean(rlp.get("DEVELOPER_MODE"));
		redstone = new RedstoneLamp();
		redstone.run();
	}
	
	public void initiateShutdown() {
		running = false;
		try {
			server.getTicker().Stop();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.exit(0);
	}
	
	public void run() {
		if(start()) {
			long ticks = 0;
			while(!running) {
				try {
					Thread.sleep(100);
				} catch(InterruptedException e) {}
				ticks++;
				if(ticks % 100 == 0) {
					Runtime.getRuntime().gc();
				}
			}
		}
	}
	
	private boolean start() {
		try {
			server = new Server(redstone, rlp.get("name"), rlp.get("motd"), rlp.get("server-port"), rlp.get("whitelist"), rlp.get("announce-player-achievements"), rlp.get("spawn-protection"), rlp.get("max-players"), rlp.get("allow-cheats"), rlp.get("spawn-animals"), rlp.get("spawn-mobs"), rlp.get("gamemode"), rlp.get("force-gamemode"), rlp.get("hardcore"), rlp.get("pvp"), rlp.get("difficulty"), rlp.get("generator-settings"), rlp.get("level-name"), rlp.get("level-seed"), rlp.get("levet-type"), rlp.get("enable-query"), rlp.get("enable-rcon"), rlp.get("rcon.password"), rlp.get("auto-save"), rlp.get("enable-plugins"));
			server.start();
			if(server.pluginsEnabled())
				server.getLogger().info("Done! For help, type \"help\" or \"?\"");
			else
				server.getLogger().info("Done!");
		} catch(SocketException se) {
			String address = "0.0.0.0";
			try {
				InetAddress ip = InetAddress.getLocalHost();
				address = ip.getHostAddress();
			} catch(UnknownHostException uhe) {
				logger.fatal("Unable to determine system IP!");
			}
			logger.fatal("***** COULDN'T BIND TO PORT " + address + ":" + StringCast.toInt(rlp.get("server-port")) + " *****\n\t\t Is there a server already running on that port?");
		}
		return true;
	}
	
	public Player addPlayer(InetAddress i, int p, long cid, ProtocolSession session) {
		if(currentPlayer(i, p) == null) {
			boolean b = false;
			int entityID = 1009;
			while(!b) {
				entityID = 1000 + (int) (Math.random() * 1050);
				if(!entityIDList.containsKey(entityID)) {
					entityIDList.put(entityID, cid);
					b = true;
				}
			}
			Player player = new Player(i, p, entityID, cid, session);
			players.add(player);
			connectedPlayers++;
			logger.info("Connected players: " + players.size());
			return player;
		}
		return null;
	}
	
	public void removePlayer(InetAddress i, int p) {
		for(int j = 0; j < players.size(); j++) {
			Player player = players.get(j);
			if(player.clientAddress.equals(i) && player.clientPort == p) {
				entityIDList.values().remove(player.clientID);
				players.remove(j);
				connectedPlayers--;
				break;
			}
		}
		System.out.println("Connected players: " + players.size() + " " + entityIDList.size());
	}
	
	public Player currentPlayer(InetAddress i, int p) {
		for(Player player : players) {
			if(player.clientAddress.equals(i) && player.clientPort == p) { return player; }
		}
		return null;
	}
	
	public Integer addEntityID(Long cid) {
		boolean b = false;
		int newID = 0;
		while(!b) {
			newID = 1000 + (int) (Math.random() * 1050);
			if(!entityIDList.containsKey(newID)) {
				entityIDList.put(newID, cid);
				b = true;
			}
		}
		return newID;
	}
}
