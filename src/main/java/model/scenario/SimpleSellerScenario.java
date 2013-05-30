/*
 * Copyright (c) 2013 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.scenario;

import agents.EconomicAgent;
import agents.firm.Firm;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.SalesDepartmentAllAtOnce;
import agents.firm.sales.SalesDepartmentFactory;
import agents.firm.sales.SalesDepartmentOneAtATime;
import agents.firm.sales.exploration.SimpleBuyerSearch;
import agents.firm.sales.exploration.SimpleSellerSearch;
import agents.firm.sales.pricing.AskPricingStrategy;
import agents.firm.sales.pricing.pid.SimpleFlowSellerPID;
import au.com.bytecode.opencsv.CSVWriter;
import financial.OrderBookMarket;
import financial.utilities.ShopSetPricePolicy;
import goods.Good;
import goods.GoodType;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.DailyStatCollector;
import model.utilities.dummies.DummyBuyer;
import model.utilities.dummies.DummyBuyerWithDelay;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.io.FileWriter;
import java.io.IOException;

import static model.experiments.tuningRuns.MarginalMaximizerWithUnitPIDTuningMultiThreaded.printProgressBar;

/**
 * <h4>Description</h4>
 * <p/> This is actually a copy of scenario1 for the test class SimpleFlowSellerPIDTest.
 * Very simple matching of supply and demand
 * <p/> Basically we periodically
 * create new buyers (after removing the old ones) to give the impression of a constant demand and let the
 * flow seller get to the right price
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author Ernesto
 * @version 2012-09-10
 * @see
 */
public class SimpleSellerScenario extends Scenario {




    private boolean demandShifts = false;

    private int buyerDelay = 0;

    /**
     * the strategy used by the sales department to tinker with prices
     */
    private Class<? extends AskPricingStrategy> sellerStrategy = SimpleFlowSellerPID.class;

    /**
     * The kind of sales department to use
     */
    protected Class<? extends SalesDepartment> salesDepartmentType = SalesDepartmentAllAtOnce.class;

    /**
     * number of sellers in the model
     */
    protected int numberOfSellers = 1;

    /**
     * How many goods the seller receives every day
     */
    private int inflowPerSeller = 4;

    /**
     * If there is a demand shift, when will it appear?
     */
    public int howManyDaysLaterShockWillOccur = 2000;

    /**
     * the strategy used by the seller, after it has been instantiated
     */
    protected AskPricingStrategy strategy;


    /**
     * Called by MacroII, it creates agents and then schedules them.
     */
    @Override
    public void start() {

        //create and record a new market!
        final OrderBookMarket market= new OrderBookMarket(GoodType.GENERIC);
        market.setPricePolicy(new ShopSetPricePolicy()); //make the seller price matter

        getMarkets().put(GoodType.GENERIC,market);


        //create 10 buyers
        for(int i=0;i<10;i++){
            buildBuyer(market, i*10);


        }

        //only one seller
        for(int i=0; i < numberOfSellers; i++)
        {
            final Firm seller = buildSeller(market);
            //arrange for goods to drop periodically in the firm
            setupProduction(seller);
        }
        //if demands shifts, add 10 more buyers after adjust 2000
        if(demandShifts)
        {
            //create 10 buyers
            for(int i=0;i<10;i++){
                final DummyBuyer buyer = createAdditionalBuyer(market, i);


                getAgents().add(buyer);



            }
        }


        //hopefully that's that!






    }

    private DummyBuyer createAdditionalBuyer(final OrderBookMarket market, final int i) {
        /**
         * For this scenario we use a different kind of dummy buyer that, after "period" passed, puts a new order in the market
         */
        final DummyBuyer buyer = new DummyBuyer(getModel(),(i+10)*10){
            @Override
            public void reactToFilledBidQuote(Good g, long price, final EconomicAgent b) {
                //trick to get the steppable to recognize the anynimous me!
                final DummyBuyer reference = this;
                //schedule a new quote in period!
                this.getModel().scheduleTomorrow(ActionOrder.TRADE, new Steppable() {
                    @Override
                    public void step(SimState simState) {
                        earn(1000l);
                        //put another quote

                        market.submitBuyQuote(reference, getFixedPrice());


                    }
                });

            }
        };


        //make it adjust once to register and submit the first quote

        getModel().scheduleAnotherDay(ActionOrder.TRADE,new Steppable() {
            @Override
            public void step(SimState simState) {
                market.registerBuyer(buyer);
                buyer.earn(1000l);
                //make the buyer submit a quote soon.
                market.submitBuyQuote(buyer,buyer.getFixedPrice());

            }
        }, howManyDaysLaterShockWillOccur);
        return buyer;
    }

    private void setupProduction(final Firm seller) {
        getModel().scheduleSoon(ActionOrder.PRODUCTION,new Steppable() {
            @Override
            public void step(SimState simState) {
                //sell 4 goods!
                for(int i=0; i<inflowPerSeller; i++){
                    Good good = new Good(GoodType.GENERIC,seller,10l);
                    seller.receive(good,null);
                    seller.reactToPlantProduction(good);
                }
                //every day
                getModel().scheduleTomorrow(ActionOrder.PRODUCTION,this);
            }
        });
    }

    protected Firm buildSeller(final OrderBookMarket market) {
        final Firm seller = new Firm(getModel());
        getAgents().add(seller);



        SalesDepartment dept = SalesDepartmentFactory.incompleteSalesDepartment(seller, market, new SimpleBuyerSearch(market, seller),
                new SimpleSellerSearch(market, seller), salesDepartmentType);
        seller.registerSaleDepartment(dept, GoodType.GENERIC);
        strategy = AskPricingStrategy.Factory.newAskPricingStrategy(sellerStrategy,dept);
        //strategy.setSpeed(sellerDelay);
        dept.setAskPricingStrategy(strategy); //set strategy to PID

        return seller;
    }

    protected void buildBuyer(final OrderBookMarket market, final long price) {
        /**
         * For this scenario we use a different kind of dummy buyer that, after "period" passed, puts a new order in the market
         */
        final DummyBuyer buyer;
        if(buyerDelay ==0)
        {

            buyer = new DummyBuyer(getModel(),price){
                @Override
                public void reactToFilledBidQuote(Good g, long price, final EconomicAgent b) {
                    //trick to get the steppable to recognize the anynimous me!
                    final DummyBuyer reference = this;
                    //schedule a new quote in period!
                    this.getModel().scheduleTomorrow(ActionOrder.TRADE,new Steppable() {
                        @Override
                        public void step(SimState simState) {
                            earn(1000l);
                            //put another quote

                            if(reference.getFixedPrice() >=0)
                                market.submitBuyQuote(reference,getFixedPrice());



                        }
                    });

                }
            };
            //make it adjust once to register and submit the first quote

            getModel().scheduleSoon(ActionOrder.TRADE, new Steppable() {
                @Override
                public void step(SimState simState) {
                    market.registerBuyer(buyer);
                    buyer.earn(1000l);
                    //make the buyer submit a quote soon.
                    market.submitBuyQuote(buyer, buyer.getFixedPrice());

                }
            });

        }
        else
        {
            buyer = new DummyBuyerWithDelay(getModel(),price,buyerDelay,market);

            //make it adjust once to register and submit the first quote
            market.registerBuyer(buyer);

            getModel().scheduleSoon(ActionOrder.TRADE,new Steppable() {
                @Override
                public void step(SimState simState) {
                    buyer.earn(1000l);
                    //make the buyer submit a quote soon.
                    if(buyer.getFixedPrice() >=0 && !market.containsQuotesFromThisBuyer(buyer))
                    {
                        market.submitBuyQuote(buyer,100);
                    }

                    getModel().scheduleTomorrow(ActionOrder.TRADE,this);



                }
            }  );
        }


        getAgents().add(buyer);
    }

    /**
     * Creates the scenario object, so that it links to the model.
     * =
     */
    public SimpleSellerScenario(MacroII model) {
        super(model);
    }


    public boolean isDemandShifts() {
        return demandShifts;
    }

    public void setDemandShifts(boolean demandShifts) {
        this.demandShifts = demandShifts;
    }

    /**
     * Gets the strategy used by the sales department to tinker with prices.
     *
     * @return Value of the strategy used by the sales department to tinker with prices.
     */
    public Class<? extends AskPricingStrategy> getSellerStrategy() {
        return sellerStrategy;
    }

    /**
     * Sets new the strategy used by the sales department to tinker with prices.
     *
     * @param sellerStrategy New value of the strategy used by the sales department to tinker with prices.
     */
    public void setSellerStrategy(Class<? extends AskPricingStrategy> sellerStrategy) {
        this.sellerStrategy = sellerStrategy;
    }


    /**
     * Gets The kind of sales department to use.
     *
     * @return Value of The kind of sales department to use.
     */
    public Class<? extends SalesDepartment> getSalesDepartmentType() {
        return salesDepartmentType;
    }

    /**
     * Sets new The kind of sales department to use.
     *
     * @param salesDepartmentType New value of The kind of sales department to use.
     */
    public void setSalesDepartmentType(Class<? extends SalesDepartment> salesDepartmentType) {
        this.salesDepartmentType = salesDepartmentType;
    }

    public int getBuyerDelay() {
        return buyerDelay;
    }

    public void setBuyerDelay(int buyerDelay) {
        this.buyerDelay = buyerDelay;
    }


    /**
     * Gets How many goods the seller receives every day.
     *
     * @return Value of How many goods the seller receives every day.
     */
    public int getInflowPerSeller() {
        return inflowPerSeller;
    }

    /**
     * Sets new number of sellers in the model.
     *
     * @param numberOfSellers New value of number of sellers in the model.
     */
    public void setNumberOfSellers(int numberOfSellers) {
        this.numberOfSellers = numberOfSellers;
    }

    /**
     * Gets number of sellers in the model.
     *
     * @return Value of number of sellers in the model.
     */
    public int getNumberOfSellers() {
        return numberOfSellers;
    }

    /**
     * Sets new How many goods the seller receives every day.
     *
     * @param inflowPerSeller New value of How many goods the seller receives every day.
     */
    public void setInflowPerSeller(int inflowPerSeller) {
        this.inflowPerSeller = inflowPerSeller;
    }





    /**
     * Runs the simple seller scenario with no GUI and writes a big CSV file  (to draw graphs for the paper)
     * @param args
     */
    public static void main(String[] args)
    {

        //do it 10 times
        for(int i=0; i<10;i++)
        {

            //set up
            final MacroII macroII = new MacroII(System.currentTimeMillis());
            macroII.getRandom().setSeed(i);
            SimpleSellerScenario scenario1 = new SimpleSellerScenario(macroII);
            scenario1.setSalesDepartmentType(SalesDepartmentOneAtATime.class);
            scenario1.setSellerStrategy(SimpleFlowSellerPID.class);
            scenario1.setDemandShifts(true);
            scenario1.setHowManyDaysLaterShockWillOccur(500);


            //assign scenario
            macroII.setScenario(scenario1);

            macroII.start();



            //CSV writer set up
            try {
                CSVWriter writer = new CSVWriter(new FileWriter("runs/simpleSeller/"+"simpleSellerShock"+i +".csv"));
                DailyStatCollector collector = new DailyStatCollector(macroII,writer);
                collector.start();

            } catch (IOException e) {
                System.err.println("failed to create the file!");
            }


            //run!
            while(macroII.schedule.getTime()<1000)
            {
                macroII.schedule.step(macroII);
                printProgressBar(1001,(int)macroII.schedule.getSteps(),100);
            }


        }

    }


    /**
     * If there is a demand shift, when will it appear?.
     *
     * @return Value of If there is a demand shift, when will it appear?.
     */
    public int getHowManyDaysLaterShockWillOccur() {
        return howManyDaysLaterShockWillOccur;
    }

    /**
     * Sets new If there is a demand shift, when will it appear?.
     *
     * @param howManyDaysLaterShockWillOccur New value of If there is a demand shift, when will it appear?.
     */
    public void setHowManyDaysLaterShockWillOccur(int howManyDaysLaterShockWillOccur) {
        this.howManyDaysLaterShockWillOccur = howManyDaysLaterShockWillOccur;
    }
}
