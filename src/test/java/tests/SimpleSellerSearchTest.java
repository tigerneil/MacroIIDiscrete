/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package tests;

import agents.EconomicAgent;
import agents.firm.Firm;
import agents.firm.sales.exploration.SimpleSellerSearch;
import financial.market.OrderBookMarket;
import goods.Good;
import goods.UndifferentiatedGoodType;
import model.MacroII;
import model.utilities.dummies.DummySeller;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
 * @author Ernesto
 * @version 2012-07-26
 * @see
 */
public class SimpleSellerSearchTest {



    SimpleSellerSearch toTest;
    OrderBookMarket market = new OrderBookMarket(UndifferentiatedGoodType.GENERIC);  //notice that the search algorithm will always ignore the quoted price, if any. That's the sales department business
    MacroII model = new MacroII(1);

    Firm f;

    @Before
    public void setUp() throws Exception {

        f =           new Firm(model);
        toTest = new SimpleSellerSearch(market,f);




    }

    @Test
    public void scenario1(){

        DummySeller seller1 = new DummySeller(model,10); market.registerSeller(seller1); seller1.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller2 = new DummySeller(model,20); market.registerSeller(seller2);  seller2.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller3 = new DummySeller(model,30); market.registerSeller(seller3); seller3.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller4 = new DummySeller(model,40); market.registerSeller(seller4); seller4.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller5 = new DummySeller(model,50); market.registerSeller(seller5); seller5.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);

        List<EconomicAgent> sample = Arrays.asList(toTest.sampleSellers());
        assertTrue(sample.size() == 5);
        assertTrue(sample.contains(seller1)); //TODO
        assertTrue(sample.contains(seller2));
        assertTrue(sample.contains(seller3));
        assertTrue(sample.contains(seller4));
        assertTrue(sample.contains(seller5));

        EconomicAgent bestSeller = toTest.getBestInSampleSeller();
        assertEquals(seller1, bestSeller);
        assertEquals(10, bestSeller.askedForASaleQuote(f, UndifferentiatedGoodType.GENERIC).getPriceQuoted());

        seller5.setSaleQuote(5);

        bestSeller = toTest.getBestInSampleSeller();
        assertEquals(seller5, bestSeller);
        assertEquals(5, bestSeller.askedForASaleQuote(f, UndifferentiatedGoodType.GENERIC).getPriceQuoted());



    }


    @Test
    public void scenario2(){

        DummySeller seller1 = new DummySeller(model,-1); market.registerSeller(seller1); seller1.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller2 = new DummySeller(model,-1); market.registerSeller(seller2);  seller2.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller3 = new DummySeller(model,100); market.registerSeller(seller3);  seller3.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller4 = new DummySeller(model,2000); market.registerSeller(seller4);  seller4.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller5 = new DummySeller(model,-1); market.registerSeller(seller5);   seller5.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller6 = new DummySeller(model,-1); market.registerSeller(seller6);   seller6.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);


        List<EconomicAgent> sample = Arrays.asList(toTest.sampleSellers());
        assertTrue(sample.size() == 5);


        EconomicAgent bestSeller = toTest.getBestInSampleSeller();
        assertTrue(bestSeller == seller3 || bestSeller == seller4);


    }

    @Test
    public void scenario3(){

        DummySeller seller1 = new DummySeller(model,100); market.registerSeller(seller1); seller1.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller2 = new DummySeller(model,-1); market.registerSeller(seller2); seller2.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);



        List<EconomicAgent> sample = Arrays.asList(toTest.sampleSellers());
        assertTrue(sample.size() == 2);


        EconomicAgent bestSeller = toTest.getBestInSampleSeller();
        assertTrue(bestSeller == seller1);


    }


    @Test
    public void scenario4(){

        DummySeller seller1 = new DummySeller(model,-1); market.registerSeller(seller1); seller1.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        DummySeller seller2 = new DummySeller(model,-1); market.registerSeller(seller2); seller2.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        market.registerSeller(f);



        List<EconomicAgent> sample = Arrays.asList(toTest.sampleSellers());
        assertEquals(sample.size(), 2);


        EconomicAgent bestSeller = toTest.getBestInSampleSeller();
        assertTrue(bestSeller == null);


    }
    
}
