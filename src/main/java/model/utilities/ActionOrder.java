/*
 * Copyright (c) 2013 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.utilities;

/**
 * <h4>Description</h4>
 * <p/> In the synchronized model we let everybody act each day. This enum holds the ordering for various actions so that everything still makes sense
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2012-11-19
 * @see
 */
public enum ActionOrder
{

    /**
     * things are started here!
     */
    DAWN,


    /**
     * Stuff gets produced here
     */
    PRODUCTION,

    /**
     * Set up prices and what can/can't be sold; basically anything between production and trade
     */
    PREPARE_TO_TRADE,

    /**
     * Match, peddle, shop
     */
    TRADE,


    /**
     * before any action is taken after trade.
     */
    POST_TRADE_STATISTICS,


    /**
     * deal with prices (pid and similar)
     */
    ADJUST_PRICES,

    /**
     * After trading has occurred, think of the consequences
     */
    THINK,


    /**
     * Final Phase, just maintenance
     */
    CLEANUP_DATA_GATHERING,

    /**
     * possibly useless phase, there for GUI objects to step
     */
    GUI_PHASE



}
