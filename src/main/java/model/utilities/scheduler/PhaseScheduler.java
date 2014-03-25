/*
 * Copyright (c) 2013 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.utilities.scheduler;

import model.utilities.ActionOrder;
import sim.engine.Steppable;

import javax.annotation.Nonnull;

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
 * @version 2013-02-12
 * @see
 */
public interface PhaseScheduler extends Steppable {

    /**
     * Schedule as soon as this phase occurs (at priority STANDARD)
     * @param phase the phase i want the action to occur in
     * @param action the steppable that should be called
     */
    void scheduleSoon(@Nonnull ActionOrder phase, @Nonnull Steppable action);


    /**
     * Schedule as soon as this phase occurs
     * @param phase the phase i want the action to occur in
     * @param action the steppable that should be called
     * @param priority the action priority
     *
     */
    void scheduleSoon(@Nonnull ActionOrder phase, @Nonnull Steppable action, Priority priority);

    /**
     * Schedule tomorrow assuming the phase passed is EXACTLY the current phase (at priority STANDARD)
     * @param phase the phase i want the action to occur in
     * @param action the steppable that should be called
     */
    void scheduleTomorrow(ActionOrder phase, Steppable action);

    /**
     * Schedule tomorrow assuming the phase passed is EXACTLY the current phase
     * @param phase the phase i want the action to occur in
     * @param action the steppable that should be called
     * @param priority the action priority
     */
    void scheduleTomorrow(ActionOrder phase, Steppable action,Priority priority);

    /**
     * Schedule in as many days as passed (at priority standard)
     * @param phase the phase i want the action to occur in
     * @param action the steppable that should be called
     * @param daysAway how many days into the future should this happen
     */
    void scheduleAnotherDay(@Nonnull ActionOrder phase, @Nonnull Steppable action,
                            int daysAway);

    /**
     * Schedule in as many days as passed (at priority standard)
     * @param phase the phase i want the action to occur in
     * @param action the steppable that should be called
     * @param daysAway how many days into the future should this happen
     * @param priority the action priority
     */
    void scheduleAnotherDay(@Nonnull ActionOrder phase, @Nonnull Steppable action,
                            int daysAway,Priority priority);

    /**
     *
     * @param phase the phase i want the action to occur in
     * @param probability each day we check against this fixed probability to know if we will step on this action today
     * @param action the steppable that should be called
     */
    void scheduleAnotherDayWithFixedProbability(@Nonnull ActionOrder phase, @Nonnull Steppable action,
                                                float probability);

    /**
     * @param probability each day we check against this fixed probability to know if we will step on this action today
     * @param phase the phase i want the action to occur in
     * @param action the steppable that should be called
     * @param
     */
    void scheduleAnotherDayWithFixedProbability(@Nonnull ActionOrder phase, @Nonnull Steppable action,
                                                float probability, Priority priority);

    /**
     * deletes everything
     */
    void clear();

    ActionOrder getCurrentPhase();
}
