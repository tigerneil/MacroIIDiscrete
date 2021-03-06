/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package agents.firm.sales.pricing.decorators;

import agents.firm.sales.pricing.AskPricingStrategy;
import goods.Good;
import model.utilities.NonDrawable;

/**
 * <h4>Description</h4>
 * <p/> This reservation decorator intercepts all strategy calls of price and ceils all the prices below the reservation price given at constructor time
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2012-11-12
 * @see
 */
@NonDrawable
public class AskReservationPriceDecorator extends AskPricingDecorator {

    /**
     * The reservation price below which the sales department will never sell
     */
    final private int reservationPrice;

    /**
     * This reservation decorator intercepts all strategy calls of price() and ceils all the prices below the reservation price given at constructor time
     * @param toDecorate the ask strategy to decorate
     * @param reservationPrice the reservation price below which the sales department will never sell
     */
    public AskReservationPriceDecorator(AskPricingStrategy toDecorate, int reservationPrice) {
        super(toDecorate);
        this.reservationPrice = reservationPrice;
    }

    /**
     * The sales department is asked at what should be the sale price for a specific good; this, I guess, is the fundamental
     * part of the sales department
     *
     * @param g the good to price
     * @return the price given to that good
     */
    @Override
    public int price(Good g) {
        return Math.max(toDecorate.price(g), reservationPrice);

    }
}
