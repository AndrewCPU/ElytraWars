package net.Andrewcpu.Minigame.Elytra;


/**
 * Created by stein on 4/19/2016.
 */
public enum ElytraClass {
    WARRIOR("Warrior"),TANK("Tank"),GHOST("Ghost"),HEALER("Healer");
    private final String text;
    private ElytraClass(final String text){
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}
