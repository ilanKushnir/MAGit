package footer;

import Engine.Branch;
import Engine.Manager;
import Engine.Repository;
import app.AppController;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import subComponents.createNewBranchDialog.CreateNewBranchDialogController;

import java.awt.TextArea;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Optional;

public class FooterController {

    @FXML Label dynamicMessageLabel;
    private AppController appController;

    // properties
    private SimpleStringProperty dynamicStatusMessage;
    // TODO progress property

    public FooterController(AppController appController) {
        this.appController = appController;
        dynamicStatusMessage = new SimpleStringProperty("No action has been made.");
    }

    public void setDynamicMessageProperty(String message) {
        this.dynamicStatusMessage.set(message);
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    @FXML
    private void initialize() {
        dynamicMessageLabel.textProperty().bind(dynamicStatusMessage);
    }
}