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
import model.utilities.stats.PeriodicMarketObserver;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;

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
 * @version 2013-07-11
 * @see
 */
public class RegressionSalePredictorTest {
    @Test
    public void testPredictSalePrice() throws Exception
    {


        PeriodicMarketObserver observer = mock(PeriodicMarketObserver.class);
        RegressionSalePredictor predictor = new RegressionSalePredictor(observer );



        when(observer.getNumberOfObservations()).thenReturn(3);
        when(observer.getPricesObservedAsArray()).thenReturn(new double[]{86,84,81});
        when(observer.getQuantitiesConsumedObservedAsArray()).thenReturn(new double[]{6,7,8});
        when(observer.getLastUntrasformedQuantityTraded()).thenReturn(8d);

        //this should regress to p=101.2 - 2.5 * q


        //the sales predictor will be predict for 9 (yesterdayVolume + 1)
        Assert.assertEquals(predictor.predictSalePriceAfterIncreasingProduction(mock(SalesDepartment.class), 100l, 1),79l);








    }


    @Test
    public void testPredictSalePriceWithLogs() throws Exception
    {

        PeriodicMarketObserver observer = mock(PeriodicMarketObserver.class);
        MacroII model = new MacroII(1l);
        RegressionSalePredictor predictor = new RegressionSalePredictor(observer );
        predictor.setQuantityTransformer(LearningFixedElasticitySalesPredictor.logTransformer);
        predictor.setPriceTransformer(LearningFixedElasticitySalesPredictor.logTransformer,
                LearningFixedElasticitySalesPredictor.expTransformer);

        //observation 1

        when(observer.getNumberOfObservations()).thenReturn(3);
        when(observer.getPricesObservedAsArray()).thenReturn(new double[]{86,84,81});
        when(observer.getQuantitiesConsumedObservedAsArray()).thenReturn(new double[]{6, 7, 8});
        when(observer.getLastUntrasformedQuantityTraded()).thenReturn(8d);


        //this should regress to log(p)=4.8275  -0.2068 * log(q)

        //the sales predictor will be predict for 9 (yesterdayVolume + 1)
        Assert.assertEquals(predictor.predictSalePriceAfterIncreasingProduction(mock(SalesDepartment.class), 100l, 1),79l);








    }


    @Test
    public void testScheduledProperly() throws NoSuchFieldException, IllegalAccessException {

        Market market = mock(Market.class);
        MacroII macroII = mock(MacroII.class);
        PeriodicMarketObserver.defaultDailyProbabilityOfObserving = .2f;

        RegressionSalePredictor predictor = new RegressionSalePredictor(market, macroII);

        //grab through reflection the reference to the observer!
        Field field = RegressionSalePredictor.class.getDeclaredField("observer");
        field.setAccessible(true);
        PeriodicMarketObserver observer = (PeriodicMarketObserver) field.get(predictor);

        verify(macroII).scheduleAnotherDay(ActionOrder.CLEANUP,observer,5, Priority.AFTER_STANDARD);
        predictor.setDailyProbabilityOfObserving(.3f);
        observer.step(macroII);
        verify(macroII).scheduleAnotherDay(ActionOrder.CLEANUP,observer,3, Priority.AFTER_STANDARD);


    }




    //Check defaults
    @Test
    public void testExtremes()
    {
        PeriodicMarketObserver.defaultDailyProbabilityOfObserving = 1;

        //no observations, should return whatever the sales department says
        Market market = mock(Market.class);
        MacroII macroII = new MacroII(System.currentTimeMillis());
        SalesDepartment department = mock(SalesDepartment.class);

        RegressionSalePredictor predictor = new RegressionSalePredictor(market, macroII);
        when(department.hypotheticalSalePrice()).thenReturn(50l);
        Assert.assertEquals(predictor.predictSalePriceAfterIncreasingProduction(department, 1000l, 1),50l);

        //with one observation, it still returns whatever the sales department says
        when(market.getYesterdayLastPrice()).thenReturn(10l);
        when(market.getYesterdayVolume()).thenReturn(1);
        macroII.getPhaseScheduler().step(macroII);
        Assert.assertEquals(predictor.predictSalePriceAfterIncreasingProduction(department, 1000l, 1),50l);

        //with no volume the observation is ignored
        when(market.getYesterdayLastPrice()).thenReturn(10l);
        when(market.getYesterdayVolume()).thenReturn(0);
        macroII.getPhaseScheduler().step(macroII);
        Assert.assertEquals(predictor.predictSalePriceAfterIncreasingProduction(department, 1000l, 1),50l);


        //two observations, everything back to normal!
        when(market.getYesterdayLastPrice()).thenReturn(10l);
        when(market.getYesterdayVolume()).thenReturn(1);
        macroII.getPhaseScheduler().step(macroII);
        Assert.assertEquals(predictor.predictSalePriceAfterIncreasingProduction(department, 1000l, 1),10l);


    }
}
