/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package financial.market;

import goods.UndifferentiatedGoodType;
import model.MacroII;
import model.scenario.Scenario;
import model.utilities.dummies.Customer;
import model.utilities.dummies.DailyGoodTree;
import org.junit.Assert;
import org.junit.Test;

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
 * @version 2014-01-16
 * @see
 */
public class EndOfPhaseOrderHandlerTest {





    @Test
    public void marketClearsCorrectly() throws IllegalAccessException {

        MacroII model = new MacroII(10);
        model.setScenario(new Scenario(model) {
            @Override
            public void start() {
                OrderBookMarket market= new OrderBookMarket(UndifferentiatedGoodType.GENERIC);
                market.setOrderHandler(new EndOfPhaseOrderHandler(),model);
                getMarkets().put(UndifferentiatedGoodType.GENERIC,market);

                //20 buyers, from 100 to 120
                for(int i=0; i < 20; i++)
                {
                    Customer customer = new Customer(model,100+i,market);
                    getAgents().add(customer);
                }
                //1 seller, pricing 100, trying to sell 10 things!
                DailyGoodTree tree = new DailyGoodTree(model,10,100,market);
                getAgents().add(tree);
            }
        });
        model.start();

        for(int i=0; i< 100; i++)
        {
            model.schedule.step(model);
            Assert.assertEquals(10, model.getMarket(UndifferentiatedGoodType.GENERIC).getTodayVolume());
            //this should always be true!
            Assert.assertEquals(109,model.getMarket(UndifferentiatedGoodType.GENERIC).getBestBuyPrice());
        }









    }

}
