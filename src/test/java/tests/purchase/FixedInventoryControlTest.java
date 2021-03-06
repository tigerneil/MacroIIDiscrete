/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package tests.purchase;

import agents.EconomicAgent;
import agents.firm.Firm;
import agents.firm.purchases.FactoryProducedPurchaseDepartment;
import agents.firm.purchases.PurchasesDepartment;
import agents.firm.purchases.inventoryControl.FixedInventoryControl;
import agents.firm.purchases.inventoryControl.Level;
import agents.firm.purchases.pricing.BidPricingStrategy;
import agents.firm.sales.exploration.BuyerSearchAlgorithm;
import agents.firm.sales.exploration.SellerSearchAlgorithm;
import financial.market.Market;
import financial.market.OrderBookMarket;
import financial.utilities.Quote;
import goods.Good;
import goods.Inventory;
import goods.UndifferentiatedGoodType;
import model.MacroII;
import model.utilities.dummies.DummySeller;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
 * @version 2012-08-08
 * @see
 */
public class FixedInventoryControlTest {

    MacroII model;
    Market market;


    @Before
    public void setup(){
        model = new MacroII(1);
        model.start();
        market = new OrderBookMarket(UndifferentiatedGoodType.GENERIC);

    }

    @Test
    public void mockInventoryRating()
    {
        Firm f = mock(Firm.class);
        //set up the firm so that it returns first 1 then 10 then 5
        when(f.hasHowMany(UndifferentiatedGoodType.GENERIC)).thenReturn(1);
        when(f.getModel()).thenReturn(model);  when(f.getRandom()).thenReturn(model.random);


        FactoryProducedPurchaseDepartment<FixedInventoryControl, BidPricingStrategy, BuyerSearchAlgorithm, SellerSearchAlgorithm>
                factoryProducedPurchaseDepartment =
                PurchasesDepartment.getPurchasesDepartment(0, f, market, FixedInventoryControl.class,
                        null, null, null);
        factoryProducedPurchaseDepartment.getInventoryControl().setHowManyTimesOverInventoryHasToBeOverTargetToBeTooMuch(1.5f);

        PurchasesDepartment dept = factoryProducedPurchaseDepartment.getDepartment();

        //assuming target inventory is 6~~

        assertEquals(dept.rateCurrentLevel(), Level.DANGER);
        when(f.hasHowMany(UndifferentiatedGoodType.GENERIC)).thenReturn(10);
        assertEquals(dept.rateCurrentLevel(), Level.TOOMUCH);
        when(f.hasHowMany(UndifferentiatedGoodType.GENERIC)).thenReturn(5);
        assertEquals(dept.rateCurrentLevel(), Level.BARELY);






    }

    @Test
    public void InventoryRating()
    {
        Firm f = new Firm(model);


        FactoryProducedPurchaseDepartment<FixedInventoryControl,BidPricingStrategy,BuyerSearchAlgorithm,SellerSearchAlgorithm>
                factoryBuiltDepartment =
                PurchasesDepartment.getPurchasesDepartment(0, f, market, FixedInventoryControl.class,
                        null, null, null);
        PurchasesDepartment dept =
                factoryBuiltDepartment
                        .getDepartment();
        factoryBuiltDepartment.getInventoryControl().setHowManyTimesOverInventoryHasToBeOverTargetToBeTooMuch(1.5f);


        //assuming target inventory is 6~~

        f.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        assertEquals(dept.rateCurrentLevel(), Level.DANGER);
        assertEquals(dept.canBuy(), true);

        for(int i=0; i <9; i++)
            f.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        assertEquals(dept.rateCurrentLevel(), Level.TOOMUCH);
        assertEquals(dept.canBuy(), false);

        for(int i=0; i <5; i++)
            f.consume(UndifferentiatedGoodType.GENERIC);
        assertEquals(dept.rateCurrentLevel(), Level.BARELY);
        assertEquals(dept.canBuy(), true);







    }


    //mock testing to count how many times buy() gets called by the inventory control reacting!
    @Test
    public void InventoryRatingShouldBuyMock() throws IllegalAccessException, NoSuchFieldException, InvocationTargetException, NoSuchMethodException {
        Firm f = new Firm(model);


        PurchasesDepartment dept = PurchasesDepartment.getEmptyPurchasesDepartment(0,f,market,model);
        dept = spy(dept);


        FixedInventoryControl control = new FixedInventoryControl(dept);

        control.setHowManyTimesOverInventoryHasToBeOverTargetToBeTooMuch(1.5f);
        //make sure it's registered as a listener
        Method method = EconomicAgent.class.getDeclaredMethod("getInventory");
        method.setAccessible(true);
        Inventory inv = (Inventory) method.invoke(f);


        assertTrue(inv.removeListener(control));
        inv.addListener(control); //re-addSalesDepartmentListener it!


        for(int i=0; i <10; i++){ //you receive 10, every time you receive one and it's less than 8 (too much value) in total you call buy() so you should have called it 5 times
            f.receive(Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC),null);
        }
        verify(dept,times(8)).buy();





    }

    @Test
    public void InventoryRatingShouldBuy() throws IllegalAccessException {

        for(int k=0; k<100; k++){ //do this test 100 times because it used to fail only every now and then
            setup();
            market.start(model);


            Firm f = new Firm(model); f.receiveMany(UndifferentiatedGoodType.MONEY,1000);
            PurchasesDepartment dept = PurchasesDepartment.getPurchasesDepartment(1000, f, market, FixedInventoryControl.class,
                    null, null, null).getDepartment();

            f.registerPurchasesDepartment(dept, UndifferentiatedGoodType.GENERIC);

            BidPricingStrategy stubStrategy = mock(BidPricingStrategy.class); //addSalesDepartmentListener this stub as a new strategy so we can fix prices as we prefer
            when(stubStrategy.maxPrice(UndifferentiatedGoodType.GENERIC)).thenReturn(80); //price everything at 80; who cares
            dept.setPricingStrategy(stubStrategy);

            // SalesDepartment salesDepartment = new SalesDepartment(seller,market,new SimpleBuyerSearch(market,seller),new SimpleSellerSearch(market,seller)); //create the sales department for the seller

            assertEquals(market.getBestBuyPrice(), -1);
            assertEquals(market.getBestSellPrice(), -1);


            //addSalesDepartmentListener 10 sellers!
            for(int i=0; i<10; i++) //create 10 buyers!!!!!!!!
            {
                DummySeller seller = new DummySeller(model,10+i*10){

                    @Override
                    public void reactToFilledAskedQuote(Quote quoteFilled, Good g, int price, EconomicAgent buyer) {
                        super.reactToFilledAskedQuote(quoteFilled, g, price, buyer);
                        market.deregisterSeller(this);
                    }
                };  //these dummies are modified so that if they do trade once, they quit the market entirely
                market.registerSeller(seller);
                Good toSell = Good.getInstanceOfUndifferentiatedGood(UndifferentiatedGoodType.GENERIC);
                seller.receive(toSell,null);
                market.submitSellQuote(seller,seller.saleQuote,toSell);
            }

            assertEquals(market.getBestBuyPrice(), -1);
            assertEquals(market.getBestSellPrice(), 10);

            assertEquals(f.hasHowMany(UndifferentiatedGoodType.GENERIC), 0);
            //now force the first buy, hopefully it'll cascade up until the firm has inventory 6
            dept.buy(); //goooooooooo
            dept.getFirm().getModel().schedule.step(dept.getFirm().getModel());
            assertEquals(dept.toString(), f.hasHowMany(UndifferentiatedGoodType.GENERIC), 8);

            //when the dust settles...
            assertEquals(80, dept.getMarket().getBestBuyPrice()); //there shouldn't be a new buy order!
            assertEquals(90, dept.getMarket().getBestSellPrice());  //all the others should have been taken
            assertEquals(80, dept.getMarket().getLastPrice());    //although the max price is 80, the buyer defaults to the order book since it's visible







        }






    }



}
