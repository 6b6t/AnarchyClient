package net.blockhost.anarchyclient.event;

import net.lenni0451.lambdaevents.types.ICancellableEvent;

public abstract class CancellableAnarchyClientEvent implements AnarchyClientEvent, ICancellableEvent {

    private boolean cancelled;

    public void cancel() {
        this.cancelled = true;
    }

    @Override
    public boolean isCancelled() {
        return this.cancelled;
    }
}
