package net.minecraftforge.fml.common.eventhandler.impl.legacy;

import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.eventhandler.ASMEventHandler;
import net.minecraftforge.fml.common.eventhandler.EventBus;

import java.lang.reflect.Method;

/**
 * @author ZZZank
 */
public class LegacyEventBus extends EventBus {

    private final LaunchClassLoader loader;

    public LegacyEventBus(LaunchClassLoader loader) {
        this.loader = loader;
    }

    @Override
    protected ASMEventHandler generateHandler(Object target, Method method, ModContainer owner, boolean isGeneric)
        throws Exception {
        return new LegacyASMEventHandler(target, method, owner, isGeneric, loader);
    }
}
