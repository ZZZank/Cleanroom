package net.minecraftforge.fml.common.eventhandler.impl;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * @author ZZZank
 */
public class ExampleEvent extends Event {
    public static int CURRENT_ID = 0;

    public final int id;
    public int incrementCounts = 0;

    public ExampleEvent() {
        this.id = CURRENT_ID++;
    }

    public void increment() {
        incrementCounts++;
    }
}
