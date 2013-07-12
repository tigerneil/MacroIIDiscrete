/*
 * Copyright (c) 2013 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package agents.firm.sales.prediction;

import agents.firm.sales.SalesDepartment;
import financial.Market;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.scheduler.Priority;
import org.junit.Assert;
import org.junit.Test;
import sim.engine.Steppable;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyFloat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
 * @version 2013-07-12
 * @see
 */
public class LearningDecreaseSalesPredictorTest
{

    @Test
    public void testPredictSalePrice() throws Exception
    {
        RegressionSalePredictor.defaultDailyProbabilityOfObserving = 1f;



        Market market = mock(Market.class);
        MacroII model = new MacroII(System.currentTimeMillis());
        LearningDecreaseSalesPredictor predictor = new LearningDecreaseSalesPredictor(market,model );

        //observation 1
        when(market.getYesterdayLastPrice()).thenReturn(86l);
        when(market.getYesterdayVolume()).thenReturn(6);
        model.getPhaseScheduler().step(model);
        //observation 2
        when(market.getYesterdayLastPrice()).thenReturn(84l);
        when(market.getYesterdayVolume()).thenReturn(7);
        model.getPhaseScheduler().step(model);
        //observation 3
        when(market.getYesterdayLastPrice()).thenReturn(81l);
        when(market.getYesterdayVolume()).thenReturn(8);
        model.getPhaseScheduler().step(model);


        //this should regress to p=101.2 - 2.5 * q
        //now Q doesn't matter anymore, only previous Price

        SalesDepartment department = mock(SalesDepartment.class);
        when(department.hypotheticalSalePrice()).thenReturn(200l);
        //the sales predictor will be predict for 9 (yesterdayVolume + 1)
        Assert.assertEquals(predictor.predictSalePrice(department, 100l), 197l); //200-2.5 (rounded)








    }


    @Test
    public void testScheduledProperly()
    {

        Market market = mock(Market.class);
        MacroII macroII = mock(MacroII.class);
        RegressionSalePredictor.defaultDailyProbabilityOfObserving = .2f;

        new LearningDecreaseSalesPredictor(market,macroII);

        verify(macroII).scheduleAnotherDayWithFixedProbability(any(ActionOrder.class),any(Steppable.class),
                anyFloat(),any(Priority.class));
    }


    //Check defaults
    @Test
    public void testExtremes()
    {
        RegressionSalePredictor.defaultDailyProbabilityOfObserving = 1f;

        //no observations, should return whatever the sales department says
        Market market = mock(Market.class);
        MacroII model = new MacroII(System.currentTimeMillis());
        SalesDepartment department = mock(SalesDepartment.class);

        LearningDecreaseSalesPredictor predictor = new LearningDecreaseSalesPredictor(market,model );
        when(department.hypotheticalSalePrice()).thenReturn(50l);
        Assert.assertEquals(predictor.predictSalePrice(department,1000l),50l);

        //with one observation, it still returns whatever the sales department says
        when(market.getYesterdayLastPrice()).thenReturn(10l);
        when(market.getYesterdayVolume()).thenReturn(1);
        model.getPhaseScheduler().step(model);
        Assert.assertEquals(predictor.predictSalePrice(department,1000l),50l);

        //with no volume the observation is ignored
        when(market.getYesterdayLastPrice()).thenReturn(10l);
        when(market.getYesterdayVolume()).thenReturn(0);
        model.getPhaseScheduler().step(model);
        Assert.assertEquals(predictor.predictSalePrice(department,1000l),50l);


        //two observations, everything back to normal! (but the slope is 0, so no effect)
        when(market.getYesterdayLastPrice()).thenReturn(10l);
        when(market.getYesterdayVolume()).thenReturn(1);
        model.getPhaseScheduler().step(model);
        Assert.assertEquals(predictor.predictSalePrice(department,50l),50l);


    }

}
