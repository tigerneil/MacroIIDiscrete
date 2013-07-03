package tests.purchase;

import agents.EconomicAgent;
import agents.firm.Firm;
import agents.firm.purchases.PurchasesDepartment;
import agents.firm.purchases.prediction.SurveyPurchasesPredictor;
import agents.firm.sales.exploration.SellerSearchAlgorithm;
import agents.firm.sales.exploration.SimpleSellerSearch;
import financial.Market;
import financial.OrderBookMarket;
import financial.utilities.Quote;
import goods.Good;
import goods.GoodType;
import model.MacroII;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
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
public class SurveyPurchasesPredictorTest {
    @Test
    public void testPredictPurchasePrice() throws Exception {

        MacroII model = new MacroII(1l);
        Market market = new OrderBookMarket(GoodType.GENERIC);
        Firm buyer = mock(Firm.class); market.registerBuyer(buyer);   when(buyer.getModel()).thenReturn(model);

        for(int i=0; i<3; i++)
        {
            Firm seller = mock(Firm.class);
            Quote q = mock(Quote.class); when(q.getAgent()).thenReturn(seller); when(q.getPriceQuoted()).thenReturn(10l+i*10); when(q.getGood()).thenReturn(mock(Good.class));
            when(seller.askedForASaleQuote(any(EconomicAgent.class), any(GoodType.class))).thenReturn(q);
            market.registerSeller(seller);
        }

        PurchasesDepartment dept = mock(PurchasesDepartment.class);
        final SellerSearchAlgorithm algorithm = new SimpleSellerSearch(market,buyer);
        when(dept.getBestSupplierFound()).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return algorithm.getBestInSampleSeller();
            }
        });

        SurveyPurchasesPredictor predictor = new SurveyPurchasesPredictor();
        assertEquals(predictor.predictPurchasePrice(dept), 10);

        Firm seller = mock(Firm.class);
        Quote q = mock(Quote.class); when(q.getAgent()).thenReturn(seller); when(q.getPriceQuoted()).thenReturn(1l); when(q.getGood()).thenReturn(mock(Good.class));
        when(seller.askedForASaleQuote(any(EconomicAgent.class), any(GoodType.class))).thenReturn(q);
        market.registerSeller(seller);

        assertEquals(predictor.predictPurchasePrice(dept), 1);

    }




}