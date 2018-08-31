package org.cirdles.topsoil.app.tab;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.SplitPane;

import javafx.scene.layout.AnchorPane;
import org.cirdles.commons.util.ResourceExtractor;
import org.cirdles.topsoil.app.isotope.IsotopeType;
import org.cirdles.topsoil.app.plot.PlotGenerationHandler;
import org.cirdles.topsoil.app.plot.TopsoilPlotView;
import org.cirdles.topsoil.app.plot.variable.Variable;
import org.cirdles.topsoil.app.plot.variable.Variables;
import org.cirdles.topsoil.app.table.TopsoilDataColumn;
import org.cirdles.topsoil.app.table.ObservableTableData;
import org.cirdles.topsoil.app.table.TopsoilSpreadsheetView;
import org.cirdles.topsoil.app.uncertainty.UncertaintyFormat;
import org.cirdles.topsoil.app.util.dialog.VariableChooserDialog;
import org.cirdles.topsoil.plot.Plot;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 *
 * @author marottajb
 *
 * @see TopsoilTab
 * @see TopsoilTabPane
 */
public class TopsoilDataView extends AnchorPane {

    private static final String CONTROLLER_FXML = "data-view.fxml";

    private ObservableTableData data;
    private TopsoilSpreadsheetView spreadsheetView;

	//**********************************************//
	//                   CONTROLS                   //
	//**********************************************//

	@FXML private ComboBox<IsotopeType> isotopeSystemComboBox;
	@FXML private ComboBox<UncertaintyFormat> uncertaintyFormatComboBox;

	@FXML private Button assignVariablesButton;
	@FXML private Button generatePlotButton;

    @FXML private AnchorPane spreadsheetPane;

	//**********************************************//
	//                  PROPERTIES                  //
	//**********************************************//

	private ObjectProperty<IsotopeType> isotopeSystem;
	public ObjectProperty<IsotopeType> isotopeSystemProperty() {
		if (isotopeSystem == null) {
			isotopeSystem = new SimpleObjectProperty<>();
			isotopeSystem.bindBidirectional(isotopeSystemComboBox.valueProperty());
		}
		return isotopeSystem;
	}
	public final IsotopeType getIsotopeSystem() {
		return isotopeSystemProperty().get();
	}
	public final void setIsotopeSystem(IsotopeType i) {
		isotopeSystemProperty().set(i);
	}

	private ObjectProperty<UncertaintyFormat> uncertaintyFormat;
	public ObjectProperty<UncertaintyFormat> uncertaintyFormatProperty() {
		if (uncertaintyFormat == null) {
			uncertaintyFormat = new SimpleObjectProperty<>();
			uncertaintyFormat.bindBidirectional(uncertaintyFormatComboBox.valueProperty());
		}
		return uncertaintyFormat;
	}
	public final UncertaintyFormat getUncertaintyFormat() {
		return uncertaintyFormatProperty().get();
	}
	public final void setUncertaintyFormat(UncertaintyFormat format) {
		uncertaintyFormatProperty().set(format);
	}

    //**********************************************//
    //                 CONSTRUCTORS                 //
    //**********************************************//

    public TopsoilDataView() {
        this(new ObservableTableData());
    }

    public TopsoilDataView(ObservableTableData data) {
        this.data = data;
        this.spreadsheetView = new TopsoilSpreadsheetView(data);
        try {
            FXMLLoader loader = new FXMLLoader(new ResourceExtractor(TopsoilDataView.class).extractResourceAsPath
		            (CONTROLLER_FXML).toUri().toURL());
            loader.setRoot(this);
            loader.setController(this);
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    //**********************************************//
    //                PUBLIC METHODS                //
    //**********************************************//

    /** {@inheritDoc}
     */
    @FXML public void initialize() {
        spreadsheetPane.getChildren().add(spreadsheetView);
        AnchorPane.setBottomAnchor(spreadsheetView, 0.0);
        AnchorPane.setRightAnchor(spreadsheetView, 0.0);
        AnchorPane.setTopAnchor(spreadsheetView, 0.0);
        AnchorPane.setLeftAnchor(spreadsheetView, 0.0);

	    isotopeSystemComboBox.getItems().addAll(IsotopeType.values());
	    uncertaintyFormatComboBox.getItems().addAll(UncertaintyFormat.values());

	    isotopeSystemProperty().bindBidirectional(data.isotopeTypeProperty());
	    setUncertaintyFormat(data.getUnctFormat());
	    uncertaintyFormatComboBox.setDisable(true);
    }

    public TopsoilSpreadsheetView getSpreadsheetView() {
        return spreadsheetView;
    }

    public ObservableTableData getData() {
        return spreadsheetView.getData();
    }

    public void showVariableChooserDialog(List<Variable<Number>> required) {
        Map<Variable<Number>, TopsoilDataColumn> selections = VariableChooserDialog.showDialog(this, required);
        setVariableAssignments(selections);
    }

    public void setVariableAssignments(Map<Variable<Number>, TopsoilDataColumn> assignments) {

        if (assignments != null) {

            for (Map.Entry<Variable<Number>, TopsoilDataColumn> entry : assignments.entrySet()) {
                data.setVariableForColumn(data.getDataColumns().indexOf(entry.getValue()), entry.getKey());
            }

            for (TopsoilPlotView plotView : data.getOpenPlots().values()) {
                plotView.getPlot().setData(data.getPlotEntries());
	            // Re-name x and y axis titles
	            if (assignments.containsKey(Variables.X)) {
		            plotView.getPropertiesPanel().setXAxisTitle((assignments.get(Variables.X).getName()));
	            }
	            if (assignments.containsKey(Variables.Y)) {
		            plotView.getPropertiesPanel().setYAxisTitle((assignments.get(Variables.Y).getName()));
	            }
            }
        }
    }

    //**********************************************//
    //               PRIVATE METHODS                //
    //**********************************************//

    @FXML private void assignVariablesButtonAction() {
        showVariableChooserDialog(null);
    }

    @FXML private void generatePlotButtonAction() {

        // If X and Y aren't specified.
        if ( ! data.variableToColumnMap().containsKey(Variables.X) || ! data.variableToColumnMap().containsKey(Variables.Y)) {
            showVariableChooserDialog(asList(Variables.X, Variables.Y));
        }

        if (data.variableToColumnMap().containsKey(Variables.X) && data.variableToColumnMap().containsKey(Variables.Y)) {
            PlotGenerationHandler.generatePlotForSelectedTab(TabPaneHandler.getTabPane());
        }
    }

}
