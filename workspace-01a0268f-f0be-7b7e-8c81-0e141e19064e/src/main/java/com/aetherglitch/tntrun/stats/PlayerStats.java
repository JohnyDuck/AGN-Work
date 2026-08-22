package com.aetherglitch.tntrun.stats;

public class PlayerStats {
    public String name;
    public int wins;
    public int games;
    public int loses;

    public PlayerStats(String name){
        this.name=name;
    }
    public PlayerStats(String name,int wins,int games){
        this.name=name; this.wins=wins; this.games=games; this.loses=games-wins;
    }
    public double winrate(){
        if(games==0) return 0;
        return (wins*100.0)/games;
    }
}
