package org.cirdles.topsoil.app.menu;

import com.sun.javafx.stage.StageHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.input.Clipboard;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.cirdles.topsoil.app.browse.DesktopWebBrowser;
import org.cirdles.topsoil.app.metadata.TopsoilMetadata;
import org.cirdles.topsoil.app.isotope.IsotopeSelectionDialog;
import org.cirdles.topsoil.app.isotope.IsotopeType;
import org.cirdles.topsoil.app.table.uncertainty.UncertaintyFormat;
import org.cirdles.topsoil.app.tab.TopsoilTabPane;
import org.cirdles.topsoil.app.dataset.entry.TopsoilDataEntry;
import org.cirdles.topsoil.app.table.TopsoilDataTable;
import org.cirdles.topsoil.app.util.dialog.TableUncertaintyChoiceDialog;
import org.cirdles.topsoil.app.util.file.FileParser;
import org.cirdles.topsoil.app.util.file.TopsoilFileChooser;
import org.cirdles.topsoil.app.util.serialization.TopsoilSerializer;
import org.cirdles.topsoil.app.util.dialog.ErrorAlerter;
import org.cirdles.topsoil.app.util.issue.IssueCreator;
import org.cirdles.topsoil.app.util.issue.StandardGitHubIssueCreator;
import org.cirdles.topsoil.app.util.dialog.YesNoAlert;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.cirdles.topsoil.app.util.file.ExampleDataTable;

/**
 * A class containing a set of methods for handling actions for {@code MenuItem}s in the {@link MainMenuBar}.
 *
 * @author Benjamin Muldrow
 * @see MainMenuBar
 */
public class MenuItemEventHandler {

    /**
     * Handles importing tables from CSV / TSV files
     *
     * @return Topsoil Table file
     * @throws IOException for invalid file selection
     */
    public static TopsoilDataTable handleTableFromFile() throws IOException {

        TopsoilDataTable table = null;

        // select file
        File file = FileParser.openTableDialog(StageHelper.getStages().get(0));

        if (file != null && FileParser.isSupportedTableFile(file)) {

            // select headers
            String[] headers = null;
            Boolean hasHeaders;

            // TODO For now, the user must have headers in the file. In the future, they can specify.
            hasHeaders = FileParser.detectHeader(file);

            // hasHeaders would only be null if the user clicked "Cancel".
            if (hasHeaders != null) {
                if (hasHeaders) {
                    headers = FileParser.parseHeaders(file);
                }

                // select isotope flavor -- For now, the user shouldn’t have to select an isotope system; instead assume Generic.
                //IsotopeType isotopeType = IsotopeSelectionDialog.selectIsotope(new IsotopeSelectionDialog());
                IsotopeType isotopeType = IsotopeType.Generic;

                // isotopeType would only be null if the user clicked "Cancel".
                if (isotopeType != null) {
                    List<TopsoilDataEntry> entries = FileParser.parseFile(file, hasHeaders);

                    if (entries != null) {
                        TableUncertaintyChoiceDialog uncertaintyChoiceDialog = new TableUncertaintyChoiceDialog();
                        UncertaintyFormat selectedFormat = uncertaintyChoiceDialog.selectUncertaintyFormat();

                        if (selectedFormat != null) {
                            ObservableList<TopsoilDataEntry> data = FXCollections.observableList(entries);
                            applyUncertaintyFormat(selectedFormat, data);

                            table = new TopsoilDataTable(headers,
                                                         isotopeType,
                                                         selectedFormat,
                                                         data.toArray(new TopsoilDataEntry[data.size()]));
                            table.setTitle(file.getName().substring(0, file.getName().indexOf(".")));
                        }
                    } else {
                        ErrorAlerter alerter = new ErrorAlerter();
                        alerter.alert("File is empty!");
                    }
                }
            }
        }

        return table;
    }

    /**
     * Pareses table data from the system {@code Clipboard} into a new {@code TopsoilDataTable}.
     *
     * @return  a new TopsoilDataTable
     */
    public static TopsoilDataTable handleTableFromClipboard() {

        TopsoilDataTable table = null;
        String content = Clipboard.getSystemClipboard().getString();

        String delim = FileParser.getDelimiter(content);

        if (delim != null) {

            String[] headers = null;
            Boolean hasHeaders;

            // TODO For now, the user must have headers in the file. In the future, they can specify.
            hasHeaders = FileParser.detectHeader(content);

            if (hasHeaders != null) {
                if (hasHeaders) {
                    headers = FileParser.parseHeaders(content, delim);
                }

                // select isotope flavor -- For now, the user shouldn’t have to select an isotope system; instead assume Generic.
                //IsotopeType isotopeType = IsotopeSelectionDialog.selectIsotope(new IsotopeSelectionDialog());
                IsotopeType isotopeType = IsotopeType.Generic;

                if (isotopeType != null) {
                    List<TopsoilDataEntry> entries = FileParser.parseClipboard(hasHeaders, delim);

                    if (entries != null) {

                        TableUncertaintyChoiceDialog uncertaintyChoiceDialog = new TableUncertaintyChoiceDialog();
                        UncertaintyFormat selectedFormat = uncertaintyChoiceDialog.selectUncertaintyFormat();

                        if (selectedFormat != null) {
                            ObservableList<TopsoilDataEntry> data = FXCollections.observableList(entries);
                            applyUncertaintyFormat(selectedFormat, data);

                            table = new TopsoilDataTable(headers,
                                                         isotopeType,
                                                         selectedFormat,
                                                         data.toArray(new TopsoilDataEntry[data.size()]));
                        }
                    } else {
                        ErrorAlerter alerter = new ErrorAlerter();
                        alerter.alert("Clipboard is empty!");
                    }
                }
            }
        }
        return table;
    }

    /**
     * Handle new {@code TopsoilDataTable} creation.
     *
     * @return new TopsoilDataTable
     */
    public static TopsoilDataTable handleNewTable() {

        TopsoilDataTable table;

//        // select isotope flavor -- For now, the user shouldn’t have to select an isotope system; instead assume Generic.
//        IsotopeType isotopeType = IsotopeSelectionDialog.selectIsotope(new IsotopeSelectionDialog());
        IsotopeType isotopeType = IsotopeType.Generic;

        // create empty table
        TableUncertaintyChoiceDialog uncertaintyChoiceDialog = new TableUncertaintyChoiceDialog();
        UncertaintyFormat selectedFormat = uncertaintyChoiceDialog.selectUncertaintyFormat();

        if (selectedFormat != null) {
            table = new TopsoilDataTable(null, isotopeType, selectedFormat, new TopsoilDataEntry[]{});
        } else {
            table = null;
        }

        return table;
    }

    /**
     * Handles the opening of sample data table for a given isotope type.
     *
     * @param tabs  the TopsoilTabPane to which to add tables
     * @param isotopeType the isotope type of the example table to be opened
     */
    public static TopsoilDataTable handleOpenExampleTable(TopsoilTabPane tabs, IsotopeType isotopeType) {
        TopsoilDataTable table = null;
        UncertaintyFormat format;

        //TODO Determine format of table.
        format = UncertaintyFormat.TWO_SIGMA_PERCENT;

        if (isotopeType != null) {
                
                List<TopsoilDataEntry> entries = null;
                String[] headers = null;
                String exampleContent = new ExampleDataTable().getSampleData(isotopeType);
                
                headers = FileParser.parseHeaders(exampleContent,FileParser.getDelimiter(exampleContent));
                entries = FileParser.parseTxt(FileParser.readLines(exampleContent),FileParser.getDelimiter(exampleContent),true);

                if (entries == null) {
                        table = null;
                    } else {
                        ObservableList<TopsoilDataEntry> data = FXCollections.observableList(entries);
                        table = new TopsoilDataTable(headers, isotopeType, format, data.toArray(new TopsoilDataEntry[data.size()]));
                        table.setTitle(isotopeType.getName()+" Example Data");
                }
                
        }
        return table;
    }
    
    /**
     * Open default browser and create a new GitHub issue with user specifications already supplied
     * */
    public static void handleReportIssue() {
        IssueCreator issueCreator = new StandardGitHubIssueCreator(
                new TopsoilMetadata(),
                System.getProperties(),
                new DesktopWebBrowser(Desktop.getDesktop(), new ErrorAlerter()),
                new StringBuilder()
        );
        issueCreator.create();
    }

    /**
     * Clear a table of all data.
     *
     * @param table TopsoilDataTable to clear
     * @return resulting TopsoilDataTable
     */
    public static TopsoilDataTable handleClearTable(TopsoilDataTable table) {

        // alert user for confirmation
        Alert confirmAlert = new YesNoAlert("Are you sure you want to clear the table?");
        Optional<ButtonType> response = confirmAlert.showAndWait();
        TopsoilDataTable resultingTable = table;

        // get user confirmation
        if (response.isPresent()
                && response.get() == ButtonType.YES) {
            resultingTable = new TopsoilDataTable(table.getColumnNames(), table.getIsotopeType(), table
                    .getUncertaintyFormat(), new TopsoilDataEntry[]{});
        }

        return resultingTable;
    }

    /**
     * Closes all open tabs in the {@code TopsoilTabPane}, as well as any open plots. Used when a project is loaded,
     * or when one is closed.
     *
     * @param tabs  the active TopsoilTabPane
     */
    private static void closeAllTabsAndPlots(TopsoilTabPane tabs) {
        tabs.getTabs().clear();
        List<Stage> stages = StageHelper.getStages();
        for (int index = stages.size() - 1; index > 0; index--) {
            stages.get(index).close();
        }
    }

    /**
     * Opens a .topsoil project {@code File}. If any tabs or plots are open, they are closed and replaced with the
     * project's information.
     *
     * @param tabs  the TopsoilTabPane to which to add tables
     */
    public static void handleOpenProjectFile(TopsoilTabPane tabs) {
        if (!tabs.isEmpty()) {
            Alert verification = new Alert(
                    Alert.AlertType.CONFIRMATION,
                    "Opening a Topsoil project will replace your current tables. Continue?",
                    ButtonType.CANCEL,
                    ButtonType.YES
            );
            verification.showAndWait().ifPresent(response -> {
                if (response == ButtonType.YES) {
                    File file = TopsoilFileChooser.getTopsoilOpenFileChooser().showOpenDialog(StageHelper.getStages().get(0));
                    openProjectFile(tabs, file);
                }
            });
        } else {
            File file = TopsoilFileChooser.getTopsoilOpenFileChooser().showOpenDialog(StageHelper.getStages().get(0));
            openProjectFile(tabs, file);
        }
    }

    /**
     * Opens a .topsoil {@code File}.
     *
     * @param tabs  the active TopsoilTabPane.
     * @param file  the project File
     */
    public static void openProjectFile(TopsoilTabPane tabs, File file) {
        if (file != null) {
            String fileName = file.getName();
            String extension = fileName.substring(
                    fileName.lastIndexOf(".") + 1,
                    fileName.length());
            if (!extension.equals("topsoil")) {
                ErrorAlerter error = new ErrorAlerter();
                error.alert("Project must be a .topsoil file.");
            } else {
                closeAllTabsAndPlots(tabs);
                TopsoilSerializer.deserialize(file, tabs);
            }
        }
    }

    /**
     * Saves changes to an open .topsoil project.
     * <p>
     *     If the project is open, but the {@code File} can't be found (e.g. if the file was deleted externally while
     *     the project was open), then the user is forced to "Save As".
     * </p>
     *
     * @param tabs  the active TopsoilTabPane
     */
    public static void handleSaveProjectFile(TopsoilTabPane tabs) {
        if (TopsoilSerializer.isProjectOpen()) {
            if (TopsoilSerializer.projectFileExists()) {
                File file = TopsoilSerializer.getCurrentProjectFile();
                saveProjectFile(file, tabs);
            } else {
                handleSaveAsProjectFile(tabs);
            }
        }
    }

    /**
     * Saves an open .topsoil project.
     *
     * @param file  the open .topsoil project File
     * @param tabs  the active TopsoilTabPane
     * @return  true if file is successfully saved
     */
    private static boolean saveProjectFile(File file, TopsoilTabPane tabs) {
        if (file != null) {
            String fileName = file.getName();
            String extension = fileName.substring(
                    fileName.lastIndexOf(".") + 1,
                    fileName.length());
            if (!extension.equals("topsoil")) {
                ErrorAlerter error = new ErrorAlerter();
                error.alert("Project must be a .topsoil file.");
            } else {
                TopsoilSerializer.serialize(file, tabs);
                return true;
            }
        }
        return false;
    }

    /**
     * Creates a new .topsoil file for the current tabs and plots.
     *
     * @param tabs  the TopsoilTabPane from which to save tables
     * @return  true if the file was successfully saved
     */
    public static boolean handleSaveAsProjectFile(TopsoilTabPane tabs) {
        FileChooser fileChooser =  TopsoilFileChooser.getTopsoilSaveFileChooser();
        File file = fileChooser.showSaveDialog(StageHelper.getStages().get(0));

        if (file != null) {
            saveProjectFile(file, tabs);
            TopsoilSerializer.setCurrentProjectFile(file);
            return true;
        }
        return false;
    }

    /**
     * Closes the project file, and closes all open tabs and plots.
     *
     * @param tabs  the active TopsoilTabPane
     * @return  true if the file is successfully closed
     */
    public static boolean handleCloseProjectFile(TopsoilTabPane tabs) {
        Alert saveAndCloseAlert = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to save your changes?",
                ButtonType.CANCEL, ButtonType.NO, ButtonType.YES);
        AtomicReference<Boolean> reference = new AtomicReference<>(false);
        saveAndCloseAlert.showAndWait().ifPresent(response -> {
            if (response != ButtonType.CANCEL) {
                if (response == ButtonType.YES) {
                    MenuItemEventHandler.handleSaveProjectFile(tabs);
                }
                closeAllTabsAndPlots(tabs);
                TopsoilSerializer.closeProjectFile();
                reference.set(true);
            }
        });
        return reference.get();
    }

    /**
     * Normalizes the supplied data using the value of the specified {@code UncertaintyFormat}.
     *
     * @param format    UncertaintyFormat
     * @param data  data as a List of TopsoilDataEntries
     */
    private static void applyUncertaintyFormat(UncertaintyFormat format, List<TopsoilDataEntry> data) {
        // If uncertainty uncertaintyFormat is not one sigma absolute, convert uncertainty data to one sigma absolute.
        if (format != UncertaintyFormat.ONE_SIGMA_ABSOLUTE) {
            double formatValue = format.getValue();

            for (TopsoilDataEntry entry : data) {
                entry.getProperties().get(2).set(entry.getProperties().get(2).get() / formatValue);
                entry.getProperties().get(3).set(entry.getProperties().get(3).get() / formatValue);
            }
        }
    }
}