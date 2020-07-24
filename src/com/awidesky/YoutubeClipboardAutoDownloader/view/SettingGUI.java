package com.awidesky.YoutubeClipboardAutoDownloader.view;

import java.io.IOException;

import com.awidesky.YoutubeClipboardAutoDownloader.YoutubeAudioDownloader;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

public class SettingGUI extends Application {


    private Stage primaryStage;
    private AnchorPane rootLayout;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Setting (you can close this window)");

        this.primaryStage.getIcons().add(new Image(YoutubeAudioDownloader.projectpath + "\\YoutubeAudioAutoDownloader-resources\\icon.ico"));
        
        initRootLayout();

    }

    /**
     * 상위 레이아웃을 초기화한다.
     */
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

}  // TODO : read https://aristatait.tistory.com/31
