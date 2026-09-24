package dev.blendemotes.core.client;

/** Snapshot of what the local player is doing, filled every tick by the platform code. */
public final class LocalInput {
    public boolean moving;
    public boolean jumping;
    public boolean sneaking;
    public boolean attacking;
    public boolean usingItem;
    public boolean hurt;
    /** Riding, sleeping, dead, swimming, elytra flying: emotes cannot play. */
    public boolean blocked;

    public LocalInput clear() {
        moving = false;
        jumping = false;
        sneaking = false;
        attacking = false;
        usingItem = false;
        hurt = false;
        blocked = false;
        return this;
    }
}
