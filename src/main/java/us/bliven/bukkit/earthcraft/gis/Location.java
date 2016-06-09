package us.bliven.bukkit.earthcraft.gis;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @deprecated Use BlockPos directly instead
 * @author Spencer Bliven
 */
@Deprecated
public class Location extends BlockPos {

	public Location(World world, double x, double y, double z) {
		super(x,y,z);
	}

}
