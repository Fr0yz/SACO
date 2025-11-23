package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.Dentista;
import service.LoginDentistaService;
import session.SessaoAtual;

import java.io.IOException;
import java.sql.SQLException;

public class LoginController {

    @FXML
    private TextField txtNome;

    @FXML
    private Label lblMensagem;

    private final LoginDentistaService loginService = new LoginDentistaService();

    @FXML
    private void onLogin(ActionEvent event) {
        lblMensagem.setText("");

        String nome = txtNome.getText();

        try {
            Dentista dentista = loginService.loginPorNome(nome);

            // guarda na sessão
            SessaoAtual.setDentistaLogado(dentista);

            // abre a tela principal
            abrirMainStage();

            // fecha a tela de login
            Stage loginStage = (Stage) txtNome.getScene().getWindow();
            loginStage.close();

        } catch (IllegalArgumentException | IllegalStateException e) {
            lblMensagem.setText(e.getMessage());
        } catch (SQLException e) {
            lblMensagem.setText("Erro no login: " + e.getMessage());
        } catch (IOException e) {
            lblMensagem.setText("Erro ao abrir tela principal: " + e.getMessage());
        }
    }

    private void abrirMainStage() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Main.fxml")); // ou /view/Main.fxml
        Parent root = loader.load(); // <-- location agora está setado

        Scene scene = new Scene(root, 1024, 640);
        Stage stage = new Stage();
        stage.setTitle("Odontologia Avançada");
        stage.setScene(scene);
        stage.show();
    }
}
