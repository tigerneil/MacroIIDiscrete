package tests.purchase;

import agents.firm.purchases.PurchasesDepartment;
import agents.firm.purchases.prediction.PricingPurchasesPredictor;
import financial.Market;
import financial.OrderBookMarket;
import goods.GoodType;
import junit.framework.Assert;
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
 * @author Ernesto
 * @version 2012-10-04
 * @see
 */
public class PricingPurchasesPredictorTest {


    @Test
    public void mockTest() throws Exception {
        //simple mock test
        PurchasesDepartment dept = mock(PurchasesDepartment.class);
        PricingPurchasesPredictor predictor = new PricingPurchasesPredictor();

        for(long i=0; i<100; i++){
            when(dept.maxPrice(any(GoodType.class),any(Market.class))).thenReturn(i);
            Assert.assertEquals(predictor.predictPurchasePrice(dept), i);
        }





    }

    @Test
    public void fullyDressedTest() throws Exception {

        Market market = new OrderBookMarket(GoodType.GENERIC);
        PurchasesDepartment dept = MemoryPurchasesPredictorTest.fixedPIDTest(market);

        /**********************************************
         * Make sure the last closing price is correctly predicted by the predictor!
         *********************************************/
        PricingPurchasesPredictor predictor = new PricingPurchasesPredictor();
        Assert.assertTrue(predictor.predictPurchasePrice(dept) >= 20 && predictor.predictPurchasePrice(dept) <= 30);
    }

}
