package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import model.Dentista;
import service.LoginDentistaService;
import session.SessaoAtual;

import java.io.IOException;
import java.sql.SQLException;
import java.net.URL;

public class MainController {

    @FXML
    private Label lblTitulo;

    @FXML
    private StackPane contentPane;

    private final LoginDentistaService loginService = new LoginDentistaService();

    @FXML
    private void initialize() {
        carregarPagina("/Paciente.fxml", "Pacientes");
    }

    // =========================
    // NAV: MENUS
    // =========================

    @FXML
    private void navPacientes(ActionEvent e) {
        carregarPagina("/Paciente.fxml", "Pacientes");
    }

    @FXML
    private void navAnamnese(ActionEvent e) {
        carregarPagina("/Anamnese.fxml", "Anamnese");
    }

    @FXML
    private void navMaterial(ActionEvent e) {
        carregarPagina("/Material.fxml", "Material");
    }

    @FXML
    private void navTratamento(ActionEvent e) {
        carregarPagina("/Tratamento.fxml", "Tratamento");
    }

    @FXML
    private void navAgendamento(ActionEvent e) {
        carregarPagina("/Agendamento.fxml", "Agendamento");
    }

    @FXML
    private void navFinanceiro(ActionEvent e) {
        carregarPagina("/Financeiro.fxml", "Financeiro");
    }

    private void carregarPagina(String caminhoFxml, String titulo) {
        try {
            // 1) Verifica se o FXML realmente existe
            URL url = getClass().getResource(caminhoFxml);
            if (url == null) {
                System.err.println("⚠ FXML NÃO ENCONTRADO: " + caminhoFxml);
                lblTitulo.setText("Arquivo não encontrado: " + caminhoFxml);
                return;
            }

            // 2) Cria o loader com a location correta
            FXMLLoader loader = new FXMLLoader(url);
            Node node = loader.load();

            // 3) Substitui o conteúdo do centro
            contentPane.getChildren().setAll(node);
            lblTitulo.setText(titulo);

        } catch (Exception ex) {
            System.err.println("❌ ERRO AO CARREGAR " + caminhoFxml);
            ex.printStackTrace();
            lblTitulo.setText("Erro ao carregar: " + titulo);
        }
    }

    // =========================
    // SAIR
    // =========================

    @FXML
    private void navSair(ActionEvent e) {
        // Tenta fazer logout do dentista logado
        Dentista d = SessaoAtual.getDentistaLogado();
        if (d != null) {
            try {
                loginService.logout(d);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        SessaoAtual.limpar();

        // Fecha a janela atual (Main)
        Stage stageAtual = (Stage) contentPane.getScene().getWindow();
        stageAtual.close();

        // Reabre a tela de login
        try {
            URL url = getClass().getResource("/Login.fxml"); // ajuste para /view/Login.fxml se estiver em pasta
            if (url == null) {
                System.err.println("⚠ Login.fxml NÃO ENCONTRADO!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            Scene scene = new Scene(root, 600, 300);
            Stage loginStage = new Stage();
            loginStage.setTitle("Login - Odontologia Avançada");
            loginStage.setScene(scene);
            loginStage.setResizable(false);
            loginStage.show();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
