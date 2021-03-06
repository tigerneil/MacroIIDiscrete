/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package agents.firm.sales.pricing;

import agents.firm.sales.SalesDepartment;
import goods.Good;
import model.MacroII;
import model.utilities.ActionOrder;
import sim.engine.SimState;
import sim.engine.Steppable;

/**
 * <h4>Description</h4>
 * <p/>  Periodically checks its opponent. He sets all the price of its good to be 1% lower than its best opponent (but never below the cost of the good).
 * <p/>
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
public class UndercuttingAskPricing extends BaseAskPricingStrategy implements Steppable{

    /**
     * By how much to reduce the best offer made by your opponent?
     */
    private float percentageToUndercut;

    /**
     * The sales department using this strategy
     */
    private final SalesDepartment sales;

    /**
     * The best price found by the opponent
     */
    private long bestOpponentPriceFound = -1;


    /**
     * As long as the strategy is on, it keeps stepping itself. Otherwise the next adjust will be the last. This way the strategy doesn't keep living in the schedule
     */
    boolean strategyActivated = true;

    /**
     *  Periodically checks its opponent.
     *  He sets all the price of its good to be x% lower than its best opponent (but never below the cost of the good).
     * @param sales the sales department using this strategy
     */
    public UndercuttingAskPricing(SalesDepartment sales) {
        this.sales = sales;
        percentageToUndercut = sales.getFirm().getModel().drawNewUndercutReduction(sales.getFirm(),sales.getMarket());
        getSales().getModel().scheduleSoon(ActionOrder.ADJUST_PRICES,this);


    }


    /**
     * The sales department is asked at what should be the sale price for a specific good; this, I guess, is the fundamental
     * part of the sales department
     *
     *
     * @param g the good to price
     * @return the price given to that good
     */
    @Override
    public int price(Good g) {
        if(bestOpponentPriceFound == -1){
            //if we didn't find a single opponent, just return the cost
            return (int) (g.getLastValidPrice() * (1f + sales.getFirm().getModel().getCluelessDefaultMarkup()));
        }
        else{

            long undercutPrice = (long) ((1f-percentageToUndercut) *bestOpponentPriceFound );
            //sometimes rounding makes it so that the price stays the same. If so, try to undercut
            if(undercutPrice == bestOpponentPriceFound && undercutPrice > 1)
                undercutPrice--;

            return (int) Math.max(undercutPrice,g.getLastValidPrice()); //return the undercut price or the cost, whichever is higher
        }



    }

    /**
     * When the undercutting pricing strategy steps it looks for a new price to undercut
     * @param simState the model
     */
    @Override
    public void step(SimState simState) {
        if(strategyActivated){
            long oldBestPrice = bestOpponentPriceFound; //keep it in memory
            bestOpponentPriceFound = sales.getLowestOpponentPrice(); //look for someone better
            if(bestOpponentPriceFound != oldBestPrice) //if we changed
                sales.updateQuotes(); //update quotes!

            ((MacroII)simState).scheduleTomorrow(ActionOrder.ADJUST_PRICES,this);


        }

    }


    /**
     * When the pricing strategy is changed or the firm is shutdown this is called. It's useful to kill off steppables and so on
     */
    @Override
    public void turnOff() {

        super.turnOff();
        strategyActivated = false;
    }

    /**
     * Undercutting doesn't care about weekend.
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

    public long getBestOpponentPriceFound() {
        return bestOpponentPriceFound;
    }

    public SalesDepartment getSales() {
        return sales;
    }

    /**
     * All inventory is unwanted
     */
    @Override
    public float estimateSupplyGap() {
        return sales.getHowManyToSell();
    }
}
