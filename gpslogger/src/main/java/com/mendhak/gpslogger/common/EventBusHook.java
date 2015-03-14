package com.mendhak.gpslogger.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * <h3><strong>This method is called by the EventBus</strong></h3>
 * <p>It is not directly invoked by any code.</p>
 * <p>Your IDE will think that it is unused. In IntelliJ, you can choose to suppress warnings for methods marked with this annotation.</p>
 */
@Target({ElementType.METHOD})
public @interface EventBusHook {
}
