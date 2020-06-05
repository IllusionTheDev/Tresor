package de.minebench.tresor;

/*
 * Tresor - Abstraction library for Bukkit plugins
 * Copyright (C) 2020 Max Lee aka Phoenix616 (mail@moep.tv)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Tresor.  If not, see <http://www.gnu.org/licenses/>.
 */

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import de.minebench.tresor.economy.TresorEconomy;
import de.themoep.hook.bukkit.HookManager;
import de.themoep.hook.core.Hook;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Field;
import java.util.logging.Level;

public class Tresor extends JavaPlugin {
    
    private WrappedServicesManager servicesManager;

    private Multimap<Class<?>, ProviderManager> hookManager = MultimapBuilder.hashKeys().arrayListValues().build();
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        servicesManager = new WrappedServicesManager(this, getServer().getServicesManager());
        try {
            injectServicesManager();
        } catch (IllegalAccessException | NoSuchFieldException | SecurityException e) {
            getLogger().log(Level.SEVERE, "Error while trying to inject WrappedServicesManager! Service mapping will not work!" + e.getMessage());
        }
        
        loadProviders();
        
        new org.bstats.MetricsLite(this);
    }
    
    private void injectServicesManager() throws IllegalAccessException, NoSuchFieldException, SecurityException {
        Field field = getServer().getClass().getDeclaredField("servicesManager");
        field.setAccessible(true);
        field.set(getServer(), servicesManager);
    }
    
    private void loadProviders() {
        new ProviderManager(Economy.class);
        new ProviderManager(TresorEconomy.class);
    }
    
    /**
     * Get the TresorServicesManager
     * @return The TresorServicesManager instance
     */
    public TresorServicesManager getServicesManager() {
        return servicesManager;
    }
    
    /**
     * Get the name of the service provider for a plugin
     * @param service   The service to get the provider name for
     * @param plugin    The plugin to get the name for
     * @return The service name or null if there was none configured
     */
    public String getProviderName(Class<?> service, JavaPlugin plugin) {
        return getConfig().getString("providers." + service.getSimpleName().toLowerCase() + "." + plugin.getName(),
                getConfig().getString("providers." + service.getSimpleName().toLowerCase() + ".default", null));
    }

    private class ProviderManager extends HookManager {

        public ProviderManager(Class<?> provider) {
            super(Tresor.this, "de.minebench.tresor.providers", true);
            setSuffix(provider.getSimpleName());
        }
    }
}
