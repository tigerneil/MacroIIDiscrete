/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.utilities.logs;

import java.util.EventObject;

/**
 * An "event" that can be logged
 * Created by carrknight on 4/30/14.
 */
public class LogEvent extends EventObject
{

    private final Object[] additionalParameters;

    private final LogLevel level;

    private final String message;


    public LogEvent(Object source, LogLevel level, String message,
                    Object... additionalParameters) {
        super(source);
        this.additionalParameters = additionalParameters;
        this.level = level;
        this.message = message;
    }

    public Object[] getAdditionalParameters() {
        return additionalParameters;
    }

    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }
}
