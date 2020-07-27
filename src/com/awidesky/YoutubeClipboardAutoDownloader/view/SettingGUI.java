package com.awidesky.YoutubeClipboardAutoDownloader.view;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ResourceBundle;

import com.awidesky.YoutubeClipboardAutoDownloader.ConfigDTO;
import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.YoutubeAudioDownloader;
import com.awidesky.YoutubeClipboardAutoDownloader.console.ConsoleView;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class SettingGUI extends Application implements Initializable {


    private Stage primaryStage;
    private AnchorPane rootLayout;
    private ConsoleView console;
    
    @FXML
    private BorderPane consolePane;
    
    @FXML
    private ComboBox<String> cb_format;
    ObservableList<String> formatList = FXCollections.observableArrayList("mp3", "best", "aac", "flac", "m4a", "opus", "vorbis", "wav");
    
    @FXML
    private ComboBox<String> cb_quality;
    ObservableList<String> qualityList = FXCollections.observableArrayList("0(best)", "1", "2", "3", "4", "5", "6", "7", "8", "9(worst)");

    @FXML
    private TextField tf_path;

    @Override
    public void start(Stage primaryStage) throws MalformedURLException {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Setting (you can close this window)");

        this.primaryStage.getIcons().add(new Image("file:resources/icon/icon.jpg"));
        System.out.println("start");
        initRootLayout();

        primaryStage.setOnCloseRequest(event -> {
            
        	Main.writeProperties();
        	
        });
        
        
    }

    
    public void initRootLayout() {
    	System.out.println("initstart");
        try {
        	
            FXMLLoader loader = new FXMLLoader(); //SettingGUI.class.getResource("SettingLayout.fxml")
            loader.setLocation(SettingGUI.class.getResource("SettingLayout.fxml"));
            rootLayout = (AnchorPane)loader.load();

            console = new ConsoleView();
            System.setOut(console.getOut());
            System.setIn(console.getIn());
            System.setErr(console.getOut());
            
            consolePane.setCenter(console);
            System.out.println("init");
            primaryStage.setScene(new Scene(rootLayout));
            primaryStage.show();
            
        } catch (Exception e) {
        	System.out.println("ex");
            e.printStackTrace();
            
        }
    }
    

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		
		 cb_format.setItems(formatList);
		 cb_format.setValue(Main.getProperties().getExtension());
		 tf_path.setText(Main.getProperties().getSaveto());
		 
	}

      
    public void comboChange(ActionEvent event) {
    	
    	ConfigDTO c = Main.getProperties();
    	
    	c.setExtension(cb_format.getValue());
    	c.setQuality(String.valueOf(qualityList.indexOf(cb_quality.getValue())));
    	System.out.println(cb_format.getValue() + "//" + cb_quality.getValue());
    }

    public void BrowsebtnClicked() {
    	
    	DirectoryChooser chooser = new DirectoryChooser();
    	chooser.setTitle("Choose directory to save music!");
    	chooser.setInitialDirectory(new File(Main.getProperties().getSaveto()));
    	File selectedDirectory = chooser.showDialog(primaryStage);
    	
    	if (selectedDirectory == null) {
    		
    		Alert alert = new Alert(AlertType.ERROR, "Please choose a directory!", ButtonType.OK);
    		alert.showAndWait();
    		selectedDirectory = new File(Main.getProperties().getSaveto());
    		
    	}
    	
    	tf_path.setText(selectedDirectory.getAbsolutePath());
    	Main.getProperties().setSaveto(selectedDirectory.getAbsolutePath());
		
	}
    
    
    public static void main(String[] args) {
    	
    	Main.start(args);
    	launch(args);
    	
    }

}  
