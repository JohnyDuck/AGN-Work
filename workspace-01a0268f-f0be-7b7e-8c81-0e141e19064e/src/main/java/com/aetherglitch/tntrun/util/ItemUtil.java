package com.aetherglitch.tntrun.util;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class ItemUtil {
    public static ItemStack create(Material mat, String name, List<String> lore){
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        if(m!=null){
            m.setDisplayName(name.replace('&','§'));
            if(lore!=null){
                lore = lore.stream().map(s->s.replace('&','§')).toList();
                m.setLore(lore);
            }
            // убрали Unbreakable чтобы не было текста в лоре
            it.setItemMeta(m);
        }
        return it;
    }
    public static ItemStack playerHead(Player p, String name, List<String> lore){
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta m = (SkullMeta) it.getItemMeta();
        if(m!=null){
            m.setOwningPlayer(p);
            m.setDisplayName(name.replace('&','§'));
            if(lore!=null) m.setLore(lore.stream().map(s->s.replace('&','§')).toList());
            it.setItemMeta(m);
        }
        return it;
    }
}
