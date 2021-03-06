/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package agents.firm.purchases.pid;

import agents.HasInventory;
import agents.firm.purchases.PurchasesDepartment;
import agents.firm.purchases.inventoryControl.DailyInventoryControl;
import agents.firm.purchases.pricing.BidPricingStrategy;
import financial.MarketEvents;
import goods.Good;
import goods.GoodType;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.pid.*;
import sim.engine.SimState;
import sim.engine.Steppable;


/**
 * <h4>Description</h4>
 * <p/> This is a controller class that doubles as both a pricing and an inventory control strategy for purchases department. This implementation retrofit DailyInventoryControl by extending it.
 * <p/>  It steps independently until receiving a turnoff signal. In the adjust process it adjust through its controller nature
 * <p/> This particular implementation uses the standard formula in spite of the fact that the observations are discrete
 * <h4>Notes</h4>
 * It's not drawable at random since the rule generator wouldn't know how to set it for both pricing and inventory control
 *  * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author Ernesto
 * @version 2012-08-12
 * @see Steppable
 */
public class PurchasesDailyPID extends DailyInventoryControl implements BidPricingStrategy, Steppable {


    /**
     * The controller used to choose prices
     */
    private CascadePToPIDController controller;

    /**
     * This is the standard constructor needed to generate at random this strategy.
     * @param purchasesDepartment the department controlled by this strategy
     */
    public PurchasesDailyPID( PurchasesDepartment purchasesDepartment) {
        super(purchasesDepartment);
        controller = ControllerFactory.buildController(CascadePToPIDController.class,purchasesDepartment.getModel());
        //if you have a pid controller, play a bit with it
        //parameters found through genetic algorithm

        controller.setMasterProportionalGain(0.035f);

        float proportionalGain = 0.04954876f + ((float) purchasesDepartment.getRandom().nextGaussian()) / 100f;
        float integralGain = 0.45825003f + ((float) purchasesDepartment.getRandom().nextGaussian()) / 100f;
        float derivativeGain = 0.000708338f + ((float) purchasesDepartment.getRandom().nextGaussian() / 10000f);
        controller.setGainsSlavePID(proportionalGain,
                integralGain,
                derivativeGain);

    }




    public PurchasesDailyPID( PurchasesDepartment purchasesDepartment, float proportionalGain, float integralGain,
                             float derivativeGain) {
        super(purchasesDepartment);
        controller = ControllerFactory.buildController(CascadePToPIDController.class,purchasesDepartment.getModel());
        //if you have a pid controller, play a bit with it
        controller.setGainsSlavePID(proportionalGain, integralGain, derivativeGain);

    }


    /**
     * The adjust is the main part of the controller controller. It checks the new error and set the MV (which is the price, really)
     * @param simState MacroII object if I am worth anything as a programmer
     */
    @Override
    public void step(SimState simState) {

        long oldprice = maxPrice(getGoodTypeToControl());
        ControllerInput controllerInput = getControllerInput(getDailyTarget());
        controller.adjust(controllerInput, isActive(),(MacroII) simState, this, ActionOrder.ADJUST_PRICES);
        long newprice = maxPrice(getGoodTypeToControl());

        //log the change in policy
        if(MacroII.hasGUI())

            getPurchasesDepartment().getFirm().logEvent(getPurchasesDepartment(),
                MarketEvents.CHANGE_IN_POLICY,
                getPurchasesDepartment().getFirm().getModel().getCurrentSimulationTimeInMillis(),
                "target: " + getDailyTarget() + ", inventory:" + getPurchasesDepartment().getFirm().hasHowMany(getGoodTypeToControl()) +
                        "; oldprice:" + oldprice + ", newprice:" + newprice);


        if(oldprice != newprice && newprice >=0) //if pid says to change prices, change prices
            getPurchasesDepartment().updateOfferPrices();

    }


    /**
     * Answer the purchase strategy question: how much am I willing to pay for a good of this type?
     *
     * @param type the type of good you want to buy
     * @return the maximum price I am willing to pay for this good
     */
    @Override
    public int maxPrice(GoodType type) {
        return Math.round(controller.getCurrentMV());
    }

    /**
     * Answer the purchase strategy question: how much am I willing to pay for this specific good?
     *
     * @param good the specific good being offered to you
     * @return the maximum price I am willing to pay for this good
     */
    @Override
    public int maxPrice(Good good) {
        return maxPrice(good.getType());
    }

    /**
     * This controller strategy is always buying. It is using prices to control its inventory
     * @return true
     */
    @Override
    public boolean canBuy() {
        return super.canBuy();

    }

    /**
     * This is the method overriden by the subclass of inventory control to decide whether the current inventory levels call for a new good being bought
     * @param source   The firm
     * @param type     The goodtype associated with this inventory control
     * @param quantity the new inventory level
     * @return true if we need to buy one more good
     */
    @Override
    protected boolean shouldIBuy(HasInventory source, GoodType type, int quantity) {
        return super.shouldIBuy(source,type,quantity);
    }

    /**
     * Calls super turnoff to kill all listeners and then set active to false
     * */
    @Override
    public void turnOff() {
        super.turnOff();
        assert !isActive();
    }

    /**
     * Whenever set the controller is reset
     */
    public void setInitialPrice(float initialPrice) {
        controller.setOffset(initialPrice, true);
    }

    public float getInitialPrice() {
        return controller.getOffset();
    }

    /**
     * When instantiated the inventory control doesn't move until it receives the first good. With this function you can start it immediately.
     * Notice: THIS DOESN'T ACTIVATE TURNEDOFF controls. Turn off is irreversible                      <br>
     * In addition the controller methods adjust their controller once.
     */
    @Override
    public void start() {
        getPurchasesDepartment().getFirm().getModel().scheduleSoon(ActionOrder.ADJUST_PRICES,
                this);
          /*      new Steppable() {
                    @Override
                    public void step(SimState state) {
                        controller.adjust(getControllerInput(getDailyTarget()),
                                isActive(),
                                getRandomPurchaseDepartment().getFirm().getModel(), PurchasesDailyPID.this, ActionOrder.THINK);
                    }
                });
            */
        super.start();
    }
}
