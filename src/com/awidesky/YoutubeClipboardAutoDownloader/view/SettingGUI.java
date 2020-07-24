package com.awidesky.YoutubeClipboardAutoDownloader.view;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.awidesky.YoutubeClipboardAutoDownloader.ConfigDTO;
import com.awidesky.YoutubeClipboardAutoDownloader.Main;
import com.awidesky.YoutubeClipboardAutoDownloader.YoutubeAudioDownloader;

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
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class SettingGUI extends Application implements Initializable {


    private Stage primaryStage;
    private AnchorPane rootLayout;
 
    @FXML
    private ComboBox<String> cb_format;
    ObservableList<String> formatList = FXCollections.observableArrayList("mp3", "best", "aac", "flac", "m4a", "opus", "vorbis", "wav");
    
    @FXML
    private ComboBox<String> cb_quality;
    ObservableList<String> qualityList = FXCollections.observableArrayList("0(best)", "1", "2", "3", "4", "5", "6", "7", "8", "9(worst)");

    @FXML
    private TextField tf_path;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Setting (you can close this window)");

        this.primaryStage.getIcons().add(new Image(YoutubeAudioDownloader.projectpath + "\\YoutubeAudioAutoDownloader-resources\\icon.ico"));
        
        initRootLayout();

        primaryStage.setOnCloseRequest(event -> {
            
        	Main.writeProperties();
        	
        });
        
        
    }

    
    public void initRootLayout() {
    	
        try {
        	
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(SettingGUI.class.getResource("SettingLayout.fxml"));
            rootLayout = (AnchorPane)loader.load();

            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (IOException e) {
        	
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
    
    

}  
