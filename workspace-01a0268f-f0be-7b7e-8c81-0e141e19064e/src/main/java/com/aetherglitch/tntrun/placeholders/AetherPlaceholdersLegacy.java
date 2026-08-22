package com.aetherglitch.tntrun.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;

// Legacy alias чтобы старые %aetherglitch_% тоже работали
public class AetherPlaceholdersLegacy extends PlaceholderExpansion {
    private final AetherPlaceholders delegate;
    public AetherPlaceholdersLegacy(AetherPlaceholders delegate){ this.delegate=delegate; }
    @Override public String getIdentifier(){ return "aetherglitch"; }
    @Override public String getAuthor(){ return "AetherGlitch"; }
    @Override public String getVersion(){ return delegate.getVersion(); }
    @Override public boolean persist(){ return true; }
    @Override public String onPlaceholderRequest(Player p, String params){ return delegate.onPlaceholderRequest(p, params); }
}
