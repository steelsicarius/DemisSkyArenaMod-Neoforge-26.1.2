package net.jrdemiurge.skyarena.scheduler;

import net.jrdemiurge.skyarena.SkyArena;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = SkyArena.MOD_ID)
public class Scheduler {
    private static final List<SchedulerTask> tasks = new ArrayList<>();

    public static void schedule(Runnable task, int delay) {
        synchronized (tasks) {
            tasks.add(new SchedulerTask(delay, task));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        synchronized (tasks) {
            Iterator<SchedulerTask> iterator = tasks.iterator();
            while (iterator.hasNext()) {
                SchedulerTask st = iterator.next();
                int newTicks = st.getTicksRemaining() - 1;
                st.setTicksRemaining(newTicks);
                if (newTicks <= 0) {
                    st.getTask().run();
                    iterator.remove();
                }
            }
        }
    }
}
