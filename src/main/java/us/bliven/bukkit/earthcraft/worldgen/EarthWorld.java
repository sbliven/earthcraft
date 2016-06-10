package us.bliven.bukkit.earthcraft.worldgen;

import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.IChunkGenerator;
import us.bliven.bukkit.earthcraft.EarthcraftMod;
import us.bliven.bukkit.earthcraft.biome.CoordBiomeProvider;
import us.bliven.bukkit.earthcraft.biome.DefaultBiomeProvider;
import us.bliven.bukkit.earthcraft.gis.ElevationProjection;
import us.bliven.bukkit.earthcraft.gis.ElevationProvider;
import us.bliven.bukkit.earthcraft.gis.EquirectangularProjection;
import us.bliven.bukkit.earthcraft.gis.LinearElevationProjection;
import us.bliven.bukkit.earthcraft.gis.MapProjection;
import us.bliven.bukkit.earthcraft.gis.TestElevationProvider;

import com.vividsolutions.jts.geom.Coordinate;

public class EarthWorld extends WorldType {

	public EarthWorld(String name) {
		super(name);
	}
//	@Override
//	public boolean isCustomizable() {
//		return true;
//	}
//
//	@Override
//	public void onCustomizeButton(Minecraft mc, GuiCreateWorld guiCreateWorld) {
//		// TODO Auto-generated method stub
//		mc.displayGuiScreen( new PyramidCustomization(guiCreateWorld, guiCreateWorld.chunkProviderSettingsJson));
//	}
	
	@Override
	public IChunkGenerator getChunkGenerator(World world,
			String settingsJson) {
		EarthcraftMod plugin = null;
		MapProjection mapProjection = new EquirectangularProjection();
		ElevationProjection elevProjection = new LinearElevationProjection();
		ElevationProvider elevation = new TestElevationProvider();
		CoordBiomeProvider biome = new DefaultBiomeProvider();
		Coordinate spawn = new Coordinate(0,0);
		EarthChunkProvider chunkGen = new EarthChunkProvider(plugin, world,
				world.getSeed(), world.getWorldInfo().isMapFeaturesEnabled(),
				settingsJson, mapProjection, elevProjection , elevation , biome , spawn );
		//return super.getChunkGenerator(world, generatorOptions);
		return chunkGen;
	}
	
	@Override
	public String getTranslateName() {
		return "Earthcraft";
	}
	
	@Override
	public String getTranslatedInfo() {
		return "The real world, in minecraft";
	}
	
	
}
