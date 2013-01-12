package tests.plantcontrol;

import agents.Person;
import agents.firm.Firm;
import agents.firm.cost.InputCostStrategy;
import agents.firm.cost.PlantCostStrategy;
import agents.firm.personell.HumanResources;
import agents.firm.purchases.PurchasesDepartment;
import model.utilities.ActionOrder;
import model.utilities.pid.PIDController;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.exploration.SimpleBuyerSearch;
import agents.firm.sales.exploration.SimpleSellerSearch;
import agents.firm.sales.pricing.pid.SimpleFlowSellerPID;
import ec.util.MersenneTwisterFast;
import financial.Market;
import financial.OrderBookMarket;
import financial.utilities.Quote;
import financial.utilities.ShopSetPricePolicy;
import goods.Good;
import goods.GoodType;
import agents.firm.production.Blueprint;
import agents.firm.production.Plant;
import agents.firm.production.PlantListener;
import agents.firm.production.control.AbstractPlantControl;
import agents.firm.production.control.DiscreteSlowPlantControl;
import agents.firm.production.control.TargetAndMaximizePlantControl;
import agents.firm.production.control.maximizer.HillClimberMaximizer;
import agents.firm.production.technology.LinearConstantMachinery;
import junit.framework.Assert;
import model.MacroII;
import org.junit.Test;
import sim.engine.SimState;
import sim.engine.Steppable;
import tests.DummyBuyer;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
 * @version 2012-09-01
 * @see
 */
public class DiscreteSlowPlantControlTest {

    @Test
    public void testChangeTargetStep() throws Exception
    {



        MacroII model = new MacroII(10l);
        Firm firm = mock(Firm.class);      when(firm.getRandom()).thenReturn(new MersenneTwisterFast());
        when(firm.getModel()).thenReturn(model);
        Plant plant = mock(Plant.class); when(plant.workerSize()).thenReturn(1); when(plant.getModel()).thenReturn(model);  when(plant.getBuildingCosts()).thenReturn(10000l);
        when(plant.maximumWorkersPossible()).thenReturn(100);
        Blueprint b = Blueprint.simpleBlueprint(GoodType.GENERIC,1,GoodType.GENERIC,1);
        when(plant.getBlueprint()).thenReturn(b);
        when(plant.getRandom()).thenReturn(model.random); when(firm.getRandom()).thenReturn(model.random);
        when(plant.removeListener(any(PlantListener.class))).thenReturn(true);
        when(plant.minimumWorkersNeeded()).thenReturn(1);
        PlantCostStrategy strategy = mock(PlantCostStrategy.class);
        when(plant.getCostStrategy()).thenReturn(strategy);
        when(strategy.weeklyFixedCosts()).thenReturn(10000l);
        Market market = new OrderBookMarket(GoodType.LABOR);   //labor market
        SalesDepartment dept = mock(SalesDepartment.class);
        firm.registerSaleDepartment(dept, GoodType.GENERIC);
        HumanResources hr = null;
        for(int i=0; i <100; i++)
        {
            hr = HumanResources.getHumanResourcesIntegrated(1000000l,
                firm,market,plant,null,null,null);
            assert market.getBuyers().contains(firm);
            hr.turnOff();
            assert !market.getBuyers().contains(firm);

        }
        hr = HumanResources.getHumanResourcesIntegrated(1000000l,
                firm,market,plant,null,null,null);
        assert market.getBuyers().contains(firm);

        DiscreteSlowPlantControl control = new DiscreteSlowPlantControl(hr);
      //  control.setQuickFiring(false);
        hr.setControl(control);
        when(plant.workerSize()).thenReturn(0);
        control.start();
        when(plant.workerSize()).thenReturn(1);


        //force the control to have wage = 50;
        Field field  = DiscreteSlowPlantControl.class.getDeclaredField("control");
        field.setAccessible(true);
        TargetAndMaximizePlantControl realControl = (TargetAndMaximizePlantControl) field.get(control);


        field = AbstractPlantControl.class.getDeclaredField("currentWage");
        field.setAccessible(true);
        field.set(realControl, 50); //wage is set by the control OTHER steppable, so for now let's just control it
        //because I care about profit maximization

        //get the maximizer
        field = TargetAndMaximizePlantControl.class.getDeclaredField("maximizer");
        field.setAccessible(true);
        HillClimberMaximizer maximizer = (HillClimberMaximizer) field.get(realControl);





        //target should be 1
        assertEquals(control.getTarget(), 1);

        //adjust on it once, it should switch to 2
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(100f);
        maximizer.step(model);
        maximizer.step(model);
        assertEquals(control.getTarget(), 2);
        when(plant.workerSize()).thenReturn(2);

        //adjust it again, it should switch to 3
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(180f);
        assertTrue(!maximizer.isCheckWeek());
        maximizer.step(model);
        assertTrue(maximizer.isCheckWeek());
        maximizer.step(model);
        assertEquals(control.getTarget(), 3);
        when(plant.workerSize()).thenReturn(3);


        //adjust it again, it should switch to 4
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(240f);
        assertTrue(!maximizer.isCheckWeek());
        maximizer.step(model);
        assertTrue(maximizer.isCheckWeek());
        maximizer.step(model);
        assertEquals(control.getTarget(), 4);
        when(plant.workerSize()).thenReturn(4);


        //adjust it again, it should switch back to 3
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(200f);
        assertTrue(!maximizer.isCheckWeek());
        maximizer.step(model);
        assertTrue(maximizer.isCheckWeek());
        maximizer.step(model);        assertEquals(control.getTarget(), 3);
        when(plant.workerSize()).thenReturn(3);


        //adjust it again, it should stay at 3
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(240f);
        assertTrue(!maximizer.isCheckWeek());
        maximizer.step(model);
        assertTrue(maximizer.isCheckWeek());
        maximizer.step(model);        assertEquals(control.getTarget(), 3);
        when(plant.workerSize()).thenReturn(3);


        //adjust it again, it should stay at 3
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(240f);
        assertTrue(!maximizer.isCheckWeek());
        maximizer.step(model);
        assertTrue(maximizer.isCheckWeek());
        maximizer.step(model);        assertEquals(control.getTarget(), 3);
        when(plant.workerSize()).thenReturn(3);


        //adjust it again, it should stay at 3
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(240f);
        assertTrue(!maximizer.isCheckWeek());
        maximizer.step(model);
        assertTrue(maximizer.isCheckWeek());
        maximizer.step(model);
        assertEquals(control.getTarget(), 3);
        when(plant.workerSize()).thenReturn(3);


        //adjust it again, now it'll switch to 4!
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(150f);
        assertTrue(!maximizer.isCheckWeek());
        maximizer.step(model);
        assertTrue(maximizer.isCheckWeek());
        maximizer.step(model);
        assertEquals(control.getTarget(), 4);
        when(plant.workerSize()).thenReturn(4);


        //adjust it again, now it'll switch to 3!
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(0f);
        assertTrue(!maximizer.isCheckWeek());
        maximizer.step(model);
        assertTrue(maximizer.isCheckWeek());
        maximizer.step(model);
        assertEquals(control.getTarget(), 3);
        when(plant.workerSize()).thenReturn(3);


        //adjust it again, now it'll switch to 2!
        when(firm.getPlantProfits(any(Plant.class))).thenReturn(0f);
        assertTrue(!maximizer.isCheckWeek());
        maximizer.step(model);
        assertTrue(maximizer.isCheckWeek());
        maximizer.step(model);
        assertEquals(control.getTarget(), 2);
        when(plant.workerSize()).thenReturn(2);



    }


    /**
     * BIG FULLY DRESSED MONOPOLY SCENARIO!
     */
    @Test
    public void monopolyScenario() throws IllegalAccessException, NoSuchFieldException {

        Market.TESTING_MODE = true;
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println("SimpleFlowSeller scenario1");

        final MacroII model = new MacroII(1l);
        final Firm firm = new Firm(model); firm.earn(100000000l);
        final OrderBookMarket market = new OrderBookMarket(GoodType.GENERIC);
        market.setPricePolicy(new ShopSetPricePolicy());
        OrderBookMarket labor = new OrderBookMarket(GoodType.LABOR);

        SalesDepartment dept =SalesDepartment.incompleteSalesDepartment(firm,market,new SimpleBuyerSearch(market,firm), new SimpleSellerSearch(market,firm));
        SimpleFlowSellerPID strategy = new SimpleFlowSellerPID(dept,.3f,.16f,.01f,10);
        strategy.setProductionCostOverride(true);
        dept.setAskPricingStrategy(strategy);
      //  dept.setAskPricingStrategy(new EverythingMustGoAdaptive(dept));
        firm.registerSaleDepartment(dept, GoodType.GENERIC);
        Blueprint blueprint = new Blueprint.Builder().output(GoodType.GENERIC,1).build();
        Plant plant = new Plant(blueprint,firm);
        plant.setPlantMachinery(new LinearConstantMachinery(GoodType.CAPITAL,mock(Firm.class),100000,plant));
        plant.setCostStrategy(new InputCostStrategy(plant));
        firm.addPlant(plant);
        HumanResources hr = HumanResources.getHumanResourcesIntegrated(100000000,firm,labor,plant,DiscreteSlowPlantControl.class,null,null);
     //   firm.registerHumanResources(plant, hr);
        hr.start();

        model.scheduleSoon(ActionOrder.DAWN, new Steppable() {
            @Override
            public void step(SimState state) {
                final List<Quote> quotes =new LinkedList<>();
                fillMarket(market, quotes, model);
                model.scheduleTomorrow(ActionOrder.DAWN,this);

                model.scheduleSoon(ActionOrder.CLEANUP,new Steppable() {
                    @Override
                    public void step(SimState state) {
                        emptyMarket(market,quotes);
                    }
                });

            }
        });


        //add the four workers
        for(int i=0; i<4; i++)
        {
            Person p = new Person(model,0,30+i*10,labor);
            labor.submitSellQuote(p, 30+i*10 , new Good(GoodType.LABOR,p,30+i*10));



        }


        Field field = SimpleFlowSellerPID.class.getDeclaredField("controller");
        field.setAccessible(true);
        PIDController pid = (PIDController) field.get(strategy);

        model.start();
        model.getAgents().clear(); model.getAgents().add(firm);
        do{
            if (!model.schedule.step(model)) break;
   //             System.out.println("the time is " + model.schedule.getTime() +
    //                    "the sales department is selling at: " + strategy.getTargetPrice() + "plant has this many workers: " + plant.workerSize() + " offering wage: " + hr.maxPrice(GoodType.GENERIC,labor));
       //   System.out.println(model.schedule.getTime()+","+strategy.getTargetPrice() +","+ pid.getCurrentMV() +","+plant.workerSize()  + "," + hr.maxPrice(GoodType.LABOR,labor) + "," + market.getBestBuyPrice());
          //  System.out.println("market best bid: " + market.getBestBuyPrice());
        }
        while(model.schedule.getSteps() < 6000);

        Assert.assertEquals(plant.workerSize(),2);
        System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");




    }

    //duopoly scenario: like monopoly but with 2 suppliers!

  //  @Test
    public void duopolyScenario() throws IllegalAccessException, NoSuchFieldException {

        Market.TESTING_MODE = true;
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println("Duopoly scenario1");

        final MacroII model = new MacroII(1l);

        //1
        final Firm firm = new Firm(model); firm.earn(100000000l);
        final OrderBookMarket market = new OrderBookMarket(GoodType.GENERIC);
        market.setPricePolicy(new ShopSetPricePolicy());
        OrderBookMarket labor = new OrderBookMarket(GoodType.LABOR);

        SalesDepartment dept = SalesDepartment.incompleteSalesDepartment(firm,market,new SimpleBuyerSearch(market,firm), new SimpleSellerSearch(market,firm));
        SimpleFlowSellerPID strategy = new SimpleFlowSellerPID(dept,.3f,.16f,.01f,10);
        dept.setAskPricingStrategy(strategy);
        //  dept.setAskPricingStrategy(new EverythingMustGoAdaptive(dept));
        firm.registerSaleDepartment(dept, GoodType.GENERIC);
        Blueprint blueprint = new Blueprint.Builder().output(GoodType.GENERIC,1).build();
        Plant plant = new Plant(blueprint,firm);
        plant.setPlantMachinery(new LinearConstantMachinery(GoodType.CAPITAL,mock(Firm.class),100000,plant));
        plant.setCostStrategy(new InputCostStrategy(plant));
        firm.addPlant(plant);
        HumanResources hr = HumanResources.getHumanResourcesIntegrated(100000000,firm,labor,plant,DiscreteSlowPlantControl.class,null,null);
        firm.registerHumanResources(plant, hr);
        hr.start();

        //2
        final Firm firm2 = new Firm(model); firm2.earn(100000000l);

        SalesDepartment dept2 = SalesDepartment.incompleteSalesDepartment(firm2,market,new SimpleBuyerSearch(market,firm2), new SimpleSellerSearch(market,firm2));
        SimpleFlowSellerPID strategy2 = new SimpleFlowSellerPID(dept2,.3f,.16f,.01f,10);
        dept2.setAskPricingStrategy(strategy2);
        //  dept2.setAskPricingStrategy(new EverythingMustGoAdaptive(dept2));
        firm2.registerSaleDepartment(dept2, GoodType.GENERIC);
        Blueprint blueprint2 = new Blueprint.Builder().output(GoodType.GENERIC,1).build();
        Plant plant2 = new Plant(blueprint2,firm2);
        plant2.setPlantMachinery(new LinearConstantMachinery(GoodType.CAPITAL,mock(Firm.class),100000,plant2));
        plant2.setCostStrategy(new InputCostStrategy(plant2));
        firm2.addPlant(plant2);
        HumanResources hr2 = HumanResources.getHumanResourcesIntegrated(100000000,firm2,labor,plant2,DiscreteSlowPlantControl.class,null,null);
        firm2.registerHumanResources(plant2, hr2);
        hr2.start();


        model.schedule.scheduleRepeating(new Steppable() {
            @Override
            public void step(SimState simState) {

                firm.weekEnd(model.schedule.getTime());         //manual weekend, might want to change this later
                firm2.weekEnd(model.schedule.getTime());
                //    System.out.println("weekend!");



            }
        },1,model.getWeekLength());

        model.schedule.scheduleRepeating(new Steppable() {
            @Override
            public void step(SimState simState) {
                final List<Quote> quotes =new LinkedList<>();
                fillMarket(market, quotes, model);
                simState.schedule.scheduleOnceIn(model.getWeekLength()/10f,
                        new Steppable() {
                            @Override
                            public void step(SimState simState) {
                                emptyMarket(market,quotes);
                            }
                        });

            }
        },2,model.getWeekLength()/10f);

        //add 10 workers (4 is the mr)
        for(int i=0; i<10; i++)
        {
            Person p = new Person(model,0,30+i*10,labor);
            labor.submitSellQuote(p, 30+i*10 , new Good(GoodType.LABOR,p,30+i*10));



        }



        do{
            if (!model.schedule.step(model)) break;

            System.out.println("FIRST FIRM : the time is " + model.schedule.getTime() +
                    "the sales department is selling at: " + strategy.getTargetPrice() + "plant has this many workers: " + plant.workerSize() + " offering wage: " + hr.maxPrice(GoodType.GENERIC,labor));
            System.out.println("SECOND FIRM: the time is " + model.schedule.getTime() +
                    "the sales department is selling at: " + strategy2.getTargetPrice() + "plant has this many workers: " + plant2.workerSize() + " offering wage: " + hr2.maxPrice(GoodType.GENERIC,labor));

        }
        while(model.schedule.getSteps() < 6000);

        Assert.assertEquals(plant.workerSize() + plant2.workerSize(),4);
        System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");




    }

//    @Test
    public void tripolyScenario() throws IllegalAccessException, NoSuchFieldException {

        Market.TESTING_MODE = true;
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println("Duopoly scenario1");

        final MacroII model = new MacroII(1l);

        //1
        final Firm firm = new Firm(model); firm.earn(100000000l);
        final OrderBookMarket market = new OrderBookMarket(GoodType.GENERIC);
        market.setPricePolicy(new ShopSetPricePolicy());
        OrderBookMarket labor = new OrderBookMarket(GoodType.LABOR);

        SalesDepartment dept = SalesDepartment.incompleteSalesDepartment(firm,market,new SimpleBuyerSearch(market,firm), new SimpleSellerSearch(market,firm));
        SimpleFlowSellerPID strategy = new SimpleFlowSellerPID(dept,.3f,.16f,.01f,10);
        dept.setAskPricingStrategy(strategy);
        //  dept.setAskPricingStrategy(new EverythingMustGoAdaptive(dept));
        firm.registerSaleDepartment(dept, GoodType.GENERIC);
        Blueprint blueprint = new Blueprint.Builder().output(GoodType.GENERIC,1).build();
        Plant plant = new Plant(blueprint,firm);
        plant.setPlantMachinery(new LinearConstantMachinery(GoodType.CAPITAL,mock(Firm.class),100000,plant));
        plant.setCostStrategy(new InputCostStrategy(plant));
        firm.addPlant(plant);
        HumanResources hr = HumanResources.getHumanResourcesIntegrated(100000000,firm,labor,plant,DiscreteSlowPlantControl.class,null,null);
        Field field = PurchasesDepartment.class.getDeclaredField("control"); field.setAccessible(true);
        DiscreteSlowPlantControl control = (DiscreteSlowPlantControl) field.get(hr);
        firm.registerHumanResources(plant, hr);
     //   control.setProbabilityForgetting(.15f);

        hr.start();

        //2
        final Firm firm2 = new Firm(model); firm2.earn(100000000l);

        SalesDepartment dept2 = SalesDepartment.incompleteSalesDepartment(firm2,market,new SimpleBuyerSearch(market,firm2), new SimpleSellerSearch(market,firm2));
        SimpleFlowSellerPID strategy2 = new SimpleFlowSellerPID(dept2,.3f,.16f,.01f,10);
        dept2.setAskPricingStrategy(strategy2);
        //  dept2.setAskPricingStrategy(new EverythingMustGoAdaptive(dept2));
        firm2.registerSaleDepartment(dept2, GoodType.GENERIC);
        Blueprint blueprint2 = new Blueprint.Builder().output(GoodType.GENERIC,1).build();
        Plant plant2 = new Plant(blueprint2,firm2);
        plant2.setPlantMachinery(new LinearConstantMachinery(GoodType.CAPITAL,mock(Firm.class),100000,plant2));
        plant2.setCostStrategy(new InputCostStrategy(plant2));
        firm2.addPlant(plant2);
        HumanResources hr2 = HumanResources.getHumanResourcesIntegrated(100000000,firm2,labor,plant2,DiscreteSlowPlantControl.class,null,null);
        field = PurchasesDepartment.class.getDeclaredField("control"); field.setAccessible(true);
        DiscreteSlowPlantControl control2 = (DiscreteSlowPlantControl) field.get(hr2);
     //   control2.setProbabilityForgetting(.15f);

        firm2.registerHumanResources(plant2, hr2);
        hr2.start();

        //2
        final Firm firm3 = new Firm(model); firm3.earn(100000000l);

        SalesDepartment dept3 =SalesDepartment.incompleteSalesDepartment(firm3,market,new SimpleBuyerSearch(market,firm3), new SimpleSellerSearch(market,firm3));
        SimpleFlowSellerPID strategy3 = new SimpleFlowSellerPID(dept3,.3f,.16f,.01f,10);
        dept3.setAskPricingStrategy(strategy3);
        //  dept3.setAskPricingStrategy(new EverythingMustGoAdaptive(dept3));
        firm3.registerSaleDepartment(dept3, GoodType.GENERIC);
        Blueprint blueprint3 = new Blueprint.Builder().output(GoodType.GENERIC,1).build();
        Plant plant3 = new Plant(blueprint3,firm3);
        plant3.setPlantMachinery(new LinearConstantMachinery(GoodType.CAPITAL,mock(Firm.class),100000,plant3));
        plant3.setCostStrategy(new InputCostStrategy(plant3));
        firm3.addPlant(plant3);
        HumanResources hr3 = HumanResources.getHumanResourcesIntegrated(100000000,firm3,labor,plant3,DiscreteSlowPlantControl.class,null,null);
        field = PurchasesDepartment.class.getDeclaredField("control"); field.setAccessible(true);
        DiscreteSlowPlantControl control3 = (DiscreteSlowPlantControl) field.get(hr3);
 //       control3.setProbabilityForgetting(.15f);
        firm3.registerHumanResources(plant3, hr3);
        hr3.start();


        model.schedule.scheduleRepeating(new Steppable() {
            @Override
            public void step(SimState simState) {

                firm.weekEnd(model.schedule.getTime());         //manual weekend, might want to change this later
                firm2.weekEnd(model.schedule.getTime());
                firm3.weekEnd(model.schedule.getTime());
                //    System.out.println("weekend!");



            }
        },1,model.getWeekLength());

        model.schedule.scheduleRepeating(new Steppable() {
            @Override
            public void step(SimState simState) {
                final List<Quote> quotes =new LinkedList<>();
                fillMarket(market, quotes, model);
                simState.schedule.scheduleOnceIn(model.getWeekLength()/10f,
                        new Steppable() {
                            @Override
                            public void step(SimState simState) {
                                emptyMarket(market,quotes);
                            }
                        });

            }
        },2,model.getWeekLength()/10f);

        //add 10 workers (4 is the mr)
        for(int i=0; i<10; i++)
        {
            Person p = new Person(model,0,30+i*10,labor);
            labor.submitSellQuote(p, 30+i*10 , new Good(GoodType.LABOR,p,30+i*10));



        }



        do{
            if (!model.schedule.step(model)) break;
         //   System.out.println("FIRST FIRM : the time is " + model.schedule.getTime() +
         //           "the sales department is selling at: " + strategy.getTargetPrice() + "plant has this many workers: " + plant.workerSize() + " offering wage: " + hr.maxPrice(GoodType.GENERIC,labor));
         //   System.out.println("SECOND FIRM: the time is " + model.schedule.getTime() +
         //           "the sales department is selling at: " + strategy2.getTargetPrice() + "plant has this many workers: " + plant2.workerSize() + " offering wage: " + hr2.maxPrice(GoodType.GENERIC,labor));
            System.out.println(plant.workerSize() + " <>" + dept.getLastClosingPrice() + "<>" + hr.maxPrice(GoodType.GENERIC,labor) + "<>" + firm.getPlantProfits(plant) +
                    " , "
                    + plant2.workerSize() + " <>" + dept2.getLastClosingPrice() + "<>" + hr2.maxPrice(GoodType.GENERIC, labor) + "<>" +firm2.getPlantProfits(plant2) +
                    " , "
                    + plant3.workerSize() + " <>" + dept3.getLastClosingPrice() + "<>" + hr3.maxPrice(GoodType.GENERIC, labor) + "<>" + firm3.getPlantProfits(plant3) +
                    " ----- "
                    + (plant.workerSize() + plant2.workerSize()  + plant3.workerSize()));

        }
        while(model.schedule.getSteps() < 50000);   //todo why it takes so long and why sometimes it's 4 rather than 5?

        Assert.assertEquals(plant.workerSize() + plant2.workerSize()  + plant3.workerSize(),4);
        System.out.println("---------------------------------------------------------------------------------------------------------------------------------------");




    }



    public static void fillMarket(Market market, Collection<Quote> quotes, MacroII model){
        for(int i=9; i>2;i --)
        {

                DummyBuyer buyer = new DummyBuyer(model,i+1);
                market.registerBuyer(buyer);
                buyer.earn(1000l);
                Quote q = market.submitBuyQuote(buyer,i+1);
                quotes.add(q);



        }

    }

    public static void emptyMarket(Market market, Iterable<Quote> quotes){
        for(Quote q : quotes)
            try{
                market.removeBuyQuote(q);
            }
            catch (IllegalArgumentException ignored){}

    }


}
