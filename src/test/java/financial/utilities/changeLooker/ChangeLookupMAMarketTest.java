/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package financial.utilities.changeLooker;

import financial.market.Market;
import model.MacroII;
import model.utilities.ActionOrder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

/**
 * <h4>Description</h4>
 * <p/>
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-02-27
 * @see
 */
public class ChangeLookupMAMarketTest {

    //example where everything is valid
    @Test
    public void changeLookup()
    {

        Market market = mock(Market.class);
        MacroII macroII = mock(MacroII.class); when(macroII.getCurrentPhase()).thenReturn(ActionOrder.THINK);

        ChangeLookupMAMarket changeLookupMAMarket = new ChangeLookupMAMarket(market,5,macroII);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0f,.0001f); //invalid
        when(market.getLastPrice()).thenReturn(1);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0f,.0001f); //invalid
        when(market.getLastPrice()).thenReturn(2);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),1f,.0001f); //100%!
        when(market.getLastPrice()).thenReturn(3);



        changeLookupMAMarket.step(macroII);
        when(market.getLastPrice()).thenReturn(4);
        changeLookupMAMarket.step(macroII);
        when(market.getLastPrice()).thenReturn(5);
        changeLookupMAMarket.step(macroII);
        when(market.getLastPrice()).thenReturn(6);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.45666f,.0001f);

        when(market.getLastPrice()).thenReturn(7);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.29f,.0001f);

        when(market.getLastPrice()).thenReturn(8);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.2185714286f,.0001f);

        when(market.getLastPrice()).thenReturn(9);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.1769047619f,.0001f);

        when(market.getLastPrice()).thenReturn(10);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.1491269841f,.0001f);

        when(market.getLastPrice()).thenReturn(11);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.1291269841f,.0001f);

        when(market.getLastPrice()).thenReturn(12);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.113975469f,.0001f);

    }


    //like the one above but a string of -1 are fed in it between six and seven
    @Test
    public void changeLookupWithInvalid()
    {

        Market market = mock(Market.class);
        MacroII macroII = mock(MacroII.class); when(macroII.getCurrentPhase()).thenReturn(ActionOrder.THINK);

        ChangeLookupMAMarket changeLookupMAMarket = new ChangeLookupMAMarket(market,5,macroII);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0f,.0001f); //invalid
        when(market.getLastPrice()).thenReturn(1);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0f,.0001f); //invalid
        when(market.getLastPrice()).thenReturn(2);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),1f,.0001f); //100%!
        when(market.getLastPrice()).thenReturn(3);



        changeLookupMAMarket.step(macroII);
        when(market.getLastPrice()).thenReturn(4);
        changeLookupMAMarket.step(macroII);
        when(market.getLastPrice()).thenReturn(5);
        changeLookupMAMarket.step(macroII);
        when(market.getLastPrice()).thenReturn(6);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.45666f,.0001f);

        for(int i=0; i < 15; i++) //15 times you are fed in a -1
        {
            when(market.getLastPrice()).thenReturn(-1);
            changeLookupMAMarket.step(macroII);
        }
        //give it back 6
        when(market.getLastPrice()).thenReturn(6);
        changeLookupMAMarket.step(macroII);
        when(market.getLastPrice()).thenReturn(7);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.29f,.0001f);

        when(market.getLastPrice()).thenReturn(8);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.2185714286f,.0001f);

        when(market.getLastPrice()).thenReturn(9);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.1769047619f,.0001f);

        when(market.getLastPrice()).thenReturn(10);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.1491269841f,.0001f);

        when(market.getLastPrice()).thenReturn(11);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.1291269841f,.0001f);

        when(market.getLastPrice()).thenReturn(12);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0.113975469f,.0001f);

    }



    //price is always 0
    @Test
    public void priceIsAlwaysStuckAt0()
    {

        Market market = mock(Market.class);
        MacroII macroII = mock(MacroII.class); when(macroII.getCurrentPhase()).thenReturn(ActionOrder.THINK);

        ChangeLookupMAMarket changeLookupMAMarket = new ChangeLookupMAMarket(market,5,macroII);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0f,.0001f); //invalid
        when(market.getLastPrice()).thenReturn(0);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0f,.0001f); //invalid
        when(market.getLastPrice()).thenReturn(0);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0f,.0001f);



        changeLookupMAMarket.step(macroII);
        changeLookupMAMarket.step(macroII);
        changeLookupMAMarket.step(macroII);
        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0,.0001f);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0,.0001f);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0,.0001f);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0,.0001f);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0,.0001f);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0,.0001f);

        changeLookupMAMarket.step(macroII);
        assertEquals(changeLookupMAMarket.getChange(),0,.0001f);






    }

}
