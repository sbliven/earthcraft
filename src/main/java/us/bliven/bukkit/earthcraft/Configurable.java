package us.bliven.bukkit.earthcraft;

import org.bukkit.configuration.ConfigurationSection;


/**
 * Interface for objects which can construct themselves from a ConfigurationSection
 *
 * All implementing classes should have a default constructor.
 * @author Spencer Bliven
 */
public interface Configurable {

	public void initFromConfig(ConfigManager config, ConfigurationSection params);

}