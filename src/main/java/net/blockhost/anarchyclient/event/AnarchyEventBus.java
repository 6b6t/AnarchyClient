package net.blockhost.anarchyclient.event;

import net.lenni0451.lambdaevents.IEventFilter;
import net.lenni0451.lambdaevents.LambdaManager;
import net.lenni0451.lambdaevents.generator.ASMGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AnarchyEventBus {

    private static final Logger LOGGER = LoggerFactory.getLogger(AnarchyEventBus.class);

    private final LambdaManager manager;

    public AnarchyEventBus() {
        this.manager = LambdaManager.basic(new ASMGenerator())
                .setEventFilter(AnarchyEventBus::allowEvent)
                .setExceptionHandler((handler, event, throwable) -> LOGGER.warn(
                        "Exception in AnarchyClient event handler {} for {}",
                        handler.getOwner().getName(),
                        event.getClass().getName(),
                        throwable
                ));
    }

    public void register(final Object listener) {
        this.manager.register(listener);
    }

    public void unregister(final Object listener) {
        this.manager.unregister(listener);
    }

    public <T extends AnarchyClientEvent> T call(final T event) {
        return this.manager.call(event);
    }

    private static boolean allowEvent(final Class<?> event, final IEventFilter.CheckType checkType) {
        return AnarchyClientEvent.class.isAssignableFrom(event);
    }
}
