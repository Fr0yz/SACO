package controller;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.util.converter.IntegerStringConverter;
import model.ConsumoMaterial;
import model.Material;
import model.Tratamento;
import service.MaterialService;
import service.TratamentoService;
import service.TratamentoService.ServiceException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TratamentoController {

    // ===================== Componentes FXML (Form / Tabelas) =====================
    @FXML private TextField txtId;
    @FXML private TextField txtNome;
    @FXML private TextArea  txtDescricao;

    // Tabela de tratamentos
    @FXML private TableView<Tratamento> tblTratamentos;
    @FXML private TableColumn<Tratamento, Integer> colId;
    @FXML private TableColumn<Tratamento, String>  colNome;
    @FXML private TableColumn<Tratamento, String>  colDescricao;

    // Tabela de consumo (material x quantidade)
    @FXML private TableView<ConsumoVM> tblMateriaisVinculados;
    @FXML private TableColumn<ConsumoVM, String>  colMatNome;
    @FXML private TableColumn<ConsumoVM, Integer> colMatQtd;

    // Inclusão de material
    @FXML private ComboBox<Material>  cmbMateriaisDisponiveis;
    @FXML private Spinner<Integer>    spQuantidade;

    @FXML private Label lblStatus;

    // ===================== Services =====================
    private final TratamentoService tratamentoService = new TratamentoService();
    private final MaterialService   materialService   = new MaterialService();

    // ===================== Observable Lists =====================
    private final ObservableList<Tratamento> tratamentosObs  = FXCollections.observableArrayList();
    private final ObservableList<ConsumoVM>  consumosObs     = FXCollections.observableArrayList();
    private final ObservableList<Material>   materiaisObs    = FXCollections.observableArrayList();

    // ===================== Estado da tela =====================
    // >>> id do tratamento atualmente em edição (0 = novo, ainda não persistido)
    private int idTratamentoAtual = 0;

    // ===================== ViewModel de Consumo =====================
    public static class ConsumoVM {
        private final Material material;
        private int quantidade;

        public ConsumoVM(Material material, int quantidade) {
            this.material = material;
            this.quantidade = quantidade;
        }
        public Material getMaterial()          { return material; }
        public String   getNomeMaterial()      { return material.getNOME(); }
        public int      getQuantidade()        { return quantidade; }
        public void     setQuantidade(int q)   { this.quantidade = q; }
        public int      getIdMaterial()        { return material.getID(); }

        @Override public String toString() { return getNomeMaterial() + " x" + quantidade; }
    }

    // ===================== Inicialização =====================
    @FXML
    public void initialize() {
        try {
            // Colunas da Tabela de Tratamentos
            colId.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getId_tratamento()).asObject());
            colNome.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNome()));
            colDescricao.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getDescricao()));

            tblTratamentos.setItems(tratamentosObs);

            // Colunas da Tabela de Materiais Vinculados
            colMatNome.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getNomeMaterial()));
            colMatQtd.setCellValueFactory(d -> new SimpleIntegerProperty(d.getValue().getQuantidade()).asObject());

            // Edição inline da quantidade
            tblMateriaisVinculados.setEditable(true);
            colMatQtd.setCellFactory(tc -> new TextFieldTableCell<>(new IntegerStringConverter()));
            colMatQtd.setOnEditCommit(evt -> {
                ConsumoVM vm = evt.getRowValue();
                Integer novo = evt.getNewValue();
                if (novo == null || novo <= 0) {
                    alertErro("Quantidade deve ser maior que zero.");
                    tblMateriaisVinculados.refresh();
                    return;
                }
                vm.setQuantidade(novo);
                tblMateriaisVinculados.refresh();
                lblStatus.setText("Quantidade atualizada: " + vm.getNomeMaterial() + " -> " + novo);

                // >>> Persistir edição inline (se já houver tratamento salvo)
                if (idTratamentoAtual > 0) {
                    Task<Void> t = new Task<>() {
                        @Override protected Void call() throws Exception {
                            tratamentoService.upsertConsumo(idTratamentoAtual, vm.getIdMaterial(), novo);
                            return null;
                        }
                    };
                    t.setOnFailed(e -> alertErro("Falha ao salvar quantidade: " + t.getException().getMessage()));
                    new Thread(t).start();
                }
            });

            tblMateriaisVinculados.setItems(consumosObs);

            // Combo de materiais disponíveis
            cmbMateriaisDisponiveis.setItems(materiaisObs);
            cmbMateriaisDisponiveis.setConverter(new javafx.util.StringConverter<>() {
                @Override public String toString(Material m) { return (m == null ? "" : m.getNOME()); }
                @Override public Material fromString(String s) { return null; }
            });

            // Spinner: 1..999, default 1
            spQuantidade.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));

            // Carrega dados iniciais
            carregarTabela();
            carregarMateriaisDisponiveis();

            lblStatus.setText("Pronto.");
        } catch (Exception e) {
            alertErro("Erro ao inicializar: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ===================== Ações (Botões) =====================

    /** Novo cadastro / limpa formulário */
    @FXML
    public void onNovo() {
        txtId.clear();
        txtNome.clear();
        txtDescricao.clear();
        consumosObs.clear();
        tblTratamentos.getSelectionModel().clearSelection();
        // >>> reset do estado: ainda sem id
        idTratamentoAtual = 0;
        lblStatus.setText("Novo tratamento.");
    }

    /** Salvar (criar/atualizar) — agora em background */
    @FXML
    public void onSalvar() {
        String nome = safeTrim(txtNome.getText());
        String descricao = safeTrim(txtDescricao.getText());

        List<ConsumoMaterial> consumos = new ArrayList<>();
        for (ConsumoVM vm : consumosObs) {
            if (vm.getQuantidade() <= 0) {
                alertErro("Quantidade inválida para: " + vm.getNomeMaterial());
                return;
            }
            consumos.add(new ConsumoMaterial(vm.getIdMaterial(), vm.getQuantidade()));
        }

        // >>> Executa service em Task para não travar a UI
        Task<Void> t = new Task<>() {
            @Override protected Void call() throws Exception {
                if (txtId.getText() == null || txtId.getText().isBlank() || idTratamentoAtual <= 0) {
                    long idGerado = tratamentoService.criar(nome, descricao, consumos);
                    idTratamentoAtual = (int) idGerado; // >>> guarda id
                } else {
                    int idTrat = Integer.parseInt(txtId.getText());
                    tratamentoService.atualizar(idTrat, nome, descricao, consumos);
                    idTratamentoAtual = idTrat; // >>> garante consistência
                }
                return null;
            }
        };
        t.setOnSucceeded(ev -> {
            alertInfo((txtId.getText() == null || txtId.getText().isBlank()) ?
                    ("Tratamento criado (ID=" + idTratamentoAtual + ").")
                    : "Tratamento atualizado.");
            // >>> reflete o id no campo
            txtId.setText(String.valueOf(idTratamentoAtual));
            carregarTabela();
        });
        t.setOnFailed(ev -> {
            Throwable ex = t.getException();
            if (ex instanceof ServiceException) {
                alertErro(ex.getMessage());
            } else if (ex instanceof NumberFormatException) {
                alertErro("ID inválido.");
            } else {
                alertErro("Falha ao salvar: " + (ex != null ? ex.getMessage() : ""));
                ex.printStackTrace();
            }
        });
        new Thread(t).start();
    }

    /** Excluir tratamento selecionado */
    @FXML
    public void onExcluir() {
        Tratamento selecionado = tblTratamentos.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            alertErro("Selecione um tratamento para excluir.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir o tratamento \"" + selecionado.getNome() + "\"?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmação");
        confirm.showAndWait();

        if (confirm.getResult() == ButtonType.YES) {
            // >>> excluir em background
            Task<Void> t = new Task<>() {
                @Override protected Void call() throws Exception {
                    tratamentoService.excluir(selecionado.getId_tratamento());
                    return null;
                }
            };
            t.setOnSucceeded(e -> {
                alertInfo("Tratamento excluído.");
                carregarTabela();
                onNovo();
            });
            t.setOnFailed(e -> alertErro("Erro ao excluir: " + t.getException().getMessage()));
            new Thread(t).start();
        }
    }

    /** Seleciona um tratamento e carrega seus dados + consumos */
    @FXML
    public void onSelecionarTratamento(MouseEvent event) {
        Tratamento t = tblTratamentos.getSelectionModel().getSelectedItem();
        if (t == null) return;

        txtId.setText(String.valueOf(t.getId_tratamento()));
        txtNome.setText(t.getNome());
        txtDescricao.setText(t.getDescricao());

        // >>> fixa o id atual para operações de upsert/remover
        idTratamentoAtual = t.getId_tratamento();

        carregarMateriaisVinculadosDoTratamento(t.getId_tratamento());
        lblStatus.setText("Editando tratamento ID " + t.getId_tratamento());
    }

    /** Adiciona/atualiza material com quantidade (UI + upsert se já houver ID) */
    @FXML
    public void onAdicionarMaterial() {
        Material mat = cmbMateriaisDisponiveis.getSelectionModel().getSelectedItem();
        Integer qtd  = spQuantidade.getValue();

        if (mat == null) { lblStatus.setText("Selecione um material."); return; }
        if (qtd == null || qtd <= 0) { lblStatus.setText("Quantidade deve ser maior que zero."); return; }

        // Atualiza a lista local (UI)
        ConsumoVM existente = null;
        for (ConsumoVM vm : consumosObs) {
            if (Objects.equals(vm.getIdMaterial(), mat.getID())) {
                existente = vm; break;
            }
        }
        int quantidadeFinal;
        if (existente != null) {
            quantidadeFinal = existente.getQuantidade() + qtd;
            existente.setQuantidade(quantidadeFinal);
            tblMateriaisVinculados.refresh();
            lblStatus.setText("Quantidade atualizada: " + mat.getNOME());
        } else {
            consumosObs.add(new ConsumoVM(mat, qtd));
            quantidadeFinal = qtd;
            lblStatus.setText("Material adicionado: " + mat.getNOME());
        }

        // >>> Se já existe tratamento persistido, faz upsert agora; senão, deixa para o Salvar
        if (idTratamentoAtual > 0) {
            Task<Void> t = new Task<>() {
                @Override protected Void call() throws Exception {
                    tratamentoService.upsertConsumo(idTratamentoAtual, mat.getID(), quantidadeFinal);
                    return null;
                }
            };
            t.setOnFailed(ev -> {
                Throwable ex = t.getException();
                lblStatus.setText("Falha ao salvar consumo: " + (ex != null ? ex.getMessage() : ""));
                // opcional: desfazer alteração na UI se precisar
            });
            new Thread(t).start();
        } else {
            lblStatus.setText("Material lançado na tela (será salvo ao gravar o tratamento).");
        }
    }

    /** Remove material selecionado (UI + exclusão do vínculo, se já houver ID) */
    @FXML
    public void onRemoverMaterial() {
        ConsumoVM sel = tblMateriaisVinculados.getSelectionModel().getSelectedItem();
        if (sel == null) {
            lblStatus.setText("Selecione um item para remover.");
            return;
        }
        consumosObs.remove(sel);
        lblStatus.setText("Material removido: " + sel.getNomeMaterial());

        // >>> Se o tratamento já existe no banco, remove o vínculo imediatamente
        if (idTratamentoAtual > 0) {
            Task<Void> t = new Task<>() {
                @Override protected Void call() throws Exception {
                    tratamentoService.excluirMaterialDoTratamento(idTratamentoAtual, sel.getIdMaterial());
                    return null;
                }
            };
            t.setOnFailed(e -> alertErro("Erro ao remover material do tratamento: " + t.getException().getMessage()));
            new Thread(t).start();
        }
    }

    // ===================== Apoio: carregamentos =====================

    /** Carrega todos os tratamentos e atualiza a tabela */
    @FXML
    public void carregarTabela() {
        // >>> background load
        Task<List<Tratamento>> t = new Task<>() {
            @Override protected List<Tratamento> call() throws Exception {
                return tratamentoService.listar();
            }
        };
        t.setOnSucceeded(e -> tratamentosObs.setAll(t.getValue()));
        t.setOnFailed(e -> alertErro("Erro ao carregar tratamentos: " + t.getException().getMessage()));
        new Thread(t).start();
    }

    /** Carrega todos os materiais disponíveis para o ComboBox */
    private void carregarMateriaisDisponiveis() {
        // >>> background load
        Task<List<Material>> t = new Task<>() {
            @Override protected List<Material> call() throws Exception {
                return materialService.listar(); // >>> usa o field existente
            }
        };
        t.setOnSucceeded(e -> {
            materiaisObs.setAll(t.getValue());
            lblStatus.setText("Materiais carregados: " + materiaisObs.size());
        });
        t.setOnFailed(e -> {
            Throwable ex = t.getException();
            throw new RuntimeException(ex != null ? ex : new RuntimeException("Erro desconhecido ao carregar materiais"));
        });
        new Thread(t).start();
    }

    /** Carrega materiais já vinculados a um tratamento (nome + quantidade) */
    private void carregarMateriaisVinculadosDoTratamento(int idTratamento) {
        // >>> background load
        Task<List<Material>> t = new Task<>() {
            @Override protected List<Material> call() throws Exception {
                return tratamentoService.listarMateriaisPorTratamento(idTratamento);
            }
        };
        t.setOnSucceeded(e -> {
            consumosObs.clear();
            for (Material m : t.getValue()) {
                Integer qtd = (m.getQtdConsumo() == null ? 0 : m.getQtdConsumo());
                if (qtd > 0) {
                    consumosObs.add(new ConsumoVM(m, qtd));
                }
            }
        });
        t.setOnFailed(e -> alertErro("Erro ao carregar materiais do tratamento: " + t.getException().getMessage()));
        new Thread(t).start();
    }

    // ===================== Utils =====================

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setHeaderText(null);
        a.setTitle("Informação");
        a.setContentText(msg);
        a.showAndWait();
    }

    private void alertErro(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setHeaderText("Erro");
        a.setTitle("Erro");
        a.setContentText(msg);
        a.showAndWait();
    }

    private static String safeTrim(String s) { return (s == null) ? "" : s.trim(); }
}
