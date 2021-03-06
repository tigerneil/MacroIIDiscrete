/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package model.utilities.logs;

import model.MacroII;
import org.slf4j.Logger;

/**
 * Appends date and phase to each log line. Done poorly. Very slow.
 * Created by carrknight on 5/2/14.
 */
public class LoggerOutputWithTimeStamp implements LogListener {

    /**
     * this is where we put out the output
     */
    private final Logger output;

    /**
     * this is the model we use
     */
    private final MacroII model;


    public LoggerOutputWithTimeStamp(Logger output, MacroII model) {
        this.output = output;
        this.model = model;
    }

    @Override
    public void handleNewEvent(LogEvent logEvent) {

       String newMessage = "date:" + model.getMainScheduleTime() + ", phase: " + model.getCurrentPhase() + logEvent.getMessage();
        LogLevel.log(output,logEvent.getLevel(),newMessage,logEvent.getAdditionalParameters());

    }
}
