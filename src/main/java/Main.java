import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // LOGIN PRIMEIRO
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml")); // ou /view/Login.fxml
        Parent root = loader.load();

        Scene scene = new Scene(root, 600, 300);
        stage.setTitle("Login - Odontologia Avançada");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
