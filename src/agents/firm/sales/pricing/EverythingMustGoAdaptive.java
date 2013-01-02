package agents.firm.sales.pricing;

import agents.firm.sales.SalesDepartment;
import goods.Good;
import model.MacroII;
import sim.engine.SimState;
import sim.engine.Steppable;

/**
 * <h4>Description</h4>
 * <p/> This strategy starts with default markup and then keeps adjusting downward whenever it fails to sell ALL its goods and upwards otherwise.
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author Ernesto
 * @version 2012-07-24
 * @see
 */
public class EverythingMustGoAdaptive implements AskPricingStrategy {

    float markup;

    SalesDepartment department;


    public EverythingMustGoAdaptive(SalesDepartment department) {
        this.department = department;
        markup = department.getFirm().getModel().getCluelessDefaultMarkup(); //set initial markup!
    }

    /**
     * The sales department is asked at what should be the sale price for a specific good; this, I guess, is the fundamental
     * part of the sales department
     *
     * @param g the good to price
     * @return
     */
    @Override
    public long price(Good g) {
        return (long) (g.getLastValidPrice() * (1+ markup));
    }

    /**
     * When the pricing strategy is changed or the firm is shutdown this is called. It's useful to kill off steppables and so on
     */
    @Override
    public void turnOff() {
    }

    /**
     * if the firm managed to sell 95% or more of its merchandise, then raise markup. Otherwise decrease it
     */
    @Override
    public void weekEnd() {
        float successRate = department.getSoldPercentage();
        MacroII model = department.getFirm().getModel();
        float oldMarkup = markup; //memorize old markup
        if(successRate > model.getMinSuccessRate())
            markup = markup + model.getMarkupIncreases();
        else
            markup = Math.max(0,markup - model.getMarkupIncreases());
        //if markup has changed, adjust your sales department to change prices soon
        if(Math.abs(markup - oldMarkup) > .001)
            model.schedule.scheduleOnce(new Steppable() { //I am not doing this IMMEDIATELY because I don't want to screw up other people's weekend routine
                @Override
                public void step(SimState simState) {
                    department.updateQuotes(); //update quotes (becaue the price is going to be different!)


                }
            });

    }
}
