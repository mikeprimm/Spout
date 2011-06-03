package net.glowstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.bukkit.BlockChangeDelegate;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.TreeType;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Boat;
import org.bukkit.entity.CreatureType;
import org.bukkit.entity.Item;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.entity.PoweredMinecart;
import org.bukkit.entity.StorageMinecart;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import net.glowstone.block.GlowBlock;
import net.glowstone.io.ChunkIoService;
import net.glowstone.entity.GlowEntity;
import net.glowstone.entity.EntityManager;
import net.glowstone.entity.GlowLivingEntity;
import net.glowstone.entity.GlowPlayer;
import net.glowstone.world.WorldGenerator;
import org.bukkit.entity.Entity;

/**
 * A class which represents the in-game world.
 * @author Graham Edgecombe
 */
public class GlowWorld implements World {

	/**
	 * The chunk manager.
	 */
	private final ChunkManager chunks;

	/**
	 * The entity manager.
	 */
	private final EntityManager entities = new EntityManager();
    
    /**
     * A map between locations and cached Block objects.
     */
    private final HashMap<Location, GlowBlock> blockCache = new HashMap<Location, GlowBlock>();

	/**
	 * The spawn position.
	 */
	private Location spawnLocation = new Location(null, 0, 128, 0);
    
    /**
     * Whether PvP is allowed in this world.
     */
    private boolean pvpAllowed = true;
    
    /**
     * The current world time.
     */
    private long time = 0;

	/**
	 * Creates a new world with the specified chunk I/O service and world
	 * generator.
	 * @param service The chunk I/O service.
	 * @param generator The world generator.
	 */
	public GlowWorld(ChunkIoService service, WorldGenerator generator) {
		chunks = new ChunkManager(this, service, generator);
	}

    ////////////////////////////////////////
    // Various internal mechanisms

	/**
	 * Updates all the entities within this world.
	 */
	public void pulse() {
		for (GlowEntity entity : entities)
			entity.pulse();

		for (GlowEntity entity : entities)
			entity.reset();
        
        // We currently tick at 1/4 the speed of regular MC
        time = (time + 4) % 24000;
	}

	/**
	 * Gets the chunk manager.
	 * @return The chunk manager.
	 */
	public ChunkManager getChunkManager() {
		return chunks;
	}

	/**
	 * Gets the entity manager.
	 * @return The entity manager.
	 */
	public EntityManager getEntityManager() {
		return entities;
	}

	public Collection<GlowPlayer> getRawPlayers() {
        return entities.getAll(GlowPlayer.class);
	}

	/**
	 * Broadcasts a message to every player.
	 * @param text The message text.
	 */
	public void broadcastMessage(String text) {
		for (Player player : getPlayers())
			player.sendMessage(text);
	}

    // GlowEntity lists
	
	public List<Player> getPlayers() {
        Collection<GlowPlayer> players = entities.getAll(GlowPlayer.class);
        ArrayList<Player> result = new ArrayList<Player>();
        for (Player p : players) {
            result.add(p);
        }
        return result;
	}

    public List<Entity> getEntities() {
        Collection<GlowEntity> list = entities.getAll(GlowEntity.class);
        ArrayList<Entity> result = new ArrayList<Entity>();
        for (Entity e : list) {
            result.add(e);
        }
        return result;
    }

    public List<LivingEntity> getLivingEntities() {
        Collection<GlowLivingEntity> list = entities.getAll(GlowLivingEntity.class);
        ArrayList<LivingEntity> result = new ArrayList<LivingEntity>();
        for (LivingEntity e : list) {
            result.add(e);
        }
        return result;
    }

	// Spawn location

	public Location getSpawnLocation() {
		return spawnLocation;
	}

    public boolean setSpawnLocation(int x, int y, int z) {
        spawnLocation = new Location(this, x, y, z);
        return true;
    }
    
    // Pvp on/off

    public boolean getPVP() {
        return pvpAllowed;
    }

    public void setPVP(boolean pvp) {
        pvpAllowed = pvp;
    }

    // force-save

    public void save() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // various fixed world properties

    public Environment getEnvironment() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public long getSeed() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public String getName() {
        return "world";
    }

    public long getId() {
        return getName().hashCode();
    }

    // get block, chunk, id, highest methods with coords

    public GlowBlock getBlockAt(int x, int y, int z) {
        if (blockCache.containsKey(new Location(this, x, y, z))) {
            return blockCache.get(new Location(this, x, y, z));
        } else {
            GlowBlock block = new GlowBlock(getChunkAt(x >> 4, z >> 4), x, y, z);
            blockCache.put(new Location(this, x, y, z), block);
            return block;
        }
    }

    public int getBlockTypeIdAt(int x, int y, int z) {
        return ((GlowChunk)getChunkAt(x >> 4, z >> 4)).getType(x & 0xF, z & 0xF, y & 0x7F);
    }

    public int getHighestBlockYAt(int x, int z) {
        for (int y = GlowChunk.HEIGHT - 1; y >= 0; --y) {
            if (getBlockTypeIdAt(x, y, z) != 0) return y;
        }
        return 0;
    }

    public GlowChunk getChunkAt(int x, int z) {
        return chunks.getChunk(x, z);
    }

    // get block, chunk, id, highest with locations

    public GlowBlock getBlockAt(Location location) {
        return getBlockAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public int getBlockTypeIdAt(Location location) {
        return getBlockTypeIdAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
    }

    public int getHighestBlockYAt(Location location) {
        return getHighestBlockYAt(location.getBlockX(), location.getBlockZ());
    }

    public Chunk getChunkAt(Location location) {
        return getChunkAt(location.getBlockX(), location.getBlockZ());
    }

    public Chunk getChunkAt(Block block) {
        return getChunkAt(block.getX(), block.getZ());
    }

    // Chunk loading and unloading

    public boolean isChunkLoaded(Chunk chunk) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isChunkLoaded(int x, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Chunk[] getLoadedChunks() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void loadChunk(Chunk chunk) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void loadChunk(int x, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean loadChunk(int x, int z, boolean generate) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean unloadChunk(int x, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean unloadChunk(int x, int z, boolean save) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean unloadChunk(int x, int z, boolean save, boolean safe) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean unloadChunkRequest(int x, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean unloadChunkRequest(int x, int z, boolean safe) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean regenerateChunk(int x, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean refreshChunk(int x, int z) {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    // Map gen related things

    public boolean generateTree(Location location, TreeType type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean generateTree(Location loc, TreeType type, BlockChangeDelegate delegate) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // GlowEntity spawning

    public Item dropItem(Location location, ItemStack item) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Item dropItemNaturally(Location location, ItemStack item) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Arrow spawnArrow(Location location, Vector velocity, float speed, float spread) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Minecart spawnMinecart(Location location) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public StorageMinecart spawnStorageMinecart(Location loc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public PoweredMinecart spawnPoweredMinecart(Location loc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Boat spawnBoat(Location loc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public LivingEntity spawnCreature(Location loc, CreatureType type) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public LightningStrike strikeLightning(Location loc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public LightningStrike strikeLightningEffect(Location loc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    // Time related methods

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        if (time < 0) time = (time % 24000) + 24000;
        if (time > 24000) time %= 24000;
        this.time = time;
    }

    public long getFullTime() {
        return getTime();
    }

    public void setFullTime(long time) {
        setTime(time);
    }

    // Weather related methods

    public boolean hasStorm() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setStorm(boolean hasStorm) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getWeatherDuration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setWeatherDuration(int duration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public boolean isThundering() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setThundering(boolean thundering) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public int getThunderDuration() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void setThunderDuration(int duration) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}