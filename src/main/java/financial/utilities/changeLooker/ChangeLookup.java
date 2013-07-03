/*
 * Copyright (c) 2013 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package financial.utilities.changeLooker;

import sim.engine.Steppable;

/**
 * <h4>Description</h4>
 * <p/> These are similar objects to price lookup except that they are steppable, they record at the end of the day the change in price
 * and either report it when asked or report a function of it (like an MA)
 * <p/> The step is supposed to be the data-lookup action
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-02-27
 * @see
 */
public interface ChangeLookup extends Steppable{

    /**
     * Get the change rate this object is supposed to look up
     * @return the change
     */
    public float getChange();

    /**
     * Turns itself off.
     */
    public void turnOff();



}