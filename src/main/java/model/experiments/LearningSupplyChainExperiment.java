/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.experiments;

import agents.firm.Firm;
import agents.firm.personell.HumanResources;
import agents.firm.production.Blueprint;
import agents.firm.production.control.maximizer.algorithms.marginalMaximizers.MarginalMaximizer;
import agents.firm.purchases.PurchasesDepartment;
import agents.firm.purchases.prediction.FixedIncreasePurchasesPredictor;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.SalesDepartmentOneAtATime;
import agents.firm.sales.prediction.FixedDecreaseSalesPredictor;
import agents.firm.sales.prediction.SalesPredictor;
import au.com.bytecode.opencsv.CSVWriter;
import financial.market.Market;
import goods.GoodType;
import model.MacroII;
import model.scenario.OneLinkSupplyChainScenarioWithCheatingBuyingPrice;
import model.utilities.stats.collectors.DailyStatCollector;
import model.utilities.stats.collectors.enums.MarketDataType;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import static model.experiments.tuningRuns.MarginalMaximizerPIDTuning.printProgressBar;

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
 * @version 2014-01-21
 * @see
 */
public class LearningSupplyChainExperiment {


    public static void main(String[] args){
        monopolist(true);
    }

    public static void monopolist(final boolean learned)
    {
        final MacroII macroII = new MacroII(1l);
        final SalesDepartment[] outerDepartment = new SalesDepartment[1];

        final OneLinkSupplyChainScenarioWithCheatingBuyingPrice scenario1= new OneLinkSupplyChainScenarioWithCheatingBuyingPrice(macroII){


                @Override
                protected void buildBeefSalesPredictor(SalesDepartment dept) {
                    if(learned)
                    {
                        FixedDecreaseSalesPredictor predictor  = SalesPredictor.Factory.newSalesPredictor(FixedDecreaseSalesPredictor.class, dept);
                        predictor.setDecrementDelta(2);
                        dept.setPredictorStrategy(predictor);
                    }
                }



                @Override
                public void buildFoodPurchasesPredictor(PurchasesDepartment department) {
                    department.setPredictor(new FixedIncreasePurchasesPredictor(0));

                }

                @Override
                protected SalesDepartment createSalesDepartment(Firm firm, Market goodmarket) {
                    SalesDepartment department = super.createSalesDepartment(firm, goodmarket);
                    if(goodmarket.getGoodType().equals(GoodType.FOOD))
                        department.setPredictorStrategy(new FixedDecreaseSalesPredictor(0));
                    else
                        outerDepartment[0] = department;
                    return department;
                }

                @Override
                protected HumanResources createPlant(Blueprint blueprint, Firm firm, Market laborMarket) {
                    HumanResources hr = super.createPlant(blueprint, firm, laborMarket);
                    if(blueprint.getOutputs().containsKey(GoodType.BEEF))
                        if(learned){
                        hr.setPredictor(new FixedIncreasePurchasesPredictor(1));
                        }
                    if(blueprint.getOutputs().containsKey(GoodType.FOOD))
                        hr.setPredictor(new FixedIncreasePurchasesPredictor(0));
                    return hr;
                }

            };

        scenario1.setControlType(MarginalMaximizer.class);
        scenario1.setSalesDepartmentType(SalesDepartmentOneAtATime.class);
        scenario1.setBeefPriceFilterer(null);

        //competition!
        scenario1.setNumberOfBeefProducers(1);
        scenario1.setNumberOfFoodProducers(5);

        scenario1.setDivideProportionalGainByThis(100f);
        scenario1.setDivideIntegrativeGainByThis(100f);
        //no delay
        scenario1.setBeefPricingSpeed(0);

        try {
            CSVWriter writer = new CSVWriter(new FileWriter("runs/supplychai/"+"beefshouldlearn"+".csv"));
            DailyStatCollector collector = new DailyStatCollector(macroII,writer);
            collector.start();


        } catch (IOException e) {
            System.err.println("failed to create the file!");
        }



        macroII.setScenario(scenario1);
        macroII.start();


        while(macroII.schedule.getTime()<14000)
        {
            macroII.schedule.step(macroII);
            printProgressBar(14001,(int)macroII.schedule.getSteps(),100);
        }


        float averageFoodPrice = 0;
        float averageBeefProduced = 0;
        float averageBeefPrice=0;
        for(int j=0; j< 1000; j++)
        {
            //make the model run one more day:
            macroII.schedule.step(macroII);
            averageFoodPrice += macroII.getMarket(GoodType.FOOD).getLatestObservation(MarketDataType.AVERAGE_CLOSING_PRICE);
            averageBeefProduced+= macroII.getMarket(GoodType.BEEF).getYesterdayVolume();
            averageBeefPrice+= macroII.getMarket(GoodType.BEEF).getLatestObservation(MarketDataType.AVERAGE_CLOSING_PRICE);
        }


        outerDepartment[0].getData().writeToCSVFile(Paths.get("runs","supplychai","supplySales.csv").toFile());

        System.out.println("beef price: " +averageBeefPrice/1000f );
        System.out.println("food price: " +averageFoodPrice/1000f );
        System.out.println("produced: " +averageBeefProduced/1000f );
        System.out.println(); System.out.flush();


    }
}