/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.experiments;

import agents.EconomicAgent;
import agents.firm.Firm;
import agents.firm.personell.HumanResources;
import agents.firm.production.control.maximizer.algorithms.marginalMaximizers.MarginalMaximizerStatics;
import agents.firm.purchases.prediction.FixedIncreasePurchasesPredictor;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.SalesDepartmentOneAtATime;
import agents.firm.sales.prediction.FixedDecreaseSalesPredictor;
import agents.firm.sales.prediction.FixedFormulaFakePredictor;
import agents.firm.sales.pricing.pid.SalesControlWithFixedInventoryAndPID;
import au.com.bytecode.opencsv.CSVWriter;
import ch.qos.logback.classic.Level;
import goods.GoodType;
import model.MacroII;
import model.scenario.MonopolistScenario;
import model.scenario.TripolistScenario;
import model.utilities.stats.collectors.DailyStatCollector;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

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
 * @version 2014-01-18
 * @see
 */
public class CompetitiveScenarioVarianceTest {

    public static void main(String[] args){
        learned(args);
    }

    public static void learned(String[] args)
    {
        //make sure logger is set up

        MarginalMaximizerStatics.logger.setLevel(Level.ALL);
        assert MarginalMaximizerStatics.class.getClassLoader().getResource("/logback.xml") != null;
        int competitor = 5;
        System.out.println("FORCED COMPETITIVE FIRMS: " + (competitor+1));


        FixedDecreaseSalesPredictor.defaultDecrementDelta=0;

        final MacroII macroII = new MacroII(System.currentTimeMillis());   //1387582416533l
        final TripolistScenario scenario1 = new TripolistScenario(macroII);

        scenario1.setSalesDepartmentType(SalesDepartmentOneAtATime.class);
        scenario1.setAskPricingStrategy(SalesControlWithFixedInventoryAndPID.class);
        scenario1.setControlType(MonopolistScenario.MonopolistScenarioIntegratedControlEnum.MARGINAL_PLANT_CONTROL);
        scenario1.setAdditionalCompetitors( competitor);
        scenario1.setWorkersToBeRehiredEveryDay(true);
        scenario1.setDemandIntercept(102);


        FixedDecreaseSalesPredictor.defaultDecrementDelta=0;
        scenario1.setSalesPricePreditorStrategy(FixedDecreaseSalesPredictor.class);


        try {
            CSVWriter writer = new CSVWriter(new FileWriter("runs/"+"competitiveLearned"+".csv"));
            DailyStatCollector collector = new DailyStatCollector(macroII,writer);
            collector.start();


        } catch (IOException e) {
            System.err.println("failed to create the file!");
        }


        //assign scenario
        macroII.setScenario(scenario1);

        macroII.start();

        macroII.schedule.step(macroII);
        for(Firm firm : scenario1.getCompetitors())
        {
            for(HumanResources hr : firm.getHRs())
                hr.setPredictor(new FixedIncreasePurchasesPredictor(0));

        }


        while(macroII.schedule.getTime()<10000)
        {
            macroII.schedule.step(macroII);



        }

        float averagePrice = 0;
        float averageQ = 0;
        float averageWorkerTarget = 0;
        for(int j=0; j<500; j++)
        {
            macroII.schedule.step(macroII);
            assert !Float.isNaN(macroII.getMarket(GoodType.GENERIC).getTodayAveragePrice());
            averagePrice += macroII.getMarket(GoodType.GENERIC).getTodayAveragePrice();
            averageQ += macroII.getMarket(GoodType.GENERIC).countTodayProductionByRegisteredSellers(); //shortcut to check how much is produced rather than just traded

            //         System.out.println("---------------------------------------------------------------------");

            long predictedPrice[] = new long[macroII.getMarket(GoodType.GENERIC).getSellers().size()];
            int i=0;
            for(EconomicAgent agent : macroII.getMarket(GoodType.GENERIC).getSellers())
            {
                SalesDepartment department = ((Firm) agent).getSalesDepartment(GoodType.GENERIC);


                averageWorkerTarget+=  ((Firm) agent).getHRs().iterator().next().getWorkerTarget();

                predictedPrice[i] = department.predictSalePriceWhenNotChangingPoduction();
                i++;
            }
            System.out.println(Arrays.toString(predictedPrice));
            //System.out.println("---> total production : " + macroII.getMarket(GoodType.GENERIC).countTodayProductionByRegisteredSellers() + " , prices:: " + macroII.getMarket(GoodType.GENERIC).getLastDaysAveragePrice());


        }

        averagePrice = averagePrice/500f;
        averageQ = averageQ/500f;
        averageWorkerTarget = averageWorkerTarget/500f;
        System.out.println(averagePrice + " - " + averageQ +"/" +averageWorkerTarget+ "----" + macroII.seed() + " | " + macroII.getMarket(GoodType.GENERIC).getLastDaysAveragePrice());





        assertEquals(averagePrice, 58, 5);
        assertEquals(averageQ, 44,5);


    }

    public static void formula(String[] args)
    {
        //make sure logger is set up

        MarginalMaximizerStatics.logger.setLevel(Level.ALL);
        assert MarginalMaximizerStatics.class.getClassLoader().getResource("/logback.xml") != null;
        int competitor = 4;
        System.out.println("FORCED COMPETITIVE FIRMS: " + (competitor+1));


        FixedDecreaseSalesPredictor.defaultDecrementDelta=0;

        final MacroII macroII = new MacroII(System.currentTimeMillis());   //1387582416533l
        final TripolistScenario scenario1 = new TripolistScenario(macroII);

        scenario1.setSalesDepartmentType(SalesDepartmentOneAtATime.class);
        scenario1.setAskPricingStrategy(SalesControlWithFixedInventoryAndPID.class);
        scenario1.setControlType(MonopolistScenario.MonopolistScenarioIntegratedControlEnum.MARGINAL_PLANT_CONTROL);
        scenario1.setAdditionalCompetitors( competitor);
        scenario1.setWorkersToBeRehiredEveryDay(true);
        scenario1.setDemandIntercept(102);


        FixedDecreaseSalesPredictor.defaultDecrementDelta=0;
        scenario1.setSalesPricePreditorStrategy(FixedDecreaseSalesPredictor.class);


        try {
            CSVWriter writer = new CSVWriter(new FileWriter("runs/"+"competitiveLearned"+".csv"));
            DailyStatCollector collector = new DailyStatCollector(macroII,writer);
            collector.start();


        } catch (IOException e) {
            System.err.println("failed to create the file!");
        }


        //assign scenario
        macroII.setScenario(scenario1);

        macroII.start();

        macroII.schedule.step(macroII);
        for(Firm firm : scenario1.getCompetitors())
        {
            for(HumanResources hr : firm.getHRs())
                //hr.setPredictor(new FixedIncreasePurchasesPredictor(0));
                hr.setPredictor(new FixedFormulaFakePredictor(scenario1.getDailyWageSlope(),scenario1.getDailyWageIntercept(),hr.getMarket()));
            for(SalesDepartment dept : firm.getSalesDepartments().values())
                dept.setPredictorStrategy(new FixedFormulaFakePredictor(-scenario1.getDemandSlope(),scenario1.getDemandIntercept(),dept.getMarket()));
        }


        while(macroII.schedule.getTime()<10000)
        {
            macroII.schedule.step(macroII);



        }

        float averagePrice = 0;
        float averageQ = 0;
        float averageWorkerTarget = 0;
        for(int j=0; j<500; j++)
        {
            macroII.schedule.step(macroII);
            assert !Float.isNaN(macroII.getMarket(GoodType.GENERIC).getTodayAveragePrice());
            averagePrice += macroII.getMarket(GoodType.GENERIC).getTodayAveragePrice();
            averageQ += macroII.getMarket(GoodType.GENERIC).countTodayProductionByRegisteredSellers(); //shortcut to check how much is produced rather than just traded

            //         System.out.println("---------------------------------------------------------------------");

            long predictedPrice[] = new long[macroII.getMarket(GoodType.GENERIC).getSellers().size()];
            int i=0;
            for(EconomicAgent agent : macroII.getMarket(GoodType.GENERIC).getSellers())
            {
                SalesDepartment department = ((Firm) agent).getSalesDepartment(GoodType.GENERIC);


                averageWorkerTarget+=  ((Firm) agent).getHRs().iterator().next().getWorkerTarget();

                predictedPrice[i] = ((Firm) agent).getHRs().iterator().next().predictPurchasePriceWhenNoChangeInProduction();
                i++;
            }
            System.out.println(Arrays.toString(predictedPrice));
            //System.out.println("---> total production : " + macroII.getMarket(GoodType.GENERIC).countTodayProductionByRegisteredSellers() + " , prices:: " + macroII.getMarket(GoodType.GENERIC).getLastDaysAveragePrice());


        }

        averagePrice = averagePrice/500f;
        averageQ = averageQ/500f;
        averageWorkerTarget = averageWorkerTarget/500f;
        System.out.println(averagePrice + " - " + averageQ +"/" +averageWorkerTarget+ "----" + macroII.seed() + " | " + macroII.getMarket(GoodType.GENERIC).getLastDaysAveragePrice());





        assertEquals(averagePrice, 58, 5);
        assertEquals(averageQ, 44,5);


    }
}
