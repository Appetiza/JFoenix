/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package demos.components;

import com.jfoenix.controls.JFXMenuButton;
import java.util.Arrays;
import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * @author gcmartins
 */
public class MenuButtonDemo extends Application {

    @Override
    public void start(Stage stage) {
        FlowPane main = new FlowPane();
        main.setVgap(20);
        main.setHgap(20);       

        MenuButton javaMenuButton = new MenuButton("Java MenuButton");
        javaMenuButton.getItems().setAll(new MenuItem("Action 1"), new MenuItem("Action 2"));
        main.getChildren().add(javaMenuButton);
        
        JFXMenuButton jfoenixMenuButton = new JFXMenuButton("JFoenix MenuButton");
        jfoenixMenuButton.getItems().setAll(new MenuItem("Action 1"), new MenuItem("Action 2"));
        main.getChildren().add(jfoenixMenuButton);

        JFXMenuButton button = new JFXMenuButton("RAISED MENUBUTTON");
        button.getStyleClass().add("menu-button-raised");
        button.getItems().setAll(new MenuItem("Action 1"), new MenuItem("Action 2"));
        main.getChildren().add(button);

        JFXMenuButton button1 = new JFXMenuButton("DISABLED");
        button1.setDisable(true);
        button1.getItems().setAll(new MenuItem("Action 1"), new MenuItem("Action 2"));
        main.getChildren().add(button1);

        StackPane pane = new StackPane();
        pane.getChildren().add(main);
        StackPane.setMargin(main, new Insets(100));
        pane.setStyle("-fx-background-color:WHITE");

        final Scene scene = new Scene(pane, 800, 200);
        scene.getStylesheets().add(MenuButtonDemo.class.getResource("/css/jfoenix-components.css").toExternalForm());
        stage.setTitle("JFX MenuButton Demo");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

}
