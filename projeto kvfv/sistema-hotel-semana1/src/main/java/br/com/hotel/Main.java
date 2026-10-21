package br.com.hotel;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        Label titulo = new Label("Sistema de Gerenciamento de Hotel");
        Label status = new Label("Projeto da disciplina — Semana 1");

        VBox layout = new VBox(15, titulo, status);
        layout.setStyle("-fx-padding: 30; -fx-font-size: 18px;");

        Scene scene = new Scene(layout, 600, 300);

        stage.setTitle("Sistema de Hotel");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        Database.initialize();
        launch(args);
    }
}
