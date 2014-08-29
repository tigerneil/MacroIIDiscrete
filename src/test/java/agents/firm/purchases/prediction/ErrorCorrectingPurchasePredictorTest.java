/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package agents.firm.purchases.prediction;

import model.MacroII;
import model.scenario.MonopolistScenario;
import model.scenario.MonopolistScenarioTest;
import org.junit.Test;

public class ErrorCorrectingPurchasePredictorTest {


    @Test
    public void inMonopolistSetting() throws Exception {

        for(int i=0; i<5; i++) {
            //this is basically random-slopes:
            long seed = System.currentTimeMillis();
            System.out.println("seed " + seed);
            MacroII model = new MacroII(seed);
            MonopolistScenario scenario = new MonopolistScenario(model);
            scenario.setPurchasesPricePreditorStrategy(ErrorCorrectingPurchasePredictor.class);
            MonopolistScenarioTest.testRandomSlopeMonopolist((int) seed, model, scenario);
        }




    }

}