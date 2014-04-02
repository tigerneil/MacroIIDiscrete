package tests;

import agents.EconomicAgent;
import agents.firm.Firm;
import agents.firm.purchases.PurchasesDepartment;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.SalesDepartmentFactory;
import agents.firm.sales.pricing.UndercuttingAskPricing;
import financial.market.ImmediateOrderHandler;
import financial.market.Market;
import financial.market.OrderBookMarket;
import goods.Good;
import goods.GoodType;
import model.MacroII;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
 * @author Ernesto
 * @version 2012-07-26
 * @see
 */
public class UndercuttingPricingTest {


    Firm producer;

    Firm other;
    UndercuttingAskPricing strategy;

    SalesDepartment dept;


    OrderBookMarket market;

    MacroII model;


    @Before
    public void setup(){

        Market.TESTING_MODE = true;

        model = new MacroII(10);
        model.setCluelessDefaultMarkup(.20f);
        producer = new Firm(model);
        other = new Firm(model);

        market = new OrderBookMarket(GoodType.GENERIC);
        market.setOrderHandler(new ImmediateOrderHandler(),model);

        dept = SalesDepartmentFactory.incompleteSalesDepartment(producer, market, null, null, agents.firm.sales.SalesDepartmentAllAtOnce.class); //useless null is useless
        strategy = new UndercuttingAskPricing(dept);


    }

    @Test
    public void imitationTest() throws Exception {
        Market.TESTING_MODE = true;


        Firm seller = new Firm(model){    //don't want to deal with it
            @Override
            public void reactToFilledAskedQuote(Good g, long price, EconomicAgent agent) {
                //ignore quotes
            }
        }; market.registerSeller(seller);
        Good good =    new Good(GoodType.GENERIC,seller,10l);
        seller.receive(good,null);
        Firm buyer = dept.getFirm(); market.registerBuyer(buyer);
        buyer.registerPurchasesDepartment(mock(PurchasesDepartment.class),GoodType.GENERIC);

        assertEquals(strategy.price(good), 12); //with no adjust, it just goes default

        buyer.earn(100);



        assertTrue(seller.has(good));
        assertTrue(!buyer.has(good));

        market.submitSellQuote(seller,20l,good);
      //  market.submitBuyQuote(buyer,10l);



        assertEquals(strategy.price(good), 12);  //with no adjust, it just goes default
        strategy.step(mock(MacroII.class));   //you should have been able to find 20
        assertEquals(strategy.price(good), 19);  //now just undercut 1% the opponent


        market.submitBuyQuote(buyer,20l);
        assertTrue(!seller.has(good));
        assertTrue(buyer.has(good));

        assertEquals(good.getLastValidPrice(), 20l);
        assertEquals(strategy.price(good), 20); //now it's forced to 20 by the cost of production

        good.setLastValidPrice(10l);
        assertEquals(strategy.price(good), 19l); //now it's the undercut.

        good.setLastValidPrice(100l);
        assertEquals(strategy.price(good), 100l); //fail to copy price and instead price them minimum


        strategy.step(mock(MacroII.class)); //now there is nobody anymore. Go back to ask 20%
        assertEquals(strategy.price(good), 120l);



        market.submitSellQuote(seller,21l,good);
        strategy.step(mock(MacroII.class));   //you should have been able to find 21 and undercut him instead!
        assertEquals(strategy.price(good), 100); //limited by the cost of production
        good.setLastValidPrice(1l);
        assertEquals(strategy.price(good), 20); //now markup should be 100% and lastValue is 20



    }


    @Test
    public void uselessgood1() throws Exception {

        Good good = new Good(GoodType.GENERIC,producer,10);

        long price = strategy.price(good);
        assertEquals(price, 12);
    }

    @Test
    public void uselessgood2() throws Exception {

        Good good = new Good(GoodType.GENERIC,producer,0);

        long price = strategy.price(good);
        assertEquals(price, 0);
    }

    @Test
    public void uselessgood3() throws Exception {

        Good good = new Good(GoodType.GENERIC,producer,10);
        producer.receive(good,null);
        other.earn(100);
        producer.deliver(good,other,50);  other.pay(50,producer,null);
        assertEquals(50, other.getCash());
        assertEquals(50, producer.getCash());




        long price = strategy.price(good);
        assertEquals(price, 60);
    }


    @Test
    public void uselessAdaptiveTest() throws Exception {

        Good good = new Good(GoodType.GENERIC,producer,10);
        producer.receive(good,null);
        other.earn(100);
        producer.deliver(good,other,50);  other.pay(50,producer,null);
        assertEquals(50, other.getCash());
        assertEquals(50, producer.getCash());




        long price = strategy.price(good);
        assertEquals(price, 60);






    }


}
