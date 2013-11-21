package agents.firm.sales.prediction;

import agents.firm.purchases.prediction.AbstractOpenLoopRecursivePredictor;
import agents.firm.sales.SalesDepartment;
import model.MacroII;

/**
 * <h4>Description</h4>
 * <p/> This is basically the open-loop version of the recursive sales predictor. Instead of using the beta of the regression as soon as new information comes in, this predictor
 * uses the beta it learned (closes the loop) only every x days.
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-11-20
 * @see
 */
public class OpenLoopRecursiveSalesPredictor extends AbstractOpenLoopRecursivePredictor implements SalesPredictor
{

    private final FixedDecreaseSalesPredictor decreaseSalesPredictor;



    public OpenLoopRecursiveSalesPredictor(MacroII model, SalesDepartment department) {
        super(new RecursiveSalePredictor(model,department),model);
        decreaseSalesPredictor = new FixedDecreaseSalesPredictor(0);
    }


    /**
     * This is called by the firm when it wants to predict the price they can sell to if they increase production
     *
     * @param dept                   the sales department that has to answer this question
     * @param expectedProductionCost the HQ estimate of costs in producing whatever it wants to sell. It isn't necesarilly used.
     * @param increaseStep           by how much the daily production will increase (has to be a positive number)
     * @return the best offer available/predicted or -1 if there are no quotes/good predictions
     */
    @Override
    public long predictSalePriceAfterIncreasingProduction(SalesDepartment dept, long expectedProductionCost, int increaseStep) {
        long slope = predictPrice(increaseStep) - predictPrice(0);
        if(slope > 0)
            slope = 0;
        decreaseSalesPredictor.setDecrementDelta(-slope);
        return decreaseSalesPredictor.predictSalePriceAfterIncreasingProduction(dept, expectedProductionCost,  modifyStepIfNeeded(increaseStep));

    }

    /**
     * This is called by the firm when it wants to predict the price they can sell to if they increase production
     *
     * @param dept                   the sales department that has to answer this question
     * @param expectedProductionCost the HQ estimate of costs in producing whatever it wants to sell. It isn't necesarilly used.
     * @param decreaseStep           by how much the daily production will decrease (has to be a positive number)
     * @return the best offer available/predicted or -1 if there are no quotes/good predictions
     */
    @Override
    public long predictSalePriceAfterDecreasingProduction(SalesDepartment dept, long expectedProductionCost, int decreaseStep) {
        long slope = predictPrice(0) - predictPrice(-decreaseStep);
        if(slope > 0)
            slope = 0;
        decreaseSalesPredictor.setDecrementDelta(-slope);

        return decreaseSalesPredictor.predictSalePriceAfterDecreasingProduction(dept, expectedProductionCost, modifyStepIfNeeded(decreaseStep));
    }

    /**
     * This is a little bit weird to predict, but basically you want to know what will be "tomorrow" price if you don't change production.
     * Most predictors simply return today closing price, because maybe this will be useful in some cases. It's used by Marginal Maximizer Statics
     *
     * @param dept the sales department
     * @return predicted price
     */
    @Override
    public long predictSalePriceWhenNotChangingPoduction(SalesDepartment dept) {
        return decreaseSalesPredictor.predictSalePriceWhenNotChangingPoduction(dept);
    }
}
