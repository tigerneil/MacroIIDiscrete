package model.gui.paper2;

import agents.firm.purchases.prediction.FixedIncreasePurchasesPredictor;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.prediction.FixedDecreaseSalesPredictor;
import agents.firm.sales.pricing.pid.InventoryBufferSalesControl;
import agents.firm.sales.pricing.pid.SimpleFlowSellerPID;
import goods.UndifferentiatedGoodType;
import model.MacroII;
import model.scenario.MonopolistScenario;
import model.utilities.ActionOrder;
import sim.engine.SimState;
import sim.engine.Steppable;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

/**
 * An easy way to build the monopolist scenario from gui instructions
 * Created by carrknight on 5/22/15.
 */
public class MonopolistScenarioFactory implements Function<MacroII, MonopolistScenario> {
    private JCheckBox inventoryTick;
    private JCheckBox learningBox;
    private JSpinner laborInterceptSpinner;
    private JSpinner laborSupplySlope;
    private JSpinner demandIntercept;
    private JSpinner demandSlope;
    private JSpinner demandDelay;
    private JPanel settingPanel;

    private SpinnerNumberModel laborInterceptModel;
    private SpinnerNumberModel laborSupplyModel;
    private SpinnerNumberModel demandInterceptModel;
    private SpinnerNumberModel demandSlopeModel;
    private SpinnerNumberModel demandDelayModel;


    private void createUIComponents() {
        laborInterceptModel = new SpinnerNumberModel(
                new Integer(14), // value
                new Integer(0), // min
                new Integer(100), // max
                new Integer(1) // step
        );
        laborInterceptSpinner = new JSpinner(laborInterceptModel);
        laborSupplyModel = new SpinnerNumberModel(
                new Integer(1), // value
                new Integer(1), // min
                new Integer(3), // max
                new Integer(1) // step
        );
        laborSupplySlope = new JSpinner(laborSupplyModel);
        demandInterceptModel = new SpinnerNumberModel(
                new Integer(100), // value
                new Integer(0), // min
                new Integer(300), // max
                new Integer(1) // step
        );
        demandIntercept = new JSpinner(demandInterceptModel);
        demandSlopeModel = new SpinnerNumberModel(
                new Integer(1), // value
                new Integer(1), // min
                new Integer(3), // max
                new Integer(1) // step
        );
        demandSlope = new JSpinner(demandSlopeModel);
        demandDelayModel = new SpinnerNumberModel(
                new Integer(0), // value
                new Integer(0), // min
                new Integer(30), // max
                new Integer(1) // step
        );
        demandDelay = new JSpinner(demandDelayModel);


    }


    /**
     * Applies this function to the given argument.
     *
     * @param macroII the function argument
     * @return the function result
     */
    @Override
    public MonopolistScenario apply(MacroII macroII) {

        MonopolistScenario scenario = new MonopolistScenario(macroII);

        scenario.setDemandSlope(demandSlopeModel.getNumber().intValue());
        scenario.setDemandIntercept(demandInterceptModel.getNumber().intValue());
        scenario.setDailyWageIntercept(laborInterceptModel.getNumber().intValue());
        scenario.setDailyWageSlope(laborSupplyModel.getNumber().intValue());
        scenario.setBuyerDelay(demandDelayModel.getNumber().intValue());
        scenario.setControlType(MonopolistScenario.MonopolistScenarioIntegratedControlEnum.MARGINAL_PLANT_CONTROL);
        scenario.setWorkersToBeRehiredEveryDay(true);

        if (inventoryTick.isSelected())
            scenario.setAskPricingStrategy(InventoryBufferSalesControl.class);
        else
            scenario.setAskPricingStrategy(SimpleFlowSellerPID.class);


        if (!learningBox.isSelected())
            macroII.scheduleSoon(ActionOrder.CLEANUP_DATA_GATHERING, new Steppable() {
                @Override
                public void step(SimState simState) {

                    SalesDepartment salesDepartment = scenario.getMonopolist().getSalesDepartment(
                            UndifferentiatedGoodType.GENERIC);
                    salesDepartment.setPredictorStrategy(new FixedDecreaseSalesPredictor(scenario.getDemandSlope()));
                    scenario.getMonopolist().getHRs().iterator().next().setPredictor(
                            new FixedIncreasePurchasesPredictor(scenario.getDailyWageSlope()));
                }
            });

        return scenario;
    }


    public JPanel getSettingPanel() {
        return settingPanel;
    }

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        createUIComponents();
        settingPanel = new JPanel();
        settingPanel.setLayout(
                new com.intellij.uiDesigner.core.GridLayoutManager(7, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Labor Supply Intercept");
        settingPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Labor Supply Slope");
        settingPanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Demand Slope");
        settingPanel.add(label3, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Demand Intercept");
        settingPanel.add(label4, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        inventoryTick = new JCheckBox();
        inventoryTick.setText("");
        settingPanel.add(inventoryTick, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1,
                                                                                         com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                         com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                         com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                         com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                         null, null, null, 0, false));
        learningBox = new JCheckBox();
        learningBox.setText("");
        settingPanel.add(learningBox, new com.intellij.uiDesigner.core.GridConstraints(6, 1, 1, 1,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                       null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Inventory Buffer");
        settingPanel.add(label5, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Learning");
        settingPanel.add(label6, new com.intellij.uiDesigner.core.GridConstraints(6, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        settingPanel.add(laborInterceptSpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1,
                                                                                                 com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                                 com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                                 com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                                 null, null, null, 0,
                                                                                                 false));
        settingPanel.add(laborSupplySlope, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                            null, null, null, 0,
                                                                                            false));
        settingPanel.add(demandIntercept, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                           null, null, null, 0, false));
        settingPanel.add(demandSlope, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                       null, null, null, 0, false));
        final JLabel label7 = new JLabel();
        label7.setText("Demand Delay");
        settingPanel.add(label7, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        settingPanel.add(demandDelay, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                       com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                       null, null, null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return settingPanel;
    }
}