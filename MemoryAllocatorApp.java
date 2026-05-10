import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry point — loads the FXML layout and shows the window.
 */
public class MemoryAllocatorApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("main.fxml"));
        Scene scene = new Scene(root, 1250, 720);
        scene.getStylesheets().add(getClass().getResource("style.css").toExternalForm());
        stage.setTitle("Memory Allocation Simulator — Segmentation");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
