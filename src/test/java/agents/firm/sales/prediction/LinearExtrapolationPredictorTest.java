/*
 * Copyright (c) 2013 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package agents.firm.sales.prediction;

import agents.firm.production.Plant;
import agents.firm.sales.SalesDepartment;
import model.MacroII;
import model.utilities.ActionOrder;
import org.junit.Assert;
import org.junit.Test;

import java.util.LinkedList;

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
 * @version 2013-04-09
 * @see
 */
public class LinearExtrapolationPredictorTest {


    @Test
    public void SchedulesAtConstructionTest()
    {
        SalesDepartment dept = mock(SalesDepartment.class);
        MacroII macroII = mock(MacroII.class);




        LinearExtrapolationPredictor predictor = new LinearExtrapolationPredictor(dept,macroII);

        verify(macroII,times(1)).scheduleSoon(ActionOrder.THINK, predictor);



    }

    @Test
    public void LinearExtrapolationPredictorTest()
    {

        //simple test, move from one worker to two
        SalesDepartment dept = mock(SalesDepartment.class);
        MacroII macroII = mock(MacroII.class);   when(macroII.getCurrentPhase()).thenReturn(ActionOrder.THINK); //to please preconditions
        Plant p = mock(Plant.class);
        LinkedList<Plant> plants = new LinkedList<>(); plants.add(p); when(dept.getServicedPlants()).thenReturn(plants);


        LinearExtrapolationPredictor predictor = new LinearExtrapolationPredictor(dept,macroII);

        when(p.workerSize()).thenReturn(1);
        when(dept.getLastClosingPrice()).thenReturn(15l);

        //let it learn
        predictor.step(macroII);
        predictor.step(macroII);
        predictor.step(macroII);
        predictor.step(macroII);
        Assert.assertEquals(predictor.getLowWorkersPrice(),15f,.001f); //float because it's rounding

        //decrease price by 5!
        when(p.workerSize()).thenReturn(2);
        when(dept.getLastClosingPrice()).thenReturn(10l);
        predictor.step(macroII);  //learn again
        predictor.step(macroII);
        predictor.step(macroII);
        predictor.step(macroII);
        Assert.assertEquals(predictor.getLowWorkersPrice(),15f,.001f); //float because it's rounding
        Assert.assertEquals(predictor.getHighWorkersPrice(),10f,.001f); //float because it's rounding
        Assert.assertEquals(predictor.predictSalePrice(dept,1000l),5l);
    }

    //test precipitous drop!
    @Test
    public void precipitousDrop()
    {
        //simple test, move from one worker to two
        SalesDepartment dept = mock(SalesDepartment.class);
        MacroII macroII = mock(MacroII.class);   when(macroII.getCurrentPhase()).thenReturn(ActionOrder.THINK); //to please preconditions
        Plant p = mock(Plant.class);
        LinkedList<Plant> plants = new LinkedList<>(); plants.add(p); when(dept.getServicedPlants()).thenReturn(plants);


        LinearExtrapolationPredictor predictor = new LinearExtrapolationPredictor(dept,macroII);


        //imagine linear demand p = 100-2W
        for(int workers = 1; workers <= 10; workers++)
        {
            long price = 100 - 2 * workers;
            when(p.workerSize()).thenReturn(workers);
            when(dept.getLastClosingPrice()).thenReturn(price);
            predictor.step(macroII);
            predictor.step(macroII);
            predictor.step(macroII);
            predictor.step(macroII);
        }


        //now drop the workers to 5!
        when(p.workerSize()).thenReturn(5);
        when(dept.getLastClosingPrice()).thenReturn(100l-2l*5);
        predictor.step(macroII);
        predictor.step(macroII);
        predictor.step(macroII);
        predictor.step(macroII);

        //the prediction should now be
        Assert.assertEquals(predictor.predictSalePrice(dept,1000l),100l-2l*6);



    }


    //test at 0
    @Test
    public void at0()
    {


        //simple test, move from one worker to two
        SalesDepartment dept = mock(SalesDepartment.class);
        MacroII macroII = mock(MacroII.class);   when(macroII.getCurrentPhase()).thenReturn(ActionOrder.THINK); //to please preconditions
        Plant p = mock(Plant.class);
        LinkedList<Plant> plants = new LinkedList<>(); plants.add(p); when(dept.getServicedPlants()).thenReturn(plants);


        LinearExtrapolationPredictor predictor = new LinearExtrapolationPredictor(dept,macroII);

        when(p.workerSize()).thenReturn(0);
        when(dept.getLastClosingPrice()).thenReturn(15l);

        //let it learn
        predictor.step(macroII);
        predictor.step(macroII);
        predictor.step(macroII);
        predictor.step(macroII);
        Assert.assertEquals(predictor.getLowWorkersPrice(),15f,.001f); //float because it's rounding

        //decrease price by 5!
        when(p.workerSize()).thenReturn(1);
        when(dept.getLastClosingPrice()).thenReturn(10l);
        predictor.step(macroII);  //learn again
        predictor.step(macroII);
        predictor.step(macroII);
        predictor.step(macroII);
        Assert.assertEquals(predictor.getLowWorkersPrice(),15f,.001f); //float because it's rounding
        Assert.assertEquals(predictor.getHighWorkersPrice(),10f,.001f); //float because it's rounding


        //the only difference is that it ignores 0 and just quotes the #1 worker price
        Assert.assertEquals(predictor.predictSalePrice(dept,1000l),10l);



    }

    //test that it gets rescheduled!
    //test precipitous drop!
    @Test
    public void rescheduleTest()
    {
        //simple test, move from one worker to two
        SalesDepartment dept = mock(SalesDepartment.class);
        MacroII macroII = mock(MacroII.class);   when(macroII.getCurrentPhase()).thenReturn(ActionOrder.THINK); //to please preconditions
        Plant p = mock(Plant.class);
        LinkedList<Plant> plants = new LinkedList<>(); plants.add(p); when(dept.getServicedPlants()).thenReturn(plants);


        LinearExtrapolationPredictor predictor = new LinearExtrapolationPredictor(dept,macroII);
        verify(macroII,times(1)).scheduleSoon(ActionOrder.THINK, predictor);

        int timesRescheduled = 0;
        //imagine linear demand p = 100-2W
        for(int workers = 1; workers <= 10; workers++)
        {
            long price = 100 - 2 * workers;
            when(p.workerSize()).thenReturn(workers);
            when(dept.getLastClosingPrice()).thenReturn(price);
            predictor.step(macroII);
            verify(macroII,times(++timesRescheduled)).scheduleTomorrow(ActionOrder.THINK, predictor);
            predictor.step(macroII);
            verify(macroII,times(++timesRescheduled)).scheduleTomorrow(ActionOrder.THINK, predictor);
            predictor.step(macroII);
            verify(macroII,times(++timesRescheduled)).scheduleTomorrow(ActionOrder.THINK, predictor);
            predictor.step(macroII);
            verify(macroII,times(++timesRescheduled)).scheduleTomorrow(ActionOrder.THINK, predictor);

        }






    }


}