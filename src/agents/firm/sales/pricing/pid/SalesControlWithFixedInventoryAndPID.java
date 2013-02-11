package agents.firm.sales.pricing.pid;

import agents.firm.sales.SalesDepartment;
import agents.firm.sales.pricing.AskPricingStrategy;
import com.google.common.base.Preconditions;
import goods.Good;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.pid.*;
import sim.engine.SimState;
import sim.engine.Steppable;

/**
 * <h4>Description</h4>
 * <p/> This is a very generic sales department control that doesn't count stockouts but rather just tinkers with the price
 * to keep a fixed inventory level above 0.
 * <p/> By default this level is 5. But it can be set at any point during runtime. "Fixed inventory" refers to the fact that
 * the control itself doesn't change its targets, but any external object can set it
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-02-06
 * @see
 */
public class SalesControlWithFixedInventoryAndPID implements AskPricingStrategy, Steppable
{
    //todo add this as a simpleSeller test scenario


    /**
     * how much inventory is correct to have. Any number below this should trigger the PID to raise prices, any number above this to lower them
     */
    private int targetInventory = 5;

    /**
     * A controller to change prices as inventory changes. By default it's a cascade control
     */
    private final Controller controller;


    /**
     * the sales department linked controlled by the object
     */
    private final SalesDepartment department;

    public SalesControlWithFixedInventoryAndPID(SalesDepartment department) {

        this(department,5); //default to 5 units of inventory
    }

    /**
     * The sales department with an initial target inventory and a PID controller
     * @param department the sales department that is controlled by this strategy
     * @param targetInventory the inventory to target
     */
    public SalesControlWithFixedInventoryAndPID(SalesDepartment department, int targetInventory)
    {
        this(department,targetInventory, PIDController.class);

    }

    /**
     * The full constructor with Sales department
     * @param department the sales department to link to
     * @param targetInventory the target inventory of this control
     * @param controllerType the type of controller to use
     */
    public SalesControlWithFixedInventoryAndPID(SalesDepartment department,int targetInventory,
                                                Class<? extends Controller> controllerType )
    {
        this.department = department;
        this.controller = ControllerFactory.buildController(controllerType,department.getFirm().getModel());
        this.targetInventory = targetInventory;

        //schedule yourself when possible
        department.getFirm().getModel().scheduleSoon(ActionOrder.THINK,this);


    }

    /**
     * The sales department is asked at what should be the sale price for a specific good; this, I guess, is the fundamental
     * part of the sales department
     *
     * @param g the good to price
     * @return the price given to that good
     */
    @Override
    public long price(Good g) {
        return (long)Math.round(controller.getCurrentMV());
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

    /**
     * this step manages the controller. It runs every THINK phase
     * @param state the MacroII object
     */
    @Override
    public void step(SimState state)
    {
        MacroII macroII = (MacroII) state;
        Preconditions.checkState(macroII.getCurrentPhase().equals(ActionOrder.THINK), "I wanted to act on THINK, " +
                "but I acted on " + macroII.getCurrentPhase());
        //run the controller to adjust prices
        //notice that i use todayOutflow as a measure of outflow because it gets resed at PREPARE_TO_TRADE and
        // I am calling this at THINK (which is after TRADE, which is after PREPARE_TO_TRADE)
        ControllerInput input = new ControllerInput.ControllerInputBuilder().inputs((float) department.getHowManyToSell(),
                (float)department.getTodayOutflow()).targets((float)targetInventory, (float)department.getTodayInflow()).build();

        controller.adjust(input,
                department.getFirm().isActive(), macroII, this, ActionOrder.THINK);


    }


    /**
     * Gets how much inventory is correct to have. Any number below this should trigger the PID to raise prices, any number above this to lower them.
     *
     * @return Value of how much inventory is correct to have. Any number below this should trigger the PID to raise prices, any number above this to lower them.
     */
    public int getTargetInventory() {
        return targetInventory;
    }

    /**
     * Sets new how much inventory is correct to have. Any number below this should trigger the PID to raise prices, any number above this to lower them.
     *
     * @param targetInventory New value of how much inventory is correct to have. Any number below this should trigger the PID to raise prices, any number above this to lower them.
     */
    public void setTargetInventory(int targetInventory) {
        this.targetInventory = targetInventory;
    }

    /**
     * Resets the PID at this new price
     * @param price the new price (can't be a negative number)
     */
    public void setInitialPrice(long price) {
        Preconditions.checkArgument(price >= 0);
        controller.setOffset(price);
    }


    /**
     * Gets the sales department linked controlled by the object.
     *
     * @return Value of the sales department linked controlled by the object.
     */
    public SalesDepartment getDepartment() {
        return department;
    }
}