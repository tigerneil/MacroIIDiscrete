package tests.predictor;

import agents.firm.Firm;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.SalesDepartmentFactory;
import agents.firm.sales.exploration.SimpleBuyerSearch;
import agents.firm.sales.exploration.SimpleSellerSearch;
import agents.firm.sales.prediction.MarketSalesPredictor;
import agents.firm.sales.prediction.SalesPredictor;
import financial.market.ImmediateOrderHandler;
import financial.market.Market;
import financial.market.OrderBookMarket;
import financial.utilities.Quote;
import goods.Good;
import goods.GoodType;
import model.MacroII;
import model.utilities.dummies.DummyBuyer;
import model.utilities.dummies.DummySeller;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;

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
 * @version 2012-07-28
 * @see
 */
public class MarketPredictorStrategyTest {


    SalesDepartment department;
    SalesPredictor strategy;
    MacroII model;
    OrderBookMarket market;
    Firm f;

    /**
     * Create two bids and an ask
     */
    @Test
    public void scenario1() throws Exception {

        Market.TESTING_MODE = true;

        model = new MacroII(100l);
        market = new OrderBookMarket(GoodType.GENERIC);
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        strategy = new MarketSalesPredictor();
        department.setPredictorStrategy(strategy);


        DummyBuyer buyer1 = new DummyBuyer(model,100l,market); market.registerBuyer(buyer1);
        market.submitBuyQuote(buyer1,100l);
        DummyBuyer buyer2 = new DummyBuyer(model,200l,market); market.registerBuyer(buyer2);
        market.submitBuyQuote(buyer2,200l);
        DummySeller seller = new DummySeller(model, 300l); market.registerSeller(seller);
        market.submitSellQuote(seller,300l,new Good(GoodType.GENERIC,seller,300l));


        assertEquals(-1, strategy.predictSalePriceAfterIncreasingProduction(department, 200, 1)); //fails because there is no closing price in memory


    }


    //like 1, but let a trade occur first
    @Test
    public void scenario2() throws Exception {


        Market.TESTING_MODE = true;


        model = new MacroII(100l);
        market = new OrderBookMarket(GoodType.GENERIC);
        market.setOrderHandler(new ImmediateOrderHandler(),model);
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        strategy = new MarketSalesPredictor();
        department.setPredictorStrategy(strategy);




        DummyBuyer buyer1 = new DummyBuyer(model,100l,market); market.registerBuyer(buyer1);
        market.submitBuyQuote(buyer1,100l);
        DummyBuyer buyer2 = new DummyBuyer(model,200l,market); market.registerBuyer(buyer2);
        market.submitBuyQuote(buyer2,200l);
        DummySeller seller = new DummySeller(model, 300l); market.registerSeller(seller);
        market.submitSellQuote(seller,300l,new Good(GoodType.GENERIC,seller,300l));


        Good sold = new Good(GoodType.GENERIC,seller,200l);
        DummyBuyer buyer3 = new DummyBuyer(model,250,market); market.registerBuyer(buyer3);   buyer3.earn(300);
        market.submitBuyQuote(buyer3,250l);
        DummySeller seller2 = new DummySeller(model, 250); market.registerSeller(seller2);
        seller2.receive(sold,null);
        market.submitSellQuote(seller2,250l,sold);

        assertTrue(buyer3.has(sold));
        assertTrue(!seller2.has(sold));
        assertEquals(50, buyer3.getCash());
        assertEquals(250, seller2.getCash());

        assertEquals(250, strategy.predictSalePriceAfterIncreasingProduction(department, 200, 1)); //copy last closing price


    }


    //as 2, but best bid is invisible
    @Test
    public void scenario3() throws Exception {

        Market.TESTING_MODE = true;

        model = new MacroII(100l);
        market = new OrderBookMarket(GoodType.GENERIC){ //break the order book so that the best buyer is not visible anymore
            /**
             * Best bid and asks are visible.
             */
            @Override
            public boolean isBestBuyPriceVisible() {
                return false;
            }
        };
        market.setOrderHandler(new ImmediateOrderHandler(),model);
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        strategy = new MarketSalesPredictor();
        department.setPredictorStrategy(strategy);




        DummyBuyer buyer1 = new DummyBuyer(model,100l,market); market.registerBuyer(buyer1);
        market.submitBuyQuote(buyer1,100l);
        DummyBuyer buyer2 = new DummyBuyer(model,200l,market); market.registerBuyer(buyer2);
        market.submitBuyQuote(buyer2,200l);
        DummySeller seller = new DummySeller(model, 300l); market.registerSeller(seller);
        market.submitSellQuote(seller,300l,new Good(GoodType.GENERIC,seller,300l));


        Good sold = new Good(GoodType.GENERIC,seller,200l);
        DummyBuyer buyer3 = new DummyBuyer(model,250,market); market.registerBuyer(buyer3);   buyer3.earn(300);
        market.submitBuyQuote(buyer3,250l);
        DummySeller seller2 = new DummySeller(model, 250); market.registerSeller(seller2);
        seller2.receive(sold,null);
        market.submitSellQuote(seller2,250l,sold);

        assertTrue(buyer3.has(sold));
        assertTrue(!seller2.has(sold));
        assertEquals(50, buyer3.getCash());
        assertEquals(250, seller2.getCash());

        assertEquals(250, strategy.predictSalePriceAfterIncreasingProduction(department, 200, 1)); //copy last closing price
        assertEquals(250, department.predictSalePriceAfterIncreasingProduction(200, 1)); //not overriden this time!

    }


    //empty market
    @Test
    public void scenario4() throws Exception {


        model = new MacroII(100l);
        market = new OrderBookMarket(GoodType.GENERIC);
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        strategy = new MarketSalesPredictor();
        department.setPredictorStrategy(strategy);

        assertEquals(-1, strategy.predictSalePriceAfterIncreasingProduction(department, 200, 1)); //useless
        assertEquals(-1, department.predictSalePriceAfterIncreasingProduction(200, 1)); //useless

    }


    //like scenario 2 but the trade is carried out by us rather than a bystander
    @Test
    public void scenario5() throws Exception {


        Market.TESTING_MODE = true;


        model = new MacroII(100l);
        market = new OrderBookMarket(GoodType.GENERIC);
        market.setOrderHandler(new ImmediateOrderHandler(),model);
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        f.registerSaleDepartment(department,GoodType.GENERIC);



        strategy = new MarketSalesPredictor();
        department.setPredictorStrategy(strategy);





        DummyBuyer buyer1 = new DummyBuyer(model,100l,market); market.registerBuyer(buyer1);
        market.submitBuyQuote(buyer1,100l);
        DummyBuyer buyer2 = new DummyBuyer(model,200l,market); market.registerBuyer(buyer2);
        market.submitBuyQuote(buyer2,200l);
        DummySeller seller = new DummySeller(model, 300l); market.registerSeller(seller);
        market.submitSellQuote(seller,300l,new Good(GoodType.GENERIC,seller,300l));


        Good sold = new Good(GoodType.GENERIC,seller,200l);
        DummyBuyer buyer3 = new DummyBuyer(model,250,market); market.registerBuyer(buyer3);   buyer3.earn(300);
        market.submitBuyQuote(buyer3, 250l);
        //market.registerSeller(department.getFirm()); Automatically registered when you create the sales department
        department.getFirm().receive(sold,null);
        //hack to simulate sellThis without actually calling it
        Field field = SalesDepartment.class.getDeclaredField("goodsQuotedOnTheMarket");
        field.setAccessible(true);
        ((HashMap<Good,Quote>)field.get (department)).put(sold, null);

        market.submitSellQuote(department.getFirm(),250l,sold);

        assertTrue(buyer3.has(sold));
        assertTrue(!f.has(sold));
        assertEquals(50, buyer3.getCash());
        assertEquals(250, f.getCash());

        assertEquals(250, strategy.predictSalePriceAfterIncreasingProduction(department, 200, 1)); //copy the last closing price (which is our closing price anyway)

    }

}
