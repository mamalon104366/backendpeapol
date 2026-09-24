//#if MC < 11900
package dev.blendemotes.modern.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The superflat preset (private before 1.19); used by the CI self-test only. */
@Mixin(WorldPreset.class)
public interface WorldPresetAccessor {
    @Accessor("FLAT")
    static WorldPreset blendemotes$flat() {
        throw new AssertionError();
    }
}
//#endif
