/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package agents.firm.sales.pricing;

import agents.firm.sales.SalesDepartment;
import goods.Good;

/**
 * <h4>Description</h4>
 * <p/> This is a sales department that always chooses to charge its prices equal to the cost of the good trying to sell
 * <p/> If we couple this with a costing function that counts only marginal costs then we are going to copy perfectly competitive behavior from econ
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author Ernesto
 * @version 2012-07-22
 * @see
 */
public class CostAskPricing extends BaseAskPricingStrategy {

    private final SalesDepartment sales;

    public CostAskPricing(SalesDepartment sales) {
        this.sales = sales;
    }

    @Override
    public int price(Good g) {
        return g.getLastValidPrice(); //the good is going to be either the price of production or just the price for which  the good was bought

    }


    /**
     * Week end doesn't bother the cost pricing strategy
     */
    @Override
    public void weekEnd() {
    }

    /**
     * tries to sell everything
     *
     * @param inventorySize
     * @return
     */
    @Override
    public boolean isInventoryAcceptable(int inventorySize) {
        return inventorySize == 0; //tries to sell everything

    }

    /**
     * All inventory is unwanted
     */
    @Override
    public float estimateSupplyGap() {
       return sales.getHowManyToSell();
    }
}
