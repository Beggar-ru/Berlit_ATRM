package ru.beggar.berlimatrm.network;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Runs work on the server thread.
 *
 * <p>
 * 1.7.10 has no {@code MinecraftServer.addScheduledTask}; network handlers are called from the
 * netty thread, so world/recipe changes are queued and drained at the end of a server tick.
 */
public final class ServerTaskQueue {

    private static final Queue<Runnable> TASKS = new ConcurrentLinkedQueue<>();

    private ServerTaskQueue() {}

    public static void schedule(Runnable task) {
        if (task != null) {
            TASKS.add(task);
        }
    }

    /** Drains the queue. Registered on the Forge event bus in the common proxy. */
    public static final class Handler {

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            if (event.phase != TickEvent.Phase.END) {
                return;
            }
            Runnable task;
            while ((task = TASKS.poll()) != null) {
                try {
                    task.run();
                } catch (Throwable t) {
                    ru.beggar.berlimatrm.CommonProxy.LOG.error("Queued server task failed", t);
                }
            }
        }
    }
}
