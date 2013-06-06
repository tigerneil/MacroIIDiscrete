package tests;

import agents.Person;
import agents.firm.Firm;
import agents.firm.cost.EmptyCostStrategy;
import agents.firm.cost.InputCostStrategy;
import agents.firm.production.Blueprint;
import agents.firm.production.Plant;
import agents.firm.production.PlantListener;
import agents.firm.production.PlantStatus;
import agents.firm.production.technology.CRSExponentialMachinery;
import agents.firm.production.technology.DRSExponentialMachinery;
import agents.firm.production.technology.IRSExponentialMachinery;
import agents.firm.production.technology.LinearConstantMachinery;
import agents.firm.sales.SalesDepartmentAllAtOnce;
import goods.Good;
import goods.GoodType;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.scheduler.PhaseScheduler;
import model.utilities.scheduler.RandomQueuePhaseScheduler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import sim.engine.Schedule;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: carrknight
 * Date: 7/14/12
 * Time: 1:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class PlantTest {


    Plant crs;
    Plant irs;
    Plant drs;
    MacroII macro;


    @Before
    public void setUp() throws Exception {

        Blueprint b = new Blueprint.Builder().output(GoodType.GENERIC,2).build(); //create a simple output
        macro = new MacroII(1l);
        macro.schedule = mock(Schedule.class); //put in a fake schedule so we avoid steppables firing at random


        Firm f = new Firm(macro);
        crs = new Plant(b,new Firm(macro)); crs.setPlantMachinery(new CRSExponentialMachinery(GoodType.CAPITAL,f,0l,crs, 1f, 1f));
        irs = new Plant(b,new Firm(macro)); irs.setPlantMachinery(new IRSExponentialMachinery(GoodType.CAPITAL,f,0l,irs, 1f, 1f));
        drs = new Plant(b,new Firm(macro)); drs.setPlantMachinery(new DRSExponentialMachinery(GoodType.CAPITAL,f,0l,drs, 1f, 1f));

        Person w1 = new Person(macro); Person w2 = new Person(macro);
        crs.addWorker(w1);crs.addWorker(w2);    w1.hired(crs.getOwner(),9999999); w2.hired(crs.getOwner(), 9999999);
        w1 = new Person(macro); w2 = new Person(macro);
        irs.addWorker(w1);irs.addWorker(w2);    w1.hired(crs.getOwner(),9999999); w2.hired(crs.getOwner(), 9999999);
        w1 = new Person(macro);  w2 = new Person(macro);
        drs.addWorker(w1);drs.addWorker(w2);   w1.hired(crs.getOwner(),9999999); w2.hired(crs.getOwner(), 9999999);


        SalesDepartmentAllAtOnce stub = mock(SalesDepartmentAllAtOnce.class);
        crs.getOwner().registerSaleDepartment(stub,GoodType.GENERIC); //fake sales department so that you don't sell the stuff you completeProductionRunNow
        irs.getOwner().registerSaleDepartment(stub,GoodType.GENERIC); //fake sales department so that you don't sell the stuff you completeProductionRunNow
        drs.getOwner().registerSaleDepartment(stub, GoodType.GENERIC); //fake sales department so that you don't sell the stuff you completeProductionRunNow
        when(stub.isSelling(any(Good.class))).thenReturn(Boolean.TRUE);


        crs.setCostStrategy(new EmptyCostStrategy());
        irs.setCostStrategy(new EmptyCostStrategy());
        drs.setCostStrategy(new EmptyCostStrategy());

    }


    @Test
    public void testGetModel() throws Exception {

        assertNotNull(crs.getModel());
        assertNotNull(irs.getModel());
        assertNotNull(drs.getModel());

        assertSame(crs.getModel(), irs.getModel());
        assertSame(drs.getModel(), irs.getModel());


    }

    @Test
    public void testGetBlueprint() throws Exception {

        assertNotNull(crs.getBlueprint());
        assertNotNull(irs.getBlueprint());
        assertNotNull(drs.getBlueprint());

        assertSame(crs.getModel(), irs.getModel());
        assertSame(drs.getModel(), irs.getModel());

    }

    @Test
    public void testWorkerSize() throws Exception {

        assertEquals(crs.workerSize(), 2);
        Person w = crs.removeLastWorker();
        assertEquals(crs.workerSize(), 1);
        crs.addWorker(w);
        assertEquals(crs.workerSize(), 2);
    }

    @Test
    public void testCheckForInputs() throws Exception {

        assertTrue(crs.checkForInputs());  //the setup has no input need
        crs.getModel().schedule.scheduleOnce(Schedule.EPOCH,new Person(crs.getModel())); //this is to have the steppable not at time -1
        crs.getModel().schedule.step(crs.getModel());

        Blueprint b = Blueprint.simpleBlueprint(GoodType.GENERIC,1,GoodType.GENERIC,2); //addSalesDepartmentListener one for input
        crs.setBlueprint(b); //set it as blueprint
        assertFalse(crs.checkForInputs()); //you should miss one
        crs.getOwner().receive(new Good(GoodType.GENERIC,drs.getOwner(),1l),drs.getOwner()); //receive it
        //this automatically steps production (because it's inventory listener!)


        assertTrue(crs.checkForInputs()); //all good now


    }

    @Test
    public void testCheckForWorkers() throws Exception {

        assertTrue(crs.checkForWorkers());  //you have two workers, the minimum is one, you should be okay

        crs.removeLastWorker(); //remove a worker
        assertTrue(crs.checkForWorkers()); //should still work
        Person w = crs.removeLastWorker();
        assertTrue(!crs.checkForWorkers()); //should still work
        crs.addWorker(w);
        assertTrue(crs.checkForWorkers()); //should still work

    }

    @Test(expected=IllegalArgumentException.class)
    public void addWorkerTwiceErrorTest(){

        Person newWorker = new Person(crs.getModel());
        crs.addWorker(newWorker);
        crs.addWorker(newWorker);


    }


    @Test
    public void addWorkers(){

        Person worker1 = new Person(crs.getModel());
        Person worker2 = new Person(crs.getModel());

        assertEquals(crs.getWorkers().size(), 2);  //it starts with two
        PlantListener fakeListener = mock(PlantListener.class); //addSalesDepartmentListener a fake listener
        crs.addListener(fakeListener);
        crs.addWorkers(worker1, worker2);  worker1.hired(crs.getOwner(),9999999); worker2.hired(crs.getOwner(),9999999);
        assertEquals(crs.getWorkers().size(), 4);
        assertTrue(crs.getWorkers().contains(worker1));
        assertTrue(crs.getWorkers().contains(worker2));
        Mockito.verify(fakeListener,times(1)).changeInWorkforceEvent(any(Plant.class),any(Integer.class)); //make sure it was called just once!!! That's the all point of this method




    }

    @Test
    public void testProduce() throws Exception {

        macro.schedule = new Schedule(); //create a real schedule, please.
        assertTrue(crs.getModel().schedule.scheduleComplete()); //the schedule should be empty!
        assertTrue(!crs.getOwner().hasAny(GoodType.GENERIC));
        assertTrue(crs.getOwner().hasHowMany(GoodType.GENERIC) == 0);


        crs.getModel().schedule.reset();
        crs.getModel().schedule.scheduleOnce(Schedule.EPOCH, new Person(crs.getModel())); //this is to have the steppable not at time -1
        crs.setCostStrategy(new EmptyCostStrategy());
        //    crs.completeProductionRunNow();



        crs.startProductionRun();
        // System.out.println(crs.getModel().schedule.getTime());
        assertEquals(crs.getStatus(), PlantStatus.READY);

        assertTrue(crs.getOwner().hasAny(GoodType.GENERIC));
        assertTrue(crs.getOwner().hasHowMany(GoodType.GENERIC) == 2);






        crs.setBlueprint(Blueprint.simpleBlueprint(GoodType.GENERIC, 2, GoodType.GENERIC, 1)); //dumb technology to test inputs
        crs.startProductionRun();
        assertEquals(crs.getStatus(), PlantStatus.READY);
        assertTrue(crs.getOwner().hasAny(GoodType.GENERIC));       //should still have the stuff
        assertTrue(crs.getOwner().hasHowMany(GoodType.GENERIC) == 1);  //burned two, made one
        crs.startProductionRun();
        assertEquals(crs.getStatus(), PlantStatus.WAITING_FOR_INPUT);
        assertTrue(crs.getOwner().hasAny(GoodType.GENERIC));       //no change!
        assertTrue(crs.getOwner().hasHowMany(GoodType.GENERIC) == 1);










    }

    @Test
    public void testStep() throws Exception {


        Blueprint b = Blueprint.simpleBlueprint(GoodType.GENERIC, 1, GoodType.GENERIC, 1);
        MacroII localMacroII = new MacroII(1l);
        localMacroII.schedule = mock(Schedule.class); //put in a fake schedule so we avoid steppables firing at random


        Firm f = new Firm(localMacroII);
        Plant localCRS = new Plant(b,new Firm(localMacroII)); localCRS.setPlantMachinery(new CRSExponentialMachinery(GoodType.CAPITAL, f, 0l, localCRS, 1f, 1f));
        f.addPlant(localCRS);


        SalesDepartmentAllAtOnce stub = mock(SalesDepartmentAllAtOnce.class);
        when(stub.isSelling(any(Good.class))).thenReturn(Boolean.TRUE);
        localCRS.getOwner().registerSaleDepartment(stub,GoodType.GENERIC); //fake sales
        // department so that you don't try selling the stuff you build


        localCRS.setCostStrategy(new EmptyCostStrategy());
        try{
            localCRS.getModel();

            localCRS.getModel().getPhaseScheduler().step(localCRS.getModel());



            assertEquals(localCRS.getStatus(), PlantStatus.WAITING_FOR_WORKERS);
            localCRS.addWorker(new Person(localCRS.getModel()));
            localCRS.addWorker(new Person(localCRS.getModel()));


            localCRS.getModel().getPhaseScheduler().step(localCRS.getModel());

            assertEquals(localCRS.getStatus(), PlantStatus.WAITING_FOR_INPUT);
            localCRS.getOwner().receive(new Good(GoodType.GENERIC, drs.getOwner(), 1l), drs.getOwner());

            localCRS.getModel().getPhaseScheduler().step(localCRS.getModel());
            assertEquals(localCRS.getStatus(), PlantStatus.READY); //you should automatically start production!

        }catch(Exception e){
            fail("Can't throw exceptions now");
        }








    }


    @Test
    public void testWeekEnd() throws Exception {

        assertEquals(irs.getStatus(), PlantStatus.READY);

        for(int i=0; i < irs.getUsefulLife(); i++){
            assertEquals(irs.getStatus(), PlantStatus.READY);
            irs.weekEnd(0l);
        }

        assertEquals(irs.getStatus(), PlantStatus.OBSOLETE);


    }

    @Test
    public void testGetPlantTechnology() throws Exception {
        //make sure all delegate methods work
        assertEquals(1.0f, crs.hypotheticalWaitingTime(1), .0001f);
        assertEquals(1.0f, irs.hypotheticalWaitingTime(1), .0001f);
        assertEquals(1.0f, drs.hypotheticalWaitingTime(1), .0001f);


        assertEquals(0.5f, crs.hypotheticalWaitingTime(2), .0001f);
        assertEquals(0.25f, irs.hypotheticalWaitingTime(2), .0001f);
        assertEquals(1f / 1.41421356f, drs.hypotheticalWaitingTime(2), .0001f);


        assertEquals(1.0f * crs.getModel().getWeekLength(), crs.hypotheticalWeeklyProductionRuns(1), .0001f);
        assertEquals(1.0f * irs.getModel().getWeekLength(), irs.hypotheticalWeeklyProductionRuns(1), .0001f);
        assertEquals(1.0f * drs.getModel().getWeekLength(), drs.hypotheticalWeeklyProductionRuns(1), .0001f);

        assertEquals(2.0f * crs.getModel().getWeekLength(), crs.hypotheticalWeeklyProductionRuns(2), .0001f);
        assertEquals(4.0f * irs.getModel().getWeekLength(), irs.hypotheticalWeeklyProductionRuns(2), .0001f);
        assertEquals(1.41421356f * drs.getModel().getWeekLength(), drs.hypotheticalWeeklyProductionRuns(2), .0001f);

        assertEquals(0.5f, crs.expectedWaitingTime(), .0001f);
        assertEquals(0.25f, irs.expectedWaitingTime(), .0001f);
        assertEquals(1f / 1.41421356f, drs.expectedWaitingTime(), .0001f);


        assertEquals(2f * 2.0f * crs.getModel().getWeekLength(), crs.expectedWeeklyProduction(GoodType.GENERIC), .0001f);
        assertEquals(2f * 4.0f * drs.getModel().getWeekLength(), irs.expectedWeeklyProduction(GoodType.GENERIC), .0001f);
        assertEquals(2f * 1.41421356f * irs.getModel().getWeekLength(), drs.expectedWeeklyProduction(GoodType.GENERIC), .0001f);

        assertEquals(2f * crs.getModel().getWeekLength(), crs.marginalProductOfWorker(GoodType.GENERIC), .0001f);
        assertEquals(2f * 5.0f * irs.getModel().getWeekLength(), irs.marginalProductOfWorker(GoodType.GENERIC), .0001f);
        assertEquals(2f * 0.317837245 * drs.getModel().getWeekLength(), drs.marginalProductOfWorker(GoodType.GENERIC), .0001f);




    }

    @Test
    public void testTotalInputs()
    {

        Blueprint b = Blueprint.simpleBlueprint(GoodType.GENERIC, 1, GoodType.BEEF, 1);
        MacroII localMacroII =  mock(MacroII.class);
        PhaseScheduler scheduler = mock(RandomQueuePhaseScheduler.class); when(localMacroII.getCurrentPhase()).thenReturn(ActionOrder.PRODUCTION);
        when(localMacroII.getPhaseScheduler()).thenReturn(scheduler);  when(localMacroII.getWeekLength()).thenReturn(7f);
        Firm f = new Firm(localMacroII);
        Plant localCRS = new Plant(b,new Firm(localMacroII)); localCRS.setPlantMachinery(new LinearConstantMachinery(GoodType.CAPITAL, f, 0l, localCRS));
        f.addPlant(localCRS);
        localCRS.addWorker(new Person(localCRS.getModel()));
        localCRS.addWorker(new Person(localCRS.getModel()));
        localCRS.getOwner().registerSaleDepartment(mock(SalesDepartmentAllAtOnce.class),GoodType.BEEF); //fake sales department so that you don't sell the stuff you completeProductionRunNow
        localCRS.setCostStrategy(new InputCostStrategy(localCRS));


        //initially they should be set up to 0
        assertEquals(localCRS.getThisWeekInputCosts(), 0l);
        assertEquals(localCRS.getLastWeekInputCosts(), 0l);

        //now when I step you, you should  say you are waiting for inputs
        localCRS.getModel();
        localCRS.step(localMacroII);
        assertEquals(localCRS.getStatus(), PlantStatus.WAITING_FOR_INPUT);
        assertEquals(localCRS.getThisWeekInputCosts(), 0l); //input costs should still be 0
        assertEquals(localCRS.getLastWeekInputCosts(), 0l);



        //now I give you one input that costs 10$
        localCRS.getOwner().receive(new Good(GoodType.GENERIC, drs.getOwner(), 10l), drs.getOwner());
        localCRS.step(localCRS.getModel());
        assertEquals(localCRS.getThisWeekInputCosts(), 10l); //input costs should still now be 10
        assertEquals(localCRS.getLastWeekInputCosts(), 0l);

        //I'll give you another and force you to step again
        localCRS.getOwner().receive(new Good(GoodType.GENERIC,drs.getOwner(),10l),drs.getOwner());
        localCRS.step(localCRS.getModel());
        assertEquals(localCRS.getThisWeekInputCosts(), 20l); //input costs should still now be 20
        assertEquals(localCRS.getLastWeekInputCosts(), 0l);


        //with weekend it should reset this week and make lastweekinput costs equal 20
        localCRS.weekEnd(7);
        assertEquals(localCRS.getThisWeekInputCosts(), 0l); //reset to 0
        assertEquals(localCRS.getLastWeekInputCosts(), 20l); //now it's 20


        //try one more time
        localCRS.getOwner().receive(new Good(GoodType.GENERIC,drs.getOwner(),10l),drs.getOwner());
        localCRS.step(localCRS.getModel());
        assertEquals(localCRS.getThisWeekInputCosts(), 10l); //input costs should now be 10
        assertEquals(localCRS.getLastWeekInputCosts(), 20l); //this should be stuck at 20


        //new weekend, forget 20$ and reset this week
        localCRS.weekEnd(14);
        assertEquals(localCRS.getThisWeekInputCosts(), 0l); //reset to 0
        assertEquals(localCRS.getLastWeekInputCosts(), 10l); //now it's 10

        //another weekend, everything is at 0
        localCRS.weekEnd(0);
        assertEquals(localCRS.getThisWeekInputCosts(), 0l); //reset to 0
        assertEquals(localCRS.getLastWeekInputCosts(), 0l); //now it's 10







    }
}
