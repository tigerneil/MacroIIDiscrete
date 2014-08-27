/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package agents.firm.purchases.prediction;

import agents.firm.purchases.PurchasesDepartment;
import agents.firm.sales.prediction.RegressionDataCollector;
import agents.firm.sales.prediction.SISOGuessingPredictorBase;
import model.MacroII;
import model.utilities.stats.collectors.enums.PurchasesDataType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * The purchases alter-ego of the SISO GuessingSales Predictor
 * Created by carrknight on 8/27/14.
 */
public class SISOGuessingPurchasesPredictor implements PurchasesPredictor  {



    private final RegressionDataCollector<PurchasesDataType> collector;

    /**
     * the sales department to use
     */
    private final PurchasesDepartment toFollow;

    /**
     * the set of regressions to use
     */
    private final SISOGuessingPredictorBase<PurchasesDataType> regression;


    public SISOGuessingPurchasesPredictor(MacroII model, PurchasesDepartment toFollow) {
        this.toFollow = toFollow;
        //guesstimate
        PurchasesDataType xType = PurchasesDataType.WORKERS_CONSUMING_THIS_GOOD;
        collector = new RegressionDataCollector<>(toFollow,xType,PurchasesDataType.CLOSING_PRICES,
                PurchasesDataType.DEMAND_GAP);
        collector.setDataValidator(collector.getDataValidator().and(dep-> dep.hasTradedAtLeastOnce()));
        collector.setyValidator(price-> Double.isFinite(price) && price > 0); // we don't want -1 prices
        regression = new SISOGuessingPredictorBase<>(model,collector);
        try {
            regression.setDebugWriter(Paths.get("runs","tmp2.csv"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Predicts the future price of the next good to buy
     *
     * @param dept the department that needs to buy it
     * @return the predicted price or -1 if there are no predictions.
     */
    @Override
    public float predictPurchasePriceWhenIncreasingProduction(PurchasesDepartment dept) {
        return predictYAfterChangingX(1);
    }

    private float predictYAfterChangingX(int increaseStep) {
        final float predicted = regression.predictYAfterChangingXBy(increaseStep);
        if(Float.isFinite(predicted))
            return predicted;
        else return (float) toFollow.getAveragedClosingPrice();
    }


    /**
     * Predicts the future price of the next good to buy
     *
     * @param dept the department that needs to buy it
     * @return the predicted price or -1 if there are no predictions.
     */
    @Override
    public float predictPurchasePriceWhenDecreasingProduction(PurchasesDepartment dept) {
        return predictYAfterChangingX(-1);
    }

    /**
     * Predicts the future price of the next good to buy
     *
     * @param dept the department that needs to buy it
     * @return the predicted price or -1 if there are no predictions.
     */
    @Override
    public float predictPurchasePriceWhenNoChangeInProduction(PurchasesDepartment dept) {
        return predictYAfterChangingX(0);
    }

    /**
     * Call this to kill the predictor
     */
    @Override
    public void turnOff() {
        regression.turnOff();
    }

    public void setDebugWriter(Path pathToDebugFileToWrite) throws IOException {
        regression.setDebugWriter(pathToDebugFileToWrite);
    }
}
