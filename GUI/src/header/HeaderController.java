package header;

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

public class HeaderController {

    // FXML elements
    @FXML Button          toolbarPullButton;
    @FXML Button          toolbarPushButton;
    @FXML Button          toolbarFetchButton;
    @FXML MenuItem        createNewRepositoryMenuBarButton;
    @FXML MenuItem        importFromXMLMenuBarButton;
    @FXML MenuItem        loadrepositoryFromPathMenuBarButton;
    @FXML MenuItem        cloneMenuBarButton;
    @FXML MenuItem        fetchMenuBarButton;
    @FXML MenuItem        pullMenuBarButton;
    @FXML MenuItem        pushMenuBarButton;
    @FXML MenuItem        commitMenuBarButton;
    @FXML MenuItem        showStatusMenuBarButton;
    @FXML MenuItem        createNewBranchMenuBarButton;
    @FXML MenuItem        deleteBranchMenuBarButton;
    @FXML MenuItem        checkoutMenuBarButton;
    @FXML MenuItem        mergeWithMenuBarButton;
    @FXML MenuItem        resetBranchSHAMenuBarButton;
    @FXML MenuItem        showStatusSplitMenuButton;
    @FXML MenuItem        changeActiveUserTopMenuItem;
    @FXML MenuItem        changeActiveUserSideMenuItem;
    @FXML MenuButton      toolbarMergeWithButton;
    @FXML MenuButton      activeUserMenuButton;
    @FXML SplitMenuButton commitSplitMenuButton;


    // properties
    private SimpleBooleanProperty isRepositoryLoaded;

    private AppController appController;

    public HeaderController(AppController appController) {
        this.appController = appController;
        this.isRepositoryLoaded = appController.getIsRepositoryLoaded();
    }

    public void setAppController(AppController appController) {
        this.appController = appController;
    }

    @FXML
    private void initialize() {
        activeUserMenuButton.textProperty().bind(appController.getActiveUser());

        // Availability
        toolbarPullButton.disableProperty().bind(isRepositoryLoaded.not());
        toolbarPushButton.disableProperty().bind(isRepositoryLoaded.not());
        toolbarFetchButton.disableProperty().bind(isRepositoryLoaded.not());
        fetchMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        pullMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        pushMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        commitMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        showStatusMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        createNewBranchMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        deleteBranchMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        checkoutMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        mergeWithMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        resetBranchSHAMenuBarButton.disableProperty().bind(isRepositoryLoaded.not());
        showStatusSplitMenuButton.disableProperty().bind(isRepositoryLoaded.not());
        toolbarMergeWithButton.disableProperty().bind(isRepositoryLoaded.not());
        commitSplitMenuButton.disableProperty().bind(isRepositoryLoaded.not());
    }
}