package controller;

import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.util.converter.NumberStringConverter;
import model.Material;
import service.MaterialService;
import service.MaterialService.ServiceException;

import java.util.List;
import java.util.Optional;

public class MaterialController {

    // ====== UI: Form ======
    @FXML private TextField txtId;
    @FXML private TextField txtNome;
    @FXML private TextField txtQuantidade;

    // ====== UI: Ações ======
    @FXML private Button btnNovo;
    @FXML private Button btnSalvar;
    @FXML private Button btnExcluir;
    @FXML private Button btnMais1;
    @FXML private Button btnMenos1;
    @FXML private Button btnDefinirQtde;

    // ====== UI: Tabela ======
    @FXML private TextField txtFiltro;
    @FXML private TableView<Material> tabela;
    @FXML private TableColumn<Material, Integer> colId;
    @FXML private TableColumn<Material, Integer> colQuantidade;
    @FXML private TableColumn<Material, String> colNome;
    @FXML private TableColumn<Material, Void> colAcoes;

    // ====== Dados/Serviço ======
    private final MaterialService service = new MaterialService();
    private final ObservableList<Material> dados = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        configurarTabela();
        configurarForm();
        configurarAtalhos();
        carregarTabela();
    }

    // --------------------------------------------------------
    // Configurações de UI
    // --------------------------------------------------------
    private void configurarTabela() {
        colId.setCellValueFactory(c -> Bindings.createIntegerBinding(() ->
                c.getValue().getID() == null ? 0 : c.getValue().getID()
        ).asObject());

        colNome.setCellValueFactory(c -> Bindings.createStringBinding(() ->
                c.getValue().getNOME() == null ? "" : c.getValue().getNOME()
        ));

        colQuantidade.setCellValueFactory(c -> Bindings.createIntegerBinding(() ->
                c.getValue().getQUANTIDADE() == null ? 0 : c.getValue().getQUANTIDADE()
        ).asObject());

        // Coluna de ações: Editar / Excluir
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar  = new Button("Editar");
            private final Button btnExcluir = new Button("Excluir");
            private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6, btnEditar, btnExcluir);
            {
                btnEditar.setOnAction(e -> {
                    Material m = getTableView().getItems().get(getIndex());
                    preencherForm(m);
                });
                btnExcluir.setOnAction(e -> {
                    Material m = getTableView().getItems().get(getIndex());
                    excluirMaterial(m);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        tabela.setItems(dados);

        // Seleção da tabela preenche formulário
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) preencherForm(sel);
        });

        // Filtro simples por nome
        txtFiltro.textProperty().addListener((obs, old, f) -> aplicarFiltro(f));
    }

    private void configurarForm() {
        // ID somente leitura (texto)
        txtId.setEditable(false);

        // Quantidade só números (permitir vazio para “novo”)
        txtQuantidade.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) return;
            if (!val.matches("-?\\d+")) {
                txtQuantidade.setText(old == null ? "" : old);
            }
        });

        // Estados dos botões
        btnExcluir.disableProperty().bind(
                Bindings.createBooleanBinding(() -> txtId.getText() == null || txtId.getText().isBlank())
        );
        btnMais1.disableProperty().bind(btnExcluir.disableProperty());
        btnMenos1.disableProperty().bind(btnExcluir.disableProperty());
        btnDefinirQtde.disableProperty().bind(btnExcluir.disableProperty());
    }

    private void configurarAtalhos() {
        // Enter no filtro recarrega
        txtFiltro.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) carregarTabela();
        });
        // Enter na quantidade/ nome -> salvar
        txtNome.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onSalvar(); });
        txtQuantidade.setOnKeyPressed(e -> { if (e.getCode() == KeyCode.ENTER) onSalvar(); });
    }

    // --------------------------------------------------------
    // Ações de Botões (ligar no FXML)
    // --------------------------------------------------------
    @FXML
    public void onNovo() {
        limparForm();
        tabela.getSelectionModel().clearSelection();
        txtNome.requestFocus();
    }

    @FXML
    public void onSalvar() {
        String nome = safeTrim(txtNome.getText());
        String idStr = safeTrim(txtId.getText());
        String qStr  = safeTrim(txtQuantidade.getText());

        try {
            Integer qtd = parseIntOrZero(qStr);

            if (idStr.isBlank()) {
                // Criar
                long id = service.criar(nome, qtd);
                alertInfo("Material criado (ID=" + id + ").");
            } else {
                // Atualizar
                Integer id = parseIntOrNull(idStr);
                if (id == null) {
                    alertErro("ID inválido.");
                    return;
                }
                Material m = new Material();
                m.setID(id);
                m.setNOME(nome);
                m.setQUANTIDADE(qtd);

                service.atualizar(m);
                alertInfo("Material atualizado.");
            }

            carregarTabela();
            onNovo();

        } catch (NumberFormatException nfe) {
            alertErro("Quantidade inválida. Informe um número inteiro.");
        } catch (ServiceException ex) {
            alertErro(ex.getMessage());
        } catch (Exception ex) {
            alertErro("Falha ao salvar: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private static String safeTrim(String s) {
        return (s == null) ? "" : s.trim();
    }

    private static Integer parseIntOrZero(String s) {
        if (s == null || s.isBlank()) return 0;
        return Integer.valueOf(s); // pode lançar NumberFormatException — tratado acima
    }

    private static Integer parseIntOrNull(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Integer.valueOf(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }


    @FXML
    public void onExcluir() {
        Material sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) {
            if (txtId.getText() != null && !txtId.getText().isBlank()) {
                // excluir pelo id do form
                Material m = new Material();
                m.setID(Integer.valueOf(txtId.getText()));
                excluirMaterial(m);
            } else {
                alertInfo("Selecione um material na tabela.");
            }
            return;
        }
        excluirMaterial(sel);
    }

    @FXML
    public void onMais1() { ajustar(+1); }

    @FXML
    public void onMenos1() { ajustar(-1); }

    @FXML
    public void onDefinirQtde() {
        Material sel = getMaterialSelecionadoOuForm();
        if (sel == null || sel.getID() == null) { alertInfo("Selecione/Carregue um material."); return; }

        TextInputDialog dig = new TextInputDialog(String.valueOf(sel.getQUANTIDADE() == null ? 0 : sel.getQUANTIDADE()));
        dig.setTitle("Definir quantidade");
        dig.setHeaderText("Defina a nova quantidade para: " + sel.getNOME());
        dig.setContentText("Quantidade:");

        Optional<String> resp = dig.showAndWait();
        if (resp.isEmpty()) return;

        try {
            int nova = Integer.parseInt(resp.get().trim());
            service.definirQuantidade(sel.getID(), nova);
            alertInfo("Quantidade atualizada para " + nova + ".");
            carregarTabela();
            selecionarNaTabelaPorId(sel.getID());
        } catch (NumberFormatException nfe) {
            alertErro("Valor inválido. Digite um número inteiro.");
        } catch (ServiceException ex) {
            alertErro(ex.getMessage());
        }
    }

    // --------------------------------------------------------
    // Auxiliares de ação
    // --------------------------------------------------------
    private void ajustar(int delta) {
        Material sel = getMaterialSelecionadoOuForm();
        if (sel == null || sel.getID() == null) { alertInfo("Selecione/Carregue um material."); return; }

        try {
            int nova = service.ajustarQuantidade(sel.getID(), delta);
            alertInfo("Quantidade ajustada para " + nova + ".");
            carregarTabela();
            selecionarNaTabelaPorId(sel.getID());
        } catch (ServiceException ex) {
            alertErro(ex.getMessage());
        }
    }

    private void excluirMaterial(Material m) {
        if (m == null || m.getID() == null) { alertInfo("Selecione um material válido."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir o material \"" + m.getNOME() + "\" (ID=" + m.getID() + ")?",
                ButtonType.YES, ButtonType.NO);
        conf.setHeaderText("Confirmação");
        conf.showAndWait();

        if (conf.getResult() != ButtonType.YES) return;

        try {
            service.excluir(m.getID());
            alertInfo("Material excluído.");
            carregarTabela();
            onNovo();
        } catch (ServiceException ex) {
            alertErro(ex.getMessage());
        }
    }

    private Material getMaterialSelecionadoOuForm() {
        Material sel = tabela.getSelectionModel().getSelectedItem();
        if (sel != null) return sel;

        if (txtId.getText() == null || txtId.getText().isBlank()) return null;

        Material m = new Material();
        m.setID(Integer.valueOf(txtId.getText()));
        m.setNOME(txtNome.getText());
        String q = txtQuantidade.getText();
        m.setQUANTIDADE((q == null || q.isBlank()) ? 0 : Integer.valueOf(q));
        return m;
    }

    private void preencherForm(Material m) {
        txtId.setText(m.getID() == null ? "" : String.valueOf(m.getID()));
        txtNome.setText(m.getNOME());
        txtQuantidade.setText(String.valueOf(m.getQUANTIDADE() == null ? 0 : m.getQUANTIDADE()));
    }

    private void limparForm() {
        txtId.clear();
        txtNome.clear();
        txtQuantidade.clear();
    }

    private void carregarTabela() {
        try {
            List<Material> lista = service.listar();
            dados.setAll(lista);
            aplicarFiltro(txtFiltro.getText());
        } catch (ServiceException ex) {
            alertErro(ex.getMessage());
        }
    }

    private void aplicarFiltro(String filtro) {
        if (filtro == null || filtro.isBlank()) {
            tabela.setItems(dados);
            return;
        }
        String f = filtro.toLowerCase();
        ObservableList<Material> filtrados = dados.filtered(m ->
                (m.getNOME() != null && m.getNOME().toLowerCase().contains(f))
        );
        tabela.setItems(filtrados);
    }

    private void selecionarNaTabelaPorId(Integer id) {
        if (id == null) return;
        for (Material m : tabela.getItems()) {
            if (m.getID() != null && m.getID().intValue() == id.intValue()) {
                tabela.getSelectionModel().select(m);
                tabela.scrollTo(m);
                break;
            }
        }
    }

    // --------------------------------------------------------
    // Alerts
    // --------------------------------------------------------
    private void alertErro(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Erro");
        a.showAndWait();
    }

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
