package net.minecraftforge.fml.common.eventhandler.impl.legacy;

import net.minecraft.launchwrapper.LaunchClassLoader;

import java.net.URL;

/**
 * @author ZZZank
 */
public class TestClassLoader extends LaunchClassLoader {

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        var cached = this.getCachedClasses().get(name);
        if (cached != null) {
            return cached;
        }
        try {
            if (this.getParent() != null) {
                return this.getParent().loadClass(name);
            }
        } catch (Exception ignored) {
        }
        return super.findClass(name);
    }

    public TestClassLoader(URL[] sources) {
        super(sources);
    }

    public TestClassLoader(ClassLoader loader) {
        super(loader);
    }
}
