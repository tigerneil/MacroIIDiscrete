/*
 * Copyright (c) 2013 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.utilities.stats.regression;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

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
 * @version 2013-10-07
 * @see
 */
public class GeneticLinearRegressionTest
{


    double y[] = new double[]{0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,171.0,0.0,0.0,-2.0,0.0,0.0,-1.0,-1.0,0.0,-2.0,0.0,0.0,0.0,-2.0,0.0,-1.0,-2.0,0.0,-1.0,0.0,-1.0,-1.0,0.0,0.0,-2.0,0.0,0.0,0.0,0.0,-1.0,-1.0,0.0,-2.0,0.0,0.0,0.0,-1.0,-1.0,0.0,0.0,-2.0,0.0,0.0,-1.0,-2.0,1.0,-2.0,0.0,-1.0,-1.0,0.0,-2.0,0.0,-1.0,-1.0,0.0,-1.0,-1.0,0.0,-2.0,0.0,-1.0,-2.0,1.0,-1.0,-2.0,1.0,-1.0,-1.0,0.0,0.0,-2.0,0.0,-1.0,-1.0,0.0,-1.0,-2.0,1.0,1.0,1.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,
            0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0};

    double weights[] = new double[]{0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.45016600194986517,0.45016600194986517,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.45016600194986517,0.40131233845548236,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.3543436883195632,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.45016600194986517,0.40131233845548236,0.45016600194986517,0.45016600194986517,0.45016600194986517,0.45016600194986517,0.45016600194986517,0.45016600194986517,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.45016600194986517,0.40131233845548236,0.45016600194986517,0.45016600194986517,0.45016600194986517,0.40131233845548236,0.45016600194986517,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.45016600194986517,0.40131233845548236,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.40131233845548236,0.40131233845548236,0.45016600194986517,0.45016600194986517,0.45016600194986517,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5,0.5};

    double[] x1 = new double[]{-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,-1.0,170.0,170.0,170.0,168.0,168.0,168.0,167.0,166.0,166.0,164.0,164.0,164.0,164.0,162.0,162.0,161.0,159.0,159.0,158.0,158.0,157.0,156.0,156.0,156.0,154.0,154.0,154.0,154.0,154.0,153.0,152.0,152.0,150.0,150.0,150.0,150.0,149.0,148.0,148.0,148.0,146.0,146.0,146.0,145.0,143.0,144.0,142.0,142.0,141.0,140.0,140.0,138.0,138.0,137.0,136.0,136.0,135.0,134.0,134.0,132.0,132.0,131.0,129.0,130.0,129.0,127.0,128.0,127.0,126.0,126.0,126.0,124.0,124.0,123.0,122.0,122.0,121.0,119.0,120.0,121.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0,122.0};

    double[] x2 = new double[]{0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,1.0,1.0,1.0,2.0,2.0,2.0,3.0,3.0,4.0,4.0,4.0,4.0,4.0,5.0,5.0,6.0,6.0,7.0,7.0,7.0,8.0,8.0,8.0,8.0,9.0,9.0,9.0,9.0,9.0,10.0,10.0,11.0,11.0,11.0,11.0,11.0,12.0,12.0,12.0,12.0,13.0,13.0,13.0,14.0,14.0,15.0,15.0,15.0,16.0,16.0,17.0,17.0,17.0,18.0,18.0,18.0,19.0,19.0,20.0,20.0,20.0,21.0,21.0,21.0,22.0,22.0,22.0,23.0,23.0,23.0,24.0,24.0,24.0,25.0,25.0,25.0,26.0,26.0,26.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0,25.0};


    @Test
    public void regressionTest() throws Exception
    {

        //normal regression:
        MultipleLinearRegression regression = new MultipleLinearRegression();
        regression.estimateModel(y,weights,x1,x2);
        System.out.println(Arrays.toString(regression.getResultMatrix()));

        //check that fitness is low
        GeneticLinearRegression.RegressionFitness fitness = new GeneticLinearRegression.RegressionFitness(y,weights,x1,x2);
        System.out.println(fitness.getFitness(new GeneticLinearRegression.RegressionCandidate(new double[]{164.39d,-0.95919d,-1.896d}),null));

        GeneticLinearRegression regression1 = new GeneticLinearRegression();
        regression1.estimateModel(y,weights,x1,x2);
        System.out.println(Arrays.toString(regression1.getResults()));
        Assert.assertEquals(regression.getResultMatrix()[1],regression1.getResults()[1],.05d);
        Assert.assertEquals(regression.getResultMatrix()[2],regression1.getResults()[2],.05d);

    }
}