/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package agents.firm.production.control.facades;

import agents.firm.personell.HumanResources;
import agents.firm.production.Plant;
import agents.firm.production.control.PlantControl;
import agents.firm.production.control.TargetAndMaximizePlantControl;
import agents.firm.production.control.decorators.MatchBestControlDecorator;
import agents.firm.production.control.maximizer.SetTargetThenTryAgainMaximizer;
import agents.firm.production.control.maximizer.algorithms.hillClimbers.AlwaysMovingHillClimber;
import agents.firm.production.control.targeter.PIDTargeterWithQuickFiring;
import agents.firm.production.technology.Machinery;
import agents.firm.purchases.inventoryControl.Level;
import goods.Good;
import goods.GoodType;


/**
 * <h4>Description</h4>
 * <p/> This is like DiscreteMatcherPlantControl but added is the MatchBestControl decorator!
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author Ernesto
 * @version 2012-10-09
 * @see
 */
public class DiscreteMatcherPlantControl  implements PlantControl
{


    private  final PlantControl control;

    /**
     * Creates a TargetAndMaximizePlantControl with PIDTargeterWithQuickFiring and HillClimber
     * @param hr
     */
    public DiscreteMatcherPlantControl( HumanResources hr) {
        //instantiate the real control
        control = TargetAndMaximizePlantControl.PlantControlFactory(hr,
                PIDTargeterWithQuickFiring.class, SetTargetThenTryAgainMaximizer.class,
                AlwaysMovingHillClimber.class, MatchBestControlDecorator.class);

    }

    /**
     * Answer the question: how much am I willing to pay for this kind of labor?
     * Notice that NO UPDATING SHOULD TAKE PLACE in calling this method. Human Resources expects maxPrice() to be consistent from one call to the next.
     * To notify hr of inconsistencies call updateEmployeeWages(). <br>
     * In short,for plant control, <b>this should be a simple getter.</b>. If you are a subclass and want to change wages, use the current wage setter.
     *
     * @param type the type of good you want to buy
     * @return the maximum price I am willing to pay for this good
     */
    @Override
    public int maxPrice(GoodType type) {
        return control.maxPrice(type);
    }



    /**
     * This is somewhat similar to rate current level. It estimates the excess (or shortage)of goods purchased. It is basically
     * getCurrentInventory-AcceptableInventory
     *
     * @return positive if there is an excess of goods bought, negative if there is a shortage, 0 if you are right on target.
     */
    @Override
    public int estimateDemandGap() {
        return control.estimateDemandGap();
    }

    /**
     * The controller sets wages for everybody. Probably only used by subcomponents
     * @param newWage the new wage
     */
    @Override
    public void setCurrentWage(int newWage) {
        control.setCurrentWage(newWage);
    }

    /**
     * Generic getter to know the human resources objects associated with the control
     */
    @Override
    public HumanResources getHr() {
        return control.getHr();
    }

    /**
     * get workforce size targeted
     */
    @Override
    public int getTarget() {
        return control.getTarget();
    }

    /**
     * This method returns the control rating on current stock held <br>
     * @return the rating on the current stock conditions or null if the department is not active.
     */
    @Override

    public Level rateCurrentLevel() {
        return control.rateCurrentLevel();
    }

    /**
     * Call this if we change/remove the control to stop it from giving more orders.Turn off is irreversible
     */
    @Override
    public void turnOff() {
        control.turnOff();
    }

    /**
     * This is used by the the user to ask the control whether or not to act.<br>
     * @return
     */
    @Override
    public boolean canBuy() {
        return control.canBuy();
    }

    /**
     * When instantiated the control doesn't move until it receives a stimulus OR start() is called. With this function you can start it immediately.
     * Notice: THIS DOESN'T ACTIVATE TURNEDOFF controls. Turn off is irreversible
     */
    @Override
    public void start() {
        control.start();
    }

    /**
     * Answer the purchase strategy question: how much am I willing to pay for this specific good?
     * @param good the specific good being offered to you
     * @return the maximum price I am willing to pay for this good
     */
    @Override
    public int maxPrice(Good good) {
        return control.maxPrice(good);
    }


    /**
     * set the workforce size target
     */
    @Override
    public void setTarget(int workSize) {
        control.setTarget(workSize);
    }

    /**
     * This is called whenever a plant has changed the number of workers
     *
     * @param p          the plant that made the change
     * @param workerSizeNow the new number of workers
     * @param workerSizeBefore
     */
    @Override
    public void changeInWorkforceEvent(Plant p, int workerSizeNow, int workerSizeBefore) {
        control.changeInWorkforceEvent(p, workerSizeNow, workerSizeBefore);
    }

    /**
     * This is called whenever a plant has changed the wage it pays to workers
     *
     * @param wage       the new wage
     * @param p          the plant that made the change
     * @param workerSize the new number of workers
     */
    @Override
    public void changeInWageEvent(Plant p, int workerSize, long wage) {
        control.changeInWageEvent(p, workerSize, wage);
    }

    /**
     * This is called whenever a plant has been shut down or just went obsolete
     *
     * @param p the plant that made the change
     */
    @Override
    public void plantShutdownEvent(Plant p) {
        control.plantShutdownEvent(p);
    }

    /**
     * This is called by the plant whenever the machinery used has been changed
     *
     * @param p         The plant p
     * @param machinery the machinery used.
     */
    @Override
    public void changeInMachineryEvent(Plant p, Machinery machinery) {
        control.changeInMachineryEvent(p, machinery);
    }


    /**
     * Set whether or not the control can buy
     */
    @Override
    public void setCanBuy(boolean canBuy) {
        control.setCanBuy(canBuy);
    }

    /**
     * Get the current wages paid by the control
     */
    @Override
    public long getCurrentWage() {
        return control.getCurrentWage();
    }
}
