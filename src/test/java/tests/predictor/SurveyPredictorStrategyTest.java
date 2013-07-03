package tests.predictor;

import agents.firm.Firm;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.SalesDepartmentFactory;
import agents.firm.sales.exploration.SimpleBuyerSearch;
import agents.firm.sales.exploration.SimpleSellerSearch;
import agents.firm.sales.prediction.SalesPredictor;
import agents.firm.sales.prediction.SurveySalesPredictor;
import financial.Market;
import financial.OrderBookMarket;
import goods.Good;
import goods.GoodType;
import model.MacroII;
import org.junit.Test;
import model.utilities.dummies.DummyBuyer;
import model.utilities.dummies.DummySeller;

import java.lang.reflect.Field;
import java.util.Set;

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
public class SurveyPredictorStrategyTest {

    SalesDepartment department;
    SalesPredictor strategy;
    MacroII model;
    Market market;
    Firm f;

    /**
     * Create two bids and an ask
     */
    @Test
    public void scenario1() throws Exception {


        model = new MacroII(100l);
        market = new OrderBookMarket(GoodType.GENERIC);
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        strategy = new SurveySalesPredictor();
        department.setPredictorStrategy(strategy);


        DummyBuyer buyer1 = new DummyBuyer(model,100l); market.registerBuyer(buyer1);
        market.submitBuyQuote(buyer1,100l);
        DummyBuyer buyer2 = new DummyBuyer(model,199l); market.registerBuyer(buyer2);
        market.submitBuyQuote(buyer2,200l);
        DummySeller seller = new DummySeller(model, 300l); market.registerSeller(seller);
        market.submitSellQuote(seller,300l,new Good(GoodType.GENERIC,seller,300l));


        assertEquals(199, strategy.predictSalePrice(department, 200)); //dummy buyer 2 always answers "199"


    }


    //like 1, but let a trade occur first
    @Test
    public void scenario2() throws Exception {

        Market.TESTING_MODE = true;



        model = new MacroII(100l);
        market = new OrderBookMarket(GoodType.GENERIC);
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        strategy = new SurveySalesPredictor();
        department.setPredictorStrategy(strategy);




        DummyBuyer buyer1 = new DummyBuyer(model,100l); market.registerBuyer(buyer1);
        market.submitBuyQuote(buyer1,100l);
        DummyBuyer buyer2 = new DummyBuyer(model,199l); market.registerBuyer(buyer2);
        market.submitBuyQuote(buyer2,200l);
        DummySeller seller = new DummySeller(model, 300l); market.registerSeller(seller);
        market.submitSellQuote(seller,300l,new Good(GoodType.GENERIC,seller,300l));


        Good sold = new Good(GoodType.GENERIC,seller,200l);
        DummyBuyer buyer3 = new DummyBuyer(model,250); market.registerBuyer(buyer3);   buyer3.earn(300);
        market.submitBuyQuote(buyer3,250l);
        DummySeller seller2 = new DummySeller(model, 250); market.registerSeller(seller2);
        seller2.receive(sold,null);
        market.submitSellQuote(seller2,250l,sold);

        assertTrue(buyer3.has(sold));
        assertTrue(!seller2.has(sold));
        assertEquals(50, buyer3.getCash());
        assertEquals(250, seller2.getCash());


        assertEquals(250l, strategy.predictSalePrice(department, 200)); //find buyer 3
        market.deregisterBuyer(buyer3);
        assertEquals(199l, strategy.predictSalePrice(department, 200)); //find buyer 2


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
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        strategy = new SurveySalesPredictor();
        department.setPredictorStrategy(strategy);




        DummyBuyer buyer1 = new DummyBuyer(model,100l); market.registerBuyer(buyer1);
        market.submitBuyQuote(buyer1,100l);
        DummyBuyer buyer2 = new DummyBuyer(model,199l); market.registerBuyer(buyer2);
        market.submitBuyQuote(buyer2,200l);
        DummySeller seller = new DummySeller(model, 300l); market.registerSeller(seller);
        market.submitSellQuote(seller,300l,new Good(GoodType.GENERIC,seller,300l));


        Good sold = new Good(GoodType.GENERIC,seller,200l);
        DummyBuyer buyer3 = new DummyBuyer(model,250); market.registerBuyer(buyer3);   buyer3.earn(300);
        market.submitBuyQuote(buyer3,250l);
        DummySeller seller2 = new DummySeller(model, 250); market.registerSeller(seller2);
        seller2.receive(sold,null);
        market.submitSellQuote(seller2,250l,sold);

        assertTrue(buyer3.has(sold));
        assertTrue(!seller2.has(sold));
        assertEquals(50, buyer3.getCash());
        assertEquals(250, seller2.getCash());

        assertEquals(250l, strategy.predictSalePrice(department, 200)); //find buyer 3
        market.deregisterBuyer(buyer3);
        assertEquals(199l, strategy.predictSalePrice(department, 200)); //find buyer 2        assertEquals(199l,department.predictSalePrice(200)); //not overriden this time!

    }


    //empty market
    @Test
    public void scenario4() throws Exception {
        Market.TESTING_MODE = true;


        model = new MacroII(100l);
        market = new OrderBookMarket(GoodType.GENERIC);
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        strategy = new SurveySalesPredictor();
        department.setPredictorStrategy(strategy);

        assertEquals(-1, strategy.predictSalePrice(department, 200)); //useless
        assertEquals(-1, department.predictSalePrice(200)); //useless

    }

    //like scenario 2 but the trade is carried out by us rather than a bystander
    @Test
    public void scenario5() throws Exception {

        Market.TESTING_MODE = true;



        model = new MacroII(100l);
        market = new OrderBookMarket(GoodType.GENERIC);
        f = new Firm(model);
        department = SalesDepartmentFactory.incompleteSalesDepartment(f, market, new SimpleBuyerSearch(market, f), new SimpleSellerSearch(market, f), agents.firm.sales.SalesDepartmentAllAtOnce.class);
        f.registerSaleDepartment(department,GoodType.GENERIC);



        strategy = new SurveySalesPredictor();
        department.setPredictorStrategy(strategy);





        DummyBuyer buyer1 = new DummyBuyer(model,100l); market.registerBuyer(buyer1);
        market.submitBuyQuote(buyer1,100l);
        DummyBuyer buyer2 = new DummyBuyer(model,199l); market.registerBuyer(buyer2);
        market.submitBuyQuote(buyer2,200l);
        DummySeller seller = new DummySeller(model, 300l); market.registerSeller(seller);
        market.submitSellQuote(seller,300l,new Good(GoodType.GENERIC,seller,300l));


        Good sold = new Good(GoodType.GENERIC,seller,200l);
        DummyBuyer buyer3 = new DummyBuyer(model,250); market.registerBuyer(buyer3);   buyer3.earn(300);
        market.submitBuyQuote(buyer3, 250l);
        //market.registerSeller(department.getFirm()); Automatically registered when you create the sales department
        department.getFirm().receive(sold,null);
        //hack to simulate sellThis without actually calling it
        Field field = SalesDepartment.class.getDeclaredField("toSell");
        field.setAccessible(true);
        Set<Good> toSell = (Set<Good>) field.get (department);
        toSell.add(sold);

        market.submitSellQuote(department.getFirm(),250l,sold);

        assertTrue(buyer3.has(sold));
        assertTrue(!f.has(sold));
        assertEquals(50, buyer3.getCash());
        assertEquals(250, f.getCash());

        assertEquals(250l, strategy.predictSalePrice(department, 200)); //find buyer 3
        market.deregisterBuyer(buyer3);
        assertEquals(199l, strategy.predictSalePrice(department, 200)); //find buyer 2        assertEquals(200,department.predictSalePrice(200)); //overridden by looking at the order book.

    }


}