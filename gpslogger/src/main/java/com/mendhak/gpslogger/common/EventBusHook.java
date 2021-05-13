/*
 * Copyright (C) 2016 mendhak
 *
 * This file is part of GPSLogger for Android.
 *
 * GPSLogger for Android is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * GPSLogger for Android is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with GPSLogger for Android.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.mendhak.gpslogger.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * <h3><strong>This method is called by the EventBus</strong></h3>
 * <p>It is not directly invoked by any code.</p>
 * <p>Your IDE will think that it is unused. In Android Studio, you can choose to suppress warnings for methods marked with this annotation.</p>
 */
@Target({ElementType.METHOD})
public @interface EventBusHook {
}
