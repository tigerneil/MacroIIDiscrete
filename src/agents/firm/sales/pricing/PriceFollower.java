package agents.firm.sales.pricing;

import agents.firm.sales.SalesDepartment;
import goods.Good;

/**
 * <h4>Description</h4>
 * <p/> This strategy copies the last price in the market whenever possible.
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author Ernesto
 * @version 2012-07-23
 * @see
 */
public class PriceFollower implements AskPricingStrategy {


    SalesDepartment sales;

    /**
     * This strategy copies the last price in the market whenever possible.
     */
    public PriceFollower(SalesDepartment sales) {
        this.sales = sales;
    }

    /**
     * Copies the last closing price of the market, if available.
     *
     * @param g the good to price
     * @return the price given to that good
     */
    @Override
    public long price(Good g) {
        long priceToFollow =sales.getMarket().getLastPrice();
        if(priceToFollow == -1)
            //if you know nothing, just act cluelessly
            return (long) (g.getLastValidPrice() * (1f + sales.getFirm().getModel().getCluelessDefaultMarkup()));
        else
            //if you you can copy from the last price
            return Math.max(priceToFollow,g.getLastValidPrice());
    }

    /**
     * When the pricing strategy is changed or the firm is shutdown this is called. It's useful to kill off steppables and so on
     */
    @Override
    public void turnOff() {
    }

    /**
     * After computing all statistics the sales department calls the weekEnd method. This might come in handy
     */
    @Override
    public void weekEnd() {
    }
}