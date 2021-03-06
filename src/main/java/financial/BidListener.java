/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package financial;

import agents.EconomicAgent;
import financial.utilities.Quote;


/**
 * <h4>Description</h4>
 * <p/> This a simple interface denoting agents that are to be notified whenever a new bid is put or removed from the market
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author Ernesto
 * @version 2012-08-27
 * @see
 */
public interface BidListener {


    /**
     * Tell the listener a new bid has been placed into the market
     * @param buyer the agent placing the bid
     * @param price the price of the good
     * @param bestAsk the best ask when the bid was made
     */
    public void newBidEvent( final EconomicAgent buyer, final long price, final Quote bestAsk);


    /**
     * Tell the listener a new bid has been placed into the market
     * @param buyer the agent placing the bid
     * @param quote the removed quote
     */
    public void removedBidEvent( final EconomicAgent buyer,  final Quote quote);

}
