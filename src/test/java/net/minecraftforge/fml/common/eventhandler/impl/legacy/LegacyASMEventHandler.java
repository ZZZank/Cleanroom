/*
 * Minecraft Forge
 * Copyright (c) 2016-2020.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation version 2.1
 * of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

package net.minecraftforge.fml.common.eventhandler.impl.legacy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;

import com.google.common.collect.Maps;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraftforge.fml.common.ModContainer;

import net.minecraftforge.fml.common.eventhandler.*;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import static org.objectweb.asm.Opcodes.*;

public class LegacyASMEventHandler extends ASMEventHandler
{
    private static int IDs = 0;
    private static final String HANDLER_DESC = Type.getInternalName(IEventListener.class);
    private static final String HANDLER_FUNC_DESC = Type.getMethodDescriptor(IEventListener.class.getDeclaredMethods()[0]);
    private static final HashMap<Method, Class<?>> cache = Maps.newHashMap();

    private static final ThreadLocal<Object> LOADER_TEMP = new ThreadLocal<>();

    public LegacyASMEventHandler(
        Object target,
        Method method,
        ModContainer owner,
        boolean isGeneric,
        LaunchClassLoader loader
    ) throws Exception {
        super(cacheLoader(target, loader), method, owner, isGeneric);
    }

    private static Object cacheLoader(Object target, LaunchClassLoader loader) {
        LOADER_TEMP.set(loader);
        return target;
    }

    @Override
    protected IEventListener getEventListener(Object target, Method method, boolean isGeneric) throws Exception {
        var wrapper = createWrapper(method, (LaunchClassLoader) LOADER_TEMP.get());
        LOADER_TEMP.remove();
        var rawListener = Modifier.isStatic(method.getModifiers())
            ? (IEventListener) wrapper.newInstance()
            : (IEventListener) wrapper.getConstructor().newInstance(target);
        if (isGeneric) {
            java.lang.reflect.Type filter = null;
            if (method.getGenericParameterTypes()[0] instanceof ParameterizedType parameterized) {
                filter = parameterized.getActualTypeArguments()[0];
            }
            var finalFilter = filter;
            return event -> {
                if (finalFilter != null && finalFilter == ((IGenericEvent<?>) event).getGenericType()) {
                    rawListener.invoke(event);
                }
            };
        }
        return rawListener;
    }

    public Class<?> createWrapper(Method callback, LaunchClassLoader loader)
    {
        if (cache.containsKey(callback))
        {
            return cache.get(callback);
        }

        ClassWriter cw = new ClassWriter(0);
        MethodVisitor mv;

        boolean isStatic = Modifier.isStatic(callback.getModifiers());
        boolean isInterface = callback.getDeclaringClass().isInterface();
        String name = getUniqueName(callback);
        String desc = name.replace('.',  '/');
        String instType = Type.getInternalName(callback.getDeclaringClass());
        String eventType = Type.getInternalName(callback.getParameterTypes()[0]);
    /*
        System.out.println("Class     " + callback.getDeclaringClass().getName());
        System.out.println("Name:     " + name);
        System.out.println("Desc:     " + desc);
        System.out.println("InstType: " + instType);
        System.out.println("Callback: " + callback.getName() + Type.getMethodDescriptor(callback));
        System.out.println("Event:    " + eventType);
    */

        cw.visit(V21, ACC_PUBLIC | ACC_SUPER, desc, null, "java/lang/Object", new String[]{ HANDLER_DESC });

        cw.visitSource(".dynamic", null);
        {
            if (!isStatic)
                cw.visitField(ACC_PUBLIC, "instance", "Ljava/lang/Object;", null, null).visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "<init>", isStatic ? "()V" : "(Ljava/lang/Object;)V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            if (!isStatic)
            {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitVarInsn(ALOAD, 1);
                mv.visitFieldInsn(PUTFIELD, desc, "instance", "Ljava/lang/Object;");
            }
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        {
            mv = cw.visitMethod(ACC_PUBLIC, "invoke", HANDLER_FUNC_DESC, null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            if (!isStatic)
            {
                mv.visitFieldInsn(GETFIELD, desc, "instance", "Ljava/lang/Object;");
                mv.visitTypeInsn(CHECKCAST, instType);
            }
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, eventType);
            mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, instType, callback.getName(), Type.getMethodDescriptor(callback), isInterface);
            mv.visitInsn(RETURN);
            mv.visitMaxs(2, 2);
            mv.visitEnd();
        }
        cw.visitEnd();
        Class<?> ret = loader.defineClass(name, cw.toByteArray());
        cache.put(callback, ret);
        return ret;
    }

    private String getUniqueName(Method callback)
    {
        return String.format("%s_%d_%s_%s_%s", getClass().getName(), IDs++,
            callback.getDeclaringClass().getSimpleName(),
            callback.getName(),
            callback.getParameterTypes()[0].getSimpleName());
    }
}
