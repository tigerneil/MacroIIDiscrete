/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.utilities.logs;

import org.junit.Test;
import org.slf4j.Logger;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class LogNodeTest {


    //log node A listens to B which listens to C which listens to D. LogNode A,B and D have attached loggers.
    // New event happens in C, do both logger A and B receive it?


    @Test
    public void simpleTree() throws Exception {
        LogNode a = new LogNode();
        LogNode b = new LogNode();
        LogNode c = new LogNode();
        LogNode d = new LogNode();
        //create the tree
        a.listenTo(b);
        b.listenTo(c);
        c.listenTo(d);
        //create the loggers
        Logger loggerA = mock(Logger.class);
        a.attachOutput(loggerA);
        Logger loggerB = mock(Logger.class);
        b.attachOutput(loggerB);
        Logger loggerD = mock(Logger.class);
        d.attachOutput(loggerD);

        //create the event
        final Object[] additionalParameters = {"A", "B"};
        final String message = "scemochilegge";
        LogEvent event = new LogEvent(new Object(), LogLevel.INFO, message, additionalParameters);
        //give it to node c
        c.handleNewEvent(event);

        //should have been outputted to loggerA and loggerB but not logger D
        verify(loggerA).info(message,additionalParameters);
        verify(loggerB).info(message,additionalParameters);
        verify(loggerD,never()).info(message,additionalParameters);


        //now turn off B, percolate the same event in D. loggerD will be the only one logging
        b.turnOff();
        d.handleNewEvent(event);

        verify(loggerA).info(message,additionalParameters); //not times(2), it's still counting the old one
        verify(loggerB).info(message,additionalParameters);//not times(2), it's still counting the old one
        verify(loggerD).info(message,additionalParameters);
    }
}