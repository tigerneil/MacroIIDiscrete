/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under the Academic Free License version 3.0
 * See the file "LICENSE" for more information
 */

package model.gui;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import model.gui.market.GeographicalMarketPresentation;
import model.scenario.GeographicalScenario;
import model.utilities.Deactivatable;
import model.utilities.geography.NewFirmPortrait;

/**
 * A series of buttons to press to switch mouse mode. Unfortunately because toggleGroup property is readOnly my Heterogeneous Bidirectional Binder
 * doesn't apply and I need to duplicate again a lot of listening code!
 * Created by carrknight on 4/24/14.
 */
public class AddAgentsToMapTitledPane extends TitledPane implements ChangeListener<Object>, Deactivatable {


    /**
     * The mouse mode
     */
    private final ObjectProperty<MouseMode> currentMouseMode;


    //all the toggles
    private final ToggleButton normalSelection;
    private final ToggleButton addOilPump;
    private final ToggleButton addHousehold;
    private final ToggleGroup mouseSelection;

    private boolean updating = false;

    private final MouseModeSwitcher mouseModeSwitcher;

    public final static String SELECT_BUTTON_STRING = "Select";
    public final static String ADD_FIRM_BUTTON_STRING = "Add Firm";
    public final static String ADD_HOUSEHOLD_BUTTON_STRING = "Add Household";
    public final static String PANE_TITLE = "Add Agents";


    public AddAgentsToMapTitledPane(GeographicalMarketPresentation geographicalMarketPresentation, GeographicalScenario scenario) {


        normalSelection = new ToggleButton(SELECT_BUTTON_STRING);
        final NewFirmPortrait newFirmPortrait = new NewFirmPortrait();
        addOilPump = new ToggleButton(ADD_FIRM_BUTTON_STRING, newFirmPortrait);
        addHousehold = new ToggleButton(ADD_HOUSEHOLD_BUTTON_STRING);

        mouseSelection = new ToggleGroup();
        normalSelection.setToggleGroup(mouseSelection);
        addOilPump.setToggleGroup(mouseSelection);
        addHousehold.setToggleGroup(mouseSelection);

        //default to normal selection
        mouseSelection.selectToggle(normalSelection);
        currentMouseMode = new SimpleObjectProperty<>(MouseMode.SELECTION);
        //create the mouse mode "actuator"
        mouseModeSwitcher = new MouseModeSwitcher(currentMouseMode,geographicalMarketPresentation,scenario);

        //start listening
        mouseSelection.selectedToggleProperty().addListener(this);
        currentMouseMode.addListener(this);
        //add the buttons to the accordion
        HBox toggleContainer = new HBox(normalSelection,addOilPump,addHousehold);
        super.setContent(toggleContainer);

        super.setText(PANE_TITLE);



    }

    @Override
    public void changed(ObservableValue sourceProperty, Object oldValue, Object newValue) {
        if(updating) //avoid infinite recursion
            return;
        //this is designed so that the properties never go to null
        assert (mouseSelection.selectedToggleProperty()) != null;
        assert (currentMouseMode.getValue()) != null;


        updating=true;
        final Object source = sourceProperty.getValue();
        if(source.equals(currentMouseMode))
        {
            MouseMode mode = (MouseMode) newValue;
            switch (mode)
            {
                default:
                case SELECTION:
                    normalSelection.setSelected(true);
                    break;
                case ADD_FIRM:
                    addOilPump.setSelected(true);
                    break;
                case ADD_CONSUMER:
                    addHousehold.setSelected(true);
                    break;
            }



        }
        else
        {
            assert source.equals(mouseSelection.selectedToggleProperty()) ||
                    mouseSelection.getToggles().contains(source) ;

            Toggle newToggle = (Toggle) newValue;
            //the toggle has changed
            if(newToggle.equals(normalSelection))
                currentMouseMode.setValue(MouseMode.SELECTION);
            else if(newToggle.equals(addOilPump))
                currentMouseMode.setValue(MouseMode.ADD_FIRM);
            else{
                assert newToggle.equals(addHousehold);
                currentMouseMode.setValue(MouseMode.ADD_CONSUMER);
            }


        }
        updating =false;


    }

    @Override
    public void turnOff() {
        mouseModeSwitcher.turnOff();
        mouseSelection.selectedToggleProperty().removeListener(this);
        currentMouseMode.removeListener(this);
    }

    public ToggleButton getAddHousehold() {
        return addHousehold;
    }

    public ToggleButton getNormalSelection() {
        return normalSelection;
    }

    public ToggleButton getAddOilPump() {
        return addOilPump;
    }
}