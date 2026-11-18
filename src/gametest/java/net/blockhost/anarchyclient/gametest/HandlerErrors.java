package net.blockhost.anarchyclient.gametest;

import net.blockhost.anarchyclient.event.AnarchyEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Collects exceptions that module event handlers threw. The event bus only logs them, so a test
 * would pass while a module crashed on every tick without this.
 */
final class HandlerErrors {

    private static final Queue<String> ERRORS = new ConcurrentLinkedQueue<>();
    private static boolean installed;

    private HandlerErrors() {
    }

    static synchronized void install() {
        if (installed) {
            return;
        }
        Logger logger = (Logger) LogManager.getLogger(AnarchyEventBus.class);
        AbstractAppender appender = new AbstractAppender("AnarchyClientHandlerErrors", null, null, true,
                Property.EMPTY_ARRAY) {
            @Override
            public void append(final LogEvent event) {
                Throwable thrown = event.getThrown();
                ERRORS.add(event.getMessage().getFormattedMessage()
                        + (thrown == null ? "" : ": " + thrown + stackHead(thrown)));
            }
        };
        appender.start();
        logger.addAppender(appender);
        installed = true;
    }

    static List<String> drain() {
        List<String> errors = new ArrayList<>();
        for (String error = ERRORS.poll(); error != null; error = ERRORS.poll()) {
            errors.add(error);
        }
        return errors;
    }

    private static String stackHead(final Throwable thrown) {
        StackTraceElement[] stack = thrown.getStackTrace();
        return stack.length == 0 ? "" : " at " + stack[0];
    }
}
