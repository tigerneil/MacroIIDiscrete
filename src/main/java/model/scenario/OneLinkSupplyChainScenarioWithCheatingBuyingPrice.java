package model.scenario;

import agents.firm.Firm;
import agents.firm.production.Blueprint;
import agents.firm.production.control.maximizer.algorithms.marginalMaximizers.RobustMarginalMaximizer;
import agents.firm.purchases.FactoryProducedPurchaseDepartment;
import agents.firm.purchases.PurchasesDepartment;
import agents.firm.purchases.inventoryControl.FixedInventoryControl;
import agents.firm.purchases.pricing.CheaterPricing;
import agents.firm.sales.SalesDepartmentOneAtATime;
import agents.firm.sales.exploration.BuyerSearchAlgorithm;
import agents.firm.sales.exploration.SellerSearchAlgorithm;
import au.com.bytecode.opencsv.CSVWriter;
import goods.GoodType;
import model.MacroII;
import model.utilities.stats.collectors.DailyStatCollector;

import java.io.FileWriter;
import java.io.IOException;

import static model.experiments.tuningRuns.MarginalMaximizerPIDTuning.printProgressBar;

/**
 * <h4>Description</h4>
 * <p/> This is just like one link supply chain but the purchase department looks at the price rather than using PID.
 * In a way, only the sellers are price-makers, the buyers are price-takers!
 *
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-06-10
 * @see
 */
public class OneLinkSupplyChainScenarioWithCheatingBuyingPrice extends OneLinkSupplyChainScenario {
    public OneLinkSupplyChainScenarioWithCheatingBuyingPrice(MacroII model) {
        super(model);
    }


    @Override
    protected PurchasesDepartment createPurchaseDepartment(Blueprint blueprint, Firm firm) {

        PurchasesDepartment department = null;
        for(GoodType input : blueprint.getInputs().keySet()){
            FactoryProducedPurchaseDepartment<FixedInventoryControl,CheaterPricing,BuyerSearchAlgorithm,SellerSearchAlgorithm>
                    factoryProducedPurchaseDepartment =
                    PurchasesDepartment.getPurchasesDepartment(Long.MAX_VALUE, firm, getMarkets().get(input), FixedInventoryControl.class,
                            CheaterPricing.class, null, null);

            /*
             FactoryProducedPurchaseDepartment<PurchasesFixedPID,PurchasesFixedPID,BuyerSearchAlgorithm,SellerSearchAlgorithm>
                    factoryProducedPurchaseDepartment =
                    PurchasesDepartment.getPurchasesDepartmentIntegrated(Long.MAX_VALUE,firm,getMarkets().get(input),PurchasesFixedPID.class,
                           null,null);
             */

            factoryProducedPurchaseDepartment.getInventoryControl().setInventoryTarget(100);
            factoryProducedPurchaseDepartment.getInventoryControl().setHowManyTimesOverInventoryHasToBeOverTargetToBeTooMuch(2f);


            department = factoryProducedPurchaseDepartment.getDepartment();
            firm.registerPurchasesDepartment(department, input);

            if(input.equals(GoodType.BEEF))
                buildFoodPurchasesPredictor(department);


        }
        return department;

    }








}
