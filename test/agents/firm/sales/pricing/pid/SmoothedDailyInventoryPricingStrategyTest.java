package agents.firm.sales.pricing.pid;

import agents.firm.Firm;
import agents.firm.sales.SalesDepartment;
import junit.framework.Assert;
import model.MacroII;
import model.utilities.ActionOrder;
import org.junit.Test;

import static org.mockito.Mockito.*;

/**
 * <h4>Description</h4>
 * <p/>
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-02-08
 * @see
 */
public class SmoothedDailyInventoryPricingStrategyTest {

    /**
     * check that moving average is actually occurring
     */
    @Test
    public void testMovingAverage()
    {

        //I assume initially target is 0
        SalesDepartment department = mock(SalesDepartment.class);
        Firm firm = mock(Firm.class); when(department.getFirm()).thenReturn(firm);
        MacroII model = mock(MacroII.class); when(firm.getModel()).thenReturn(model);
        when(model.getCurrentPhase()).thenReturn(ActionOrder.DAWN);




        SmoothedDailyInventoryPricingStrategy strategy = new SmoothedDailyInventoryPricingStrategy(department);
        //force MA to be of length 10


        Assert.assertEquals(strategy.getTargetInventory(),0);
        //from now on inflow is 100
        when(department.getTodayInflow()).thenReturn(100);
        strategy.step(model);
        //should be somewhere between 0 and 100 but neither of the two extremes
        Assert.assertTrue(strategy.getTargetInventory() > 0);
        Assert.assertTrue(strategy.getTargetInventory() < 100);

        for(int i=0; i<9;i++)
        {
            strategy.step(model);
        }

        Assert.assertEquals(strategy.getTargetInventory(),100); //now it should be exactly 100







    }



}
