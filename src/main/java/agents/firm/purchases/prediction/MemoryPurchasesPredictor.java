/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package agents.firm.purchases.prediction;

import agents.firm.purchases.PurchasesDepartment;

/**
 * <h4>Description</h4>
 * <p/> This predictor simply returns the last price the purchases department managed to get a good for
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author Ernesto
 * @version 2012-10-04
 * @see
 */
public class MemoryPurchasesPredictor implements PurchasesPredictor {
    /**
     * Predicts the future price of the next good to buy
     *
     * @param dept the department that needs to buy it
     * @return the predicted price or -1 if there are no predictions.
     */
    @Override
    public float predictPurchasePriceWhenIncreasingProduction(PurchasesDepartment dept) {
        return dept.getLastClosingPrice();
    }

    @Override
    public float predictPurchasePriceWhenDecreasingProduction(PurchasesDepartment dept) {
        return dept.getLastClosingPrice();
    }
    /**
     * Predicts the last closing price
     *
     * @param dept the department that needs to buy it
     * @return the predicted price or -1 if there are no predictions.
     */
    @Override
    public float predictPurchasePriceWhenNoChangeInProduction(PurchasesDepartment dept) {
        return dept.getLastClosingPrice();
    }

    /**
     * Call this to kill the predictor
     */
    @Override
    public void turnOff() {
    }
}
