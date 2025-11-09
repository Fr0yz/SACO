package controller;


import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


public class MainController {
    @FXML private StackPane contentPane;
    @FXML private Label lblTitulo;

    // Cache simples para não recarregar FXML toda hora
    private final Map<String, Node> viewCache = new HashMap<>();

    @FXML
    public void initialize() {
        // Tela inicial
        loadView("/PacienteView.fxml", "Cadastro de Paciente");
    }

    // ========= Navegação =========

    @FXML
    private void navPacientes() {
        loadView("/PacienteView.fxml", "Cadastro de Paciente");
    }

    @FXML
    private void navAnamnese() {
        loadView("/Anamnese.fxml", "Cadastro de Anamnese");
    }
    @FXML
    private void navMaterial() {
        loadView("/Material.fxml", "Cadastro de Material");
    }
    @FXML
    private void navTratamento() {
        loadView("/Tratamento.fxml", "Cadastro de Tratamento");
    }


    @FXML
    private void navSair() {
        // Fechar a janela principal
        contentPane.getScene().getWindow().hide();
    }

    // ========= Core de carregamento =========

    private void loadView(String fxmlPath, String titulo) {
        try {
            Node view = viewCache.get(fxmlPath);
            if (view == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
                view = loader.load();

                viewCache.put(fxmlPath, view);
            }
            contentPane.getChildren().setAll(view);
            lblTitulo.setText(titulo);
        } catch (IOException e) {
            e.printStackTrace();
            // fallback simples
            lblTitulo.setText("Erro ao carregar a página");
        }
    }
}