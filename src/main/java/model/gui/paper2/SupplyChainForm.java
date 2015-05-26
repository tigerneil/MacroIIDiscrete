package model.gui.paper2;

import agents.firm.Firm;
import agents.firm.personell.HumanResources;
import agents.firm.production.Blueprint;
import agents.firm.production.control.maximizer.algorithms.marginalMaximizers.MarginalMaximizer;
import agents.firm.purchases.PurchasesDepartment;
import agents.firm.purchases.prediction.FixedIncreasePurchasesPredictor;
import agents.firm.sales.SalesDepartment;
import agents.firm.sales.SalesDepartmentOneAtATime;
import agents.firm.sales.prediction.ErrorCorrectingSalesPredictor;
import agents.firm.sales.prediction.FixedDecreaseSalesPredictor;
import agents.firm.sales.prediction.SalesPredictor;
import agents.firm.sales.pricing.pid.InventoryBufferSalesControl;
import financial.market.Market;
import model.MacroII;
import model.experiments.stickyprices.StickyPricesCSVPrinter;
import model.scenario.OneLinkSupplyChainScenario;
import model.scenario.OneLinkSupplyChainScenarioWithCheatingBuyingPrice;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.function.Function;

/**
 * A gui object to set up upstream monopolist scenario
 * Created by carrknight on 5/22/15.
 */
abstract public class SupplyChainForm implements Function<MacroII, OneLinkSupplyChainScenarioWithCheatingBuyingPrice> {
    private JSpinner timiditySpinner;
    private JSpinner stickinessSpinner;
    private JCheckBox upstreamLearning;
    private JCheckBox downstreamLearning;
    private JPanel controlPanel;
    private JSpinner proportionalSpinner;
    private JSpinner integralSpinner;


    private SpinnerNumberModel timidityModel;
    private SpinnerNumberModel stickinessModel;
    private SpinnerNumberModel proportionalModel;
    private SpinnerNumberModel integralModel;


    public JPanel getControlPanel() {
        return controlPanel;
    }

    private void createUIComponents() {


        timidityModel = new SpinnerNumberModel(
                new Integer(1), // value
                new Integer(1), // min
                new Integer(200), // max
                new Integer(1) // step
        );
        timiditySpinner = new JSpinner(timidityModel);


        stickinessModel = new SpinnerNumberModel(
                new Integer(StickyPricesCSVPrinter.DEFAULT_STICKINESS), // value
                new Integer(0), // min
                new Integer(200), // max
                new Integer(1) // step
        );
        stickinessSpinner = new JSpinner(stickinessModel);


        proportionalModel = new SpinnerNumberModel(
                new Double(.1), // value
                new Double(0.01), // min
                new Double(2), // max
                new Double(.01) // step
        );
        proportionalSpinner = new JSpinner(proportionalModel);

        integralModel = new SpinnerNumberModel(
                new Double(.1), // value
                new Double(0.01), // min
                new Double(2), // max
                new Double(.01) // step
        );
        integralSpinner = new JSpinner(integralModel);


    }


    public int getTimidity() {
        return timidityModel.getNumber().intValue();
    }

    public int getStickiness() {
        return stickinessModel.getNumber().intValue();
    }

    public float getProportionalGain() {
        return proportionalModel.getNumber().floatValue();
    }

    public float getIntegralGain() {
        return integralModel.getNumber().floatValue();
    }


    public boolean isUpstreamLearning() {
        return upstreamLearning.isSelected();
    }


    public boolean isDownstreamLearning() {
        return downstreamLearning.isSelected();
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
        controlPanel = new JPanel();
        controlPanel.setLayout(
                new com.intellij.uiDesigner.core.GridLayoutManager(6, 2, new Insets(0, 0, 0, 0), -1, -1));
        final JLabel label1 = new JLabel();
        label1.setText("Timidity");
        controlPanel.add(label1, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final JLabel label2 = new JLabel();
        label2.setText("Stickiness");
        controlPanel.add(label2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final JLabel label3 = new JLabel();
        label3.setText("Proportional Gain");
        controlPanel.add(label3, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final JLabel label4 = new JLabel();
        label4.setText("Integral Gain");
        controlPanel.add(label4, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final JLabel label5 = new JLabel();
        label5.setText("Upstream Learning");
        controlPanel.add(label5, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        final JLabel label6 = new JLabel();
        label6.setText("Downstream Learning");
        controlPanel.add(label6, new com.intellij.uiDesigner.core.GridConstraints(5, 0, 1, 1,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                  null, null, null, 0, false));
        controlPanel.add(timiditySpinner, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                           null, null, null, 0, false));
        controlPanel.add(stickinessSpinner, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                             com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                             null, null, null, 0,
                                                                                             false));
        controlPanel.add(proportionalSpinner, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1,
                                                                                               com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                               com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                               com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                               com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                               null, null, null, 0,
                                                                                               false));
        controlPanel.add(integralSpinner, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.FILL_HORIZONTAL,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_WANT_GROW,
                                                                                           com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                           null, null, null, 0, false));
        upstreamLearning = new JCheckBox();
        upstreamLearning.setText("");
        controlPanel.add(upstreamLearning, new com.intellij.uiDesigner.core.GridConstraints(4, 1, 1, 1,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                            com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                            null, null, null, 0,
                                                                                            false));
        downstreamLearning = new JCheckBox();
        downstreamLearning.setText("");
        controlPanel.add(downstreamLearning, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.ANCHOR_WEST,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.FILL_NONE,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_SHRINK | com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_CAN_GROW,
                                                                                              com.intellij.uiDesigner.core.GridConstraints.SIZEPOLICY_FIXED,
                                                                                              null, null, null, 0,
                                                                                              false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return controlPanel;
    }
}