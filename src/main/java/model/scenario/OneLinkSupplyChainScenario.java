/*
 * Copyright (c) 2013 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.scenario;

import agents.EconomicAgent;
import agents.Person;
import agents.firm.Firm;
import agents.firm.cost.InputCostStrategy;
import agents.firm.personell.FactoryProducedHumanResourcesWithMaximizerAndTargeter;
import agents.firm.personell.HumanResources;
import agents.firm.production.Blueprint;
import agents.firm.production.Plant;
import agents.firm.production.control.TargetAndMaximizePlantControl;
import agents.firm.production.control.maximizer.EveryWeekMaximizer;
import agents.firm.production.control.maximizer.WorkforceMaximizer;
import agents.firm.production.control.maximizer.algorithms.WorkerMaximizationAlgorithm;
import agents.firm.production.control.maximizer.algorithms.marginalMaximizers.MarginalMaximizer;
import agents.firm.production.control.maximizer.algorithms.marginalMaximizers.RobustMarginalMaximizer;
import agents.firm.production.control.targeter.PIDTargeterWithQuickFiring;
import agents.firm.production.technology.LinearConstantMachinery;
import agents.firm.purchases.PurchasesDepartment;
import agents.firm.purchases.pid.PurchasesFixedPID;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.SalesDepartmentFactory;
import agents.firm.sales.SalesDepartmentOneAtATime;
import agents.firm.sales.exploration.BuyerSearchAlgorithm;
import agents.firm.sales.exploration.SellerSearchAlgorithm;
import agents.firm.sales.exploration.SimpleBuyerSearch;
import agents.firm.sales.exploration.SimpleSellerSearch;
import agents.firm.sales.pricing.pid.SalesControlWithFixedInventoryAndPID;
import agents.firm.sales.pricing.pid.SmoothedDailyInventoryPricingStrategy;
import au.com.bytecode.opencsv.CSVWriter;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import financial.market.Market;
import financial.market.OrderBookMarket;
import financial.utilities.BuyerSetPricePolicy;
import financial.utilities.ShopSetPricePolicy;
import goods.Good;
import goods.GoodType;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.dummies.DummyBuyer;
import model.utilities.filters.ExponentialFilter;
import model.utilities.pid.PIDController;
import model.utilities.stats.collectors.DailyStatCollector;
import model.utilities.stats.collectors.enums.SalesDataType;
import sim.engine.SimState;
import sim.engine.Steppable;

import javax.annotation.Nullable;
import java.io.FileWriter;
import java.io.IOException;

import static model.experiments.tuningRuns.MarginalMaximizerPIDTuning.printProgressBar;
import static org.mockito.Mockito.*;

/**
 * <h4>Description</h4>
 * <p/> Very similar to supply-chain scenario, but simpler: Beef-->Food.
 * <p/> Beef only requires labor, food deals directly with fixed market
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-02-20
 * @see
 */
public class OneLinkSupplyChainScenario extends Scenario {

    /**
     * The filter to attach to the beef ask pricing strategy
     */
    public ExponentialFilter<Integer> beefPriceFilterer = null;
    /**
     * If you want to change the proportional gain of BEEF selling pid
     * for this scenario run by dividing it, here's what you are dividing it for
     */
    public float divideProportionalGainByThis = 1f;
    /**
     * If you want to change the proportional gain of BEEF selling pid
     * for this scenario run by dividing it, here's what you are dividing it for
     */
    public float divideIntegrativeGainByThis = 1f;

    /**
     * the sampling speed of the BEEF selling pid
     */
    public int beefPricingSpeed = 100;
    private Class< ? extends WorkforceMaximizer> maximizerType =  EveryWeekMaximizer.class;
    /**
     * should workers act like a flow rather than a stock?
     */
    private boolean workersToBeRehiredEveryDay = true;

    public OneLinkSupplyChainScenario(MacroII model) {
        super(model);
        //instantiate the map
    }


    /**
     * The type of integrated control that is used by human resources in firms to choose production
     */
    private Class<? extends WorkerMaximizationAlgorithm> controlType = RobustMarginalMaximizer.class;

    /**
     * the type of sales department firms use
     */
    private Class<? extends  SalesDepartment> salesDepartmentType = SalesDepartmentOneAtATime.class;

    /**
     * total number of firms producing beef
     */
    private int numberOfBeefProducers = 1;

    /**
     * total number of firms producing food
     */
    private int numberOfFoodProducers = 1;

    /**
     * how many cattles you need for one unit of beef
     */
    private int beefMultiplier = 1;

    /**
     * how many units of beef you need for 1 unit of food
     */
    private int foodMultiplier = 1;

    /**
     * The maximization speed in weeks of beef producers
     */
    private int weeksToMakeObservationBeef = 3;

    /**
     * The maximization speed in weeks of food producers
     */
    private int weeksToMakeObservationFood = 3;



    //this is public only so that I can log it!
    @VisibleForTesting
    public SmoothedDailyInventoryPricingStrategy strategy2;


    /**
     * Called by MacroII, it creates agents and then schedules them.
     */
    @Override
    public void start()
    {



        //build markets and put firms in them
        instantiateMarkets();
        populateMarkets();

        //create consumers
        buildFoodDemand(0,100,1,getMarkets().get(GoodType.FOOD));

        //create workers
        buildLaborSupplies();



    }

    private void buildLaborSupplies() {
        addWorkers(getMarkets().get(GoodType.LABOR_BEEF),5,1200,5);
        addWorkers(getMarkets().get(GoodType.LABOR_FOOD),5,1200,5);
    }

    private void populateMarkets() {
        for(int i=0; i < numberOfBeefProducers; i++)
            createFirm(getMarkets().get(GoodType.BEEF),getMarkets().get(GoodType.LABOR_BEEF));
        //food
        for(int i=0; i < numberOfFoodProducers; i++)
            createFirm(getMarkets().get(GoodType.FOOD),getMarkets().get(GoodType.LABOR_FOOD));
    }

    /**
     * Instantiate all the markets
     */
    private void instantiateMarkets() {
        Market beef = new OrderBookMarket(GoodType.BEEF);
        beef.setPricePolicy(new ShopSetPricePolicy());
        getMarkets().put(beef.getGoodType(), beef);
        Market food = new OrderBookMarket(GoodType.FOOD);
        food.setPricePolicy(new ShopSetPricePolicy());
        getMarkets().put(food.getGoodType(), food);


        Market beefLabor = new OrderBookMarket(GoodType.LABOR_BEEF);
        beefLabor.setPricePolicy(new BuyerSetPricePolicy());
        getMarkets().put(beefLabor.getGoodType(),beefLabor);
        Market foodLabor = new OrderBookMarket(GoodType.LABOR_FOOD);
        beefLabor.setPricePolicy(new BuyerSetPricePolicy());
        getMarkets().put(foodLabor.getGoodType(),foodLabor);
    }

    protected Firm createFirm(final Market goodmarket, final Market laborMarket)
    {
        final Firm firm = new Firm(getModel());
        firm.earn(Integer.MAX_VALUE);
        firm.setName(goodmarket.getGoodType().name() + " producer " + getModel().random.nextInt());
        //give it a seller department at time 1

        //set up the firm at time 1
        getModel().scheduleSoon(ActionOrder.DAWN, new Steppable() {
            @Override
            public void step(SimState simState) {
                //CREATE THE SALES DEPARTMENT
                createSalesDepartment(firm, goodmarket);


                //CREATE THE PLANT + Human resources
                Blueprint blueprint =  getBluePrint(goodmarket.getGoodType());
                createPlant(blueprint, firm, laborMarket);

                //CREATE THE PURCHASES DEPARTMENTS NEEDED
                createPurchaseDepartment(blueprint, firm);

            }
        });

        getAgents().add(firm);
        return firm;
    }

    protected void createSalesDepartment(Firm firm, Market goodmarket) {
        SalesDepartment dept = SalesDepartmentFactory.incompleteSalesDepartment(firm, goodmarket,
                new SimpleBuyerSearch(goodmarket, firm), new SimpleSellerSearch(goodmarket, firm),
                salesDepartmentType);
        firm.registerSaleDepartment(dept, goodmarket.getGoodType());




        if(!goodmarket.getGoodType().equals(GoodType.FOOD))
        {

            strategy2 = new SmoothedDailyInventoryPricingStrategy(dept,
                    model.drawProportionalGain()/ divideProportionalGainByThis,
                    model.drawIntegrativeGain()/ divideIntegrativeGainByThis,
                    model.drawDerivativeGain());

      /*      strategy2 =
                    new SalesControlFlowPIDWithFixedInventoryButTargetingFlowsOnly(dept,10,100,model,
                            model.drawProportionalGain()/ divideProportionalGainByThis,
                            model.drawIntegrativeGain()/ divideIntegrativeGainByThis,
                            model.drawDerivativeGain(),model.getRandom());
                            */
            strategy2.setInitialPrice(50);
            //if you can, filter it!
          /*  if(strategy2 instanceof SalesControlFlowPIDWithFixedInventoryButTargetingFlowsOnly && beefPriceFilterer != null)
                strategy2.attachFilter(beefPriceFilterer);
                */
            strategy2.setSpeed(beefPricingSpeed);
            dept.setAskPricingStrategy(strategy2);


            buildBeefSalesPredictor(dept);


        }
        else
        {
            SalesControlWithFixedInventoryAndPID strategy;
            strategy = new SalesControlWithFixedInventoryAndPID(dept);
            strategy.setTargetInventory(100);
            strategy.setInitialPrice(model.random.nextInt(30)+70);
            // strategy.setProductionCostOverride(false);
            dept.setAskPricingStrategy(strategy); //set strategy to PID
        }
    }

    protected void buildBeefSalesPredictor(SalesDepartment dept) {


    }

    protected void createPurchaseDepartment(Blueprint blueprint, Firm firm) {
        for(GoodType input : blueprint.getInputs().keySet()){
            PurchasesDepartment department = PurchasesDepartment.getEmptyPurchasesDepartment(Long.MAX_VALUE, firm,
                    getMarkets().get(input));
            float proportionalGain = model.drawProportionalGain();
            float integralGain = model.drawIntegrativeGain();
            float derivativeGain = model.drawDerivativeGain();
            Market market = model.getMarket(input);

            department.setOpponentSearch(new SimpleBuyerSearch(market, firm));
            department.setSupplierSearch(new SimpleSellerSearch(market, firm));

            PurchasesFixedPID control = new PurchasesFixedPID(department,200, PIDController.class,model);

            department.setControl(control);
            department.setPricingStrategy(control);
            firm.registerPurchasesDepartment(department, input);



        }
    }



    protected void createPlant(Blueprint blueprint, Firm firm, Market laborMarket) {
        Plant plant = new Plant(blueprint, firm);
        plant.setPlantMachinery(new LinearConstantMachinery(GoodType.CAPITAL, mock(Firm.class), 0, plant));
        plant.setCostStrategy(new InputCostStrategy(plant));
        firm.addPlant(plant);
        FactoryProducedHumanResourcesWithMaximizerAndTargeter<TargetAndMaximizePlantControl,BuyerSearchAlgorithm,
                SellerSearchAlgorithm,PIDTargeterWithQuickFiring,WorkforceMaximizer<WorkerMaximizationAlgorithm>,WorkerMaximizationAlgorithm>
                produced =
                HumanResources.getHumanResourcesIntegrated(Long.MAX_VALUE,firm,laborMarket,plant,
                PIDTargeterWithQuickFiring.class, maximizerType,controlType,null,null);

    /*    if(blueprint.getOutputs().containsKey(GoodType.BEEF))
            produced.getWorkforceMaximizer().setWeeksToMakeObservation(weeksToMakeObservationBeef);
        else
            produced.getWorkforceMaximizer().setWeeksToMakeObservation(weeksToMakeObservationFood);
      */


        HumanResources hr = produced.getDepartment();
        hr.setFixedPayStructure(true);
    }

    /**
     * creates dummy buyers (consumers) eating food
     */
    private void buildFoodDemand(int minPrice, int maxPrice, int increments,
                                 final Market marketToBuyFrom)
    {
        Preconditions.checkArgument(minPrice <= maxPrice);

        for(int i=minPrice; i<=maxPrice; i = i + increments)
        {
            createFoodConsumer(marketToBuyFrom, i);

        }



    }

    private void createFoodConsumer(final Market marketToBuyFrom, final int reservationPrice) {
        /**
         * For this scenario we use a different kind of dummy buyer that, after "period" passed, puts a new order in the market
         */
        final DummyBuyer buyer = new DummyBuyer(getModel(), reservationPrice,marketToBuyFrom){
            @Override
            public void reactToFilledBidQuote(Good g, long price, final EconomicAgent b) {
                consume(g.getType());
                //trick to get the steppable to recognize the anonymous me!
                final DummyBuyer reference = this;
                //schedule a new quote in period!
                this.getModel().scheduleTomorrow(ActionOrder.TRADE, new Steppable() {
                    @Override
                    public void step(SimState simState) {
                        earn(Math.max(1000l - reference.getCash(),0));
                        //put another quote
                        marketToBuyFrom.submitBuyQuote(reference, getFixedPrice());

                    }
                });

            }

            @Override
            public String toString() {
                return "Food buyer, price: " + reservationPrice;

            }
        };


        //make it adjust once to register and submit the first quote

        getModel().scheduleSoon(ActionOrder.DAWN, new Steppable() {
            @Override
            public void step(SimState simState) {
                marketToBuyFrom.registerBuyer(buyer);
                buyer.earn(1000l);
                //make the buyer submit a quote soon.
                marketToBuyFrom.submitBuyQuote(buyer, buyer.getFixedPrice());
            }
        });

        getAgents().add(buyer);
    }


    /**
     * Given the good you have to produce, make the blueprint that makes sense. An helper method
     * @param output the goodtype you are supposed to produce
     * @return
     */
    private Blueprint getBluePrint(GoodType output)
    {
        switch (output)
        {
            case BEEF:
                return new Blueprint.Builder().output(GoodType.BEEF,1).build();
            case FOOD:
                return Blueprint.simpleBlueprint(GoodType.BEEF,1,GoodType.FOOD,foodMultiplier);
            default:
                assert false;
                return null;
        }
    }



    private void addWorkers(Market laborMarket, int minWage, int maxWage, int increments)
    {
        /************************************************
         * Add workers
         ************************************************/

        for(int i=minWage; i<maxWage; i= i + increments)
        {
            //dummy worker, really
            final Person p = new Person(getModel(),0l,i,laborMarket);
            p.setPrecario(workersToBeRehiredEveryDay);

            p.setSearchForBetterOffers(false);


            getAgents().add(p);

        }
    }




    /**
     * Sets new total number of firms producing beef.
     *
     * @param numberOfBeefProducers New value of total number of firms producing beef.
     */
    public void setNumberOfBeefProducers(int numberOfBeefProducers) {
        this.numberOfBeefProducers = numberOfBeefProducers;
    }

    /**
     * Gets total number of firms producing food.
     *
     * @return Value of total number of firms producing food.
     */
    public int getNumberOfFoodProducers() {
        return numberOfFoodProducers;
    }

    /**
     * Sets new how many units of beef you need for 1 unit of food.
     *
     * @param foodMultiplier New value of how many units of beef you need for 1 unit of food.
     */
    public void setFoodMultiplier(int foodMultiplier) {
        this.foodMultiplier = foodMultiplier;
    }

    /**
     * Gets how many units of beef you need for 1 unit of food.
     *
     * @return Value of how many units of beef you need for 1 unit of food.
     */
    public int getFoodMultiplier() {
        return foodMultiplier;
    }

    /**
     * Sets new how many cattles you need for one unit of beef.
     *
     * @param beefMultiplier New value of how many cattles you need for one unit of beef.
     */
    public void setBeefMultiplier(int beefMultiplier) {
        this.beefMultiplier = beefMultiplier;
    }

    /**
     * Gets how many cattles you need for one unit of beef.
     *
     * @return Value of how many cattles you need for one unit of beef.
     */
    public int getBeefMultiplier() {
        return beefMultiplier;
    }

    /**
     * Gets total number of firms producing beef.
     *
     * @return Value of total number of firms producing beef.
     */
    public int getNumberOfBeefProducers() {
        return numberOfBeefProducers;
    }

    /**
     * Sets new total number of firms producing food.
     *
     * @param numberOfFoodProducers New value of total number of firms producing food.
     */
    public void setNumberOfFoodProducers(int numberOfFoodProducers) {
        this.numberOfFoodProducers = numberOfFoodProducers;
    }





    /**
     * Runs the supply chain with no GUI and writes a big CSV file
     * @param args
     */
    public static void main(String[] args)
    {


        final MacroII macroII = new MacroII(0);
        final OneLinkSupplyChainScenario scenario1 = new OneLinkSupplyChainScenario(macroII);
        scenario1.setControlType(MarginalMaximizer.class);
        scenario1.setSalesDepartmentType(SalesDepartmentOneAtATime.class);

        scenario1.setNumberOfBeefProducers(1);
        scenario1.setNumberOfFoodProducers(5);






        macroII.setScenario(scenario1);
        macroII.start();

        //create the CSVWriter
        try {
            CSVWriter writer = new CSVWriter(new FileWriter("runs/supplychai/stickyPrices.csv"));
            DailyStatCollector collector = new DailyStatCollector(macroII,writer);
            collector.start();

        } catch (IOException e) {
            System.err.println("failed to create the file!");
        }


        //create the CSVWriter  for purchases prices
        try {
            final CSVWriter writer2 = new CSVWriter(new FileWriter("runs/supplychai/onelinkOfferPrices.csv"));
            writer2.writeNext(new String[]{"buyer offer price","target","filtered Outflow"});
            macroII.scheduleSoon(ActionOrder.CLEANUP_DATA_GATHERING, new Steppable() {
                @Override
                public void step(SimState state) {
                    try {
                        writer2.writeNext(new String[]{String.valueOf(
                                macroII.getMarket(GoodType.BEEF).getBestBuyPrice()),
                                String.valueOf(scenario1.strategy2.getTargetInventory()),
                                String.valueOf(scenario1.strategy2.getDepartment().getLatestObservation(SalesDataType.HOW_MANY_TO_SELL))});
                        writer2.flush();
                        ((MacroII) state).scheduleTomorrow(ActionOrder.CLEANUP_DATA_GATHERING, this);
                    } catch (IllegalAccessException | IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }

                }
            });

        } catch (IOException e) {
            System.err.println("failed to create the file!");
        }





        while(macroII.schedule.getTime()<15000)
        {
            macroII.schedule.step(macroII);
            printProgressBar(15001,(int)macroII.schedule.getSteps(),100);
        }


    }





    /**
     * Gets the type of sales department firms use.
 *
     * @return Value of the type of sales department firms use.
     */
    public Class<? extends SalesDepartment> getSalesDepartmentType() {
        return salesDepartmentType;
    }

    /**
     * Sets new the type of sales department firms use.
     *
     * @param salesDepartmentType New value of the type of sales department firms use.
     */
    public void setSalesDepartmentType(Class<? extends SalesDepartment> salesDepartmentType) {
        this.salesDepartmentType = salesDepartmentType;
    }


    /**
     * Gets If you want to change the proportional gain of BEEF selling pid
     * for this scenario run by dividing it, here's what you are dividing it for.
     *
     * @return Value of If you want to change the proportional gain of BEEF selling pid
     *         for this scenario run by dividing it, here's what you are dividing it for.
     */
    public float getDivideIntegrativeGainByThis() {
        return divideIntegrativeGainByThis;
    }

    /**
     * Gets If you want to change the proportional gain of BEEF selling pid
     * for this scenario run by dividing it, here's what you are dividing it for.
     *
     * @return Value of If you want to change the proportional gain of BEEF selling pid
     *         for this scenario run by dividing it, here's what you are dividing it for.
     */
    public float getDivideProportionalGainByThis() {
        return divideProportionalGainByThis;
    }

    /**
     * Gets the sampling speed of the BEEF selling pid.
     *
     * @return Value of the sampling speed of the BEEF selling pid.
     */
    public int getBeefPricingSpeed() {
        return beefPricingSpeed;
    }

    /**
     * Sets new the sampling speed of the BEEF selling pid.
     *
     * @param beefPricingSpeed New value of the sampling speed of the BEEF selling pid.
     */
    public void setBeefPricingSpeed(int beefPricingSpeed) {
        this.beefPricingSpeed = beefPricingSpeed;
    }


    /**
     * Sets new If you want to change the proportional gain of BEEF selling pid
     * for this scenario run by dividing it, here's what you are dividing it for.
     *
     * @param divideIntegrativeGainByThis New value of If you want to change the proportional gain of BEEF selling pid
     *                                    for this scenario run by dividing it, here's what you are dividing it for.
     */
    public void setDivideIntegrativeGainByThis(float divideIntegrativeGainByThis) {
        this.divideIntegrativeGainByThis = divideIntegrativeGainByThis;
    }

    /**
     * Sets new If you want to change the proportional gain of BEEF selling pid
     * for this scenario run by dividing it, here's what you are dividing it for.
     *
     * @param divideProportionalGainByThis New value of If you want to change the proportional gain of BEEF selling pid
     *                                     for this scenario run by dividing it, here's what you are dividing it for.
     */
    public void setDivideProportionalGainByThis(float divideProportionalGainByThis) {
        this.divideProportionalGainByThis = divideProportionalGainByThis;
    }


    /**
     * Gets The filter to attach to the beef ask pricing strategy.
     *
     * @return Value of The filter to attach to the beef ask pricing strategy.
     */
    public @Nullable ExponentialFilter<Integer> getBeefPriceFilterer() {
        return beefPriceFilterer;
    }


    /**
     * Sets new The filter to attach to the beef ask pricing strategy.
     *
     * @param beefPriceFilterer New value of The filter to attach to the beef ask pricing strategy.
     */
    public void setBeefPriceFilterer(@Nullable ExponentialFilter<Integer> beefPriceFilterer) {
        this.beefPriceFilterer = beefPriceFilterer;
    }

    /**
     * Gets The type of integrated control that is used by human resources in firms to choose production.
     *
     * @return Value of The type of integrated control that is used by human resources in firms to choose production.
     */
    public Class<? extends WorkerMaximizationAlgorithm> getControlType() {
        return controlType;
    }

    /**
     * Sets new The type of integrated control that is used by human resources in firms to choose production.
     *
     * @param controlType New value of The type of integrated control that is used by human resources in firms to choose production.
     */
    public void setControlType(Class<? extends WorkerMaximizationAlgorithm> controlType) {
        this.controlType = controlType;
    }


    /**
     * Sets new The maximization speed in weeks of beef producers.
     *
     * @param weeksToMakeObservationBeef New value of The maximization speed in weeks of beef producers.
     */
    public void setWeeksToMakeObservationBeef(int weeksToMakeObservationBeef) {
        this.weeksToMakeObservationBeef = weeksToMakeObservationBeef;
    }

    /**
     * Gets The maximization speed in weeks of food producers.
     *
     * @return Value of The maximization speed in weeks of food producers.
     */
    public int getWeeksToMakeObservationFood() {
        return weeksToMakeObservationFood;
    }

    /**
     * Sets new The maximization speed in weeks of food producers.
     *
     * @param weeksToMakeObservationFood New value of The maximization speed in weeks of food producers.
     */
    public void setWeeksToMakeObservationFood(int weeksToMakeObservationFood) {
        this.weeksToMakeObservationFood = weeksToMakeObservationFood;
    }

    /**
     * Gets The maximization speed in weeks of beef producers.
     *
     * @return Value of The maximization speed in weeks of beef producers.
     */
    public int getWeeksToMakeObservationBeef() {
        return weeksToMakeObservationBeef;
    }


    public Class<? extends WorkforceMaximizer> getMaximizerType() {
        return maximizerType;
    }

    public void setMaximizerType(Class<? extends WorkforceMaximizer> maximizerType) {
        this.maximizerType = maximizerType;
    }

    /**
     * Sets new should workers act like a flow rather than a stock?.
     *
     * @param workersToBeRehiredEveryDay New value of should workers act like a flow rather than a stock?.
     */
    public void setWorkersToBeRehiredEveryDay(boolean workersToBeRehiredEveryDay) {
        this.workersToBeRehiredEveryDay = workersToBeRehiredEveryDay;
    }

    /**
     * Gets should workers act like a flow rather than a stock?.
     *
     * @return Value of should workers act like a flow rather than a stock?.
     */
    public boolean isWorkersToBeRehiredEveryDay() {
        return workersToBeRehiredEveryDay;
    }
}
