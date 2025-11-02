package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;
import model.Anamnese;
import model.Pessoa;
import service.AnamneseService;
import service.CadastroPessoaService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Controller da tela de Anamnese.
 * - Carrega pacientes (ComboBox ID - Nome)
 * - Busca/preenche anamnese do paciente selecionado
 * - Salva/Atualiza anamnese + odontograma (LONGBLOB)
 * - Tabela de anamneses com colunas Data/Alergias/Histórico/Medicamentos/Detalhes/Imagem?
 */
public class AnamneseController {

    // ====== UI: Formulário ======
    @FXML private ComboBox<Pessoa> cbPaciente;

    @FXML private TextArea txtAlergias;
    @FXML private TextArea txtHistorico;
    @FXML private TextArea txtMedicamentos;
    @FXML private TextArea txtDetalhes;

    @FXML private CheckBox chkOdontoGrama;
    @FXML private Label lblOdontoGrama;
    @FXML private ImageView imgOdonto;

    // ====== UI: Tabela de Anamneses ======
    @FXML private TableView<Anamnese> tabela;

    @FXML private TableColumn<Anamnese, Date>   colData;
    @FXML private TableColumn<Anamnese, String> colAlerg;
    @FXML private TableColumn<Anamnese, String> colHist;
    @FXML private TableColumn<Anamnese, String> colMed;
    @FXML private TableColumn<Anamnese, String> colDet;
    @FXML private TableColumn<Anamnese, Void>   colImg;
    @FXML private TableColumn<Anamnese, Void> colAcoes;

    // ====== Services ======
    private final CadastroPessoaService pessoaService = new CadastroPessoaService();
    private final AnamneseService anamneseService = new AnamneseService();

    // Guarda bytes atuais da imagem (para salvar/limpar)
    private byte[] odontogramaBytes;

    // ====== Init ======
    @FXML
    public void initialize() {
        configurarBindingsOdontograma();
        configurarComboPacientes();
        configurarColunasTabela();
        configurarColunaAcoes();
        carregarPacientesIdNome();
        onListar();
    }

    /* ==========================================================
       Bindings: mostra/oculta nós do odontograma com o checkbox
       ========================================================== */
    private void configurarBindingsOdontograma() {
        for (Node n : new Node[]{ lblOdontoGrama, imgOdonto }) {
            n.managedProperty().bind(n.visibleProperty());
            n.visibleProperty().bind(chkOdontoGrama.selectedProperty());
        }
    }

    /* ==========================================================
       Combo de Pacientes: ID - Nome, converter e célula custom
       ========================================================== */
    private void configurarComboPacientes() {
        cbPaciente.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(Pessoa item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getId_pessoa() + " - " + item.getNome());
            }
        });
        cbPaciente.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Pessoa item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getId_pessoa() + " - " + item.getNome());
            }
        });
        cbPaciente.setConverter(new StringConverter<>() {
            @Override public String toString(Pessoa p) { return p == null ? "" : p.getId_pessoa() + " - " + p.getNome(); }
            @Override public Pessoa fromString(String s) {
                if (s == null) return null;
                return cbPaciente.getItems().stream()
                        .filter(p -> (p.getId_pessoa() + " - " + p.getNome()).equals(s))
                        .findFirst().orElse(null);
            }
        });
    }

    /** Carrega pacientes (somente quem está em TB_PACIENTE) no ComboBox. */
    public void carregarPacientesIdNome() {
        try {
            List<Pessoa> pessoas = pessoaService.listarPacientes();
            ObservableList<Pessoa> dados = FXCollections.observableArrayList(pessoas);
            cbPaciente.setItems(dados);
            cbPaciente.setPromptText("Selecione um paciente");
        } catch (SQLException e) {
            e.printStackTrace();
            alertErro("Erro ao carregar pacientes: " + e.getMessage());
        }
    }

    /* ==========================================================
       Tabela: configuração de colunas
       ========================================================== */
    private void configurarColunasTabela() {
        // Data (Date -> dd/MM/yyyy HH:mm)
        colData.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(c.getValue().getData_registro()));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colData.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                LocalDateTime ldt = item.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
                setText(fmt.format(ldt));
            }
        });

        // Textos (com resumo/primeira linha e sem quebras)
        colAlerg.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullSafe(c.getValue().getAlergias())));
        colHist .setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullSafe(c.getValue().getHistorico_medico())));
        colMed  .setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullSafe(c.getValue().getMedicamentos())));
        colDet  .setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nullSafe(c.getValue().getDetalhes())));

        java.util.function.Function<String,String> resumo = s -> {
            if (s == null) return "";
            s = s.strip().replaceAll("\\R+", " "); // quebra de linha -> espaço
            return s.length() > 80 ? s.substring(0, 77) + "..." : s;
        };
        java.util.function.Consumer<TableColumn<Anamnese,String>> aplicarResumo = col -> {
            col.setCellFactory(c -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : resumo.apply(item));
                }
            });
        };
        aplicarResumo.accept(colAlerg);
        aplicarResumo.accept(colHist);
        aplicarResumo.accept(colMed);
        aplicarResumo.accept(colDet);

        // Imagem? (odontograma presente)
        colImg.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setText(null); return; }
                Anamnese a = getTableView().getItems().get(getIndex());
                setText(a.getImagem_odontograma() != null ? "Sim" : "Não");
            }
        });
    }

    /* ==========================================================
       Disparado pelo ComboBox (onAction no FXML)
       ========================================================== */
    @FXML
    private void onPacienteSelecionado() {
        Pessoa selecionado = cbPaciente.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;

        try {
            Anamnese a = anamneseService.buscarPorPaciente(selecionado.getId_pessoa());
            if (a != null) {
                txtAlergias.setText(nullSafe(a.getAlergias()));
                txtHistorico.setText(nullSafe(a.getHistorico_medico()));
                txtMedicamentos.setText(nullSafe(a.getMedicamentos()));
                txtDetalhes.setText(nullSafe(a.getDetalhes()));
                setOdontogramaBytes(a.getImagem_odontograma());
                chkOdontoGrama.setSelected(a.getImagem_odontograma() != null);

                tabela.setItems(FXCollections.observableArrayList(a)); // 0/1 item
            } else {
                limparCampos(false);                 // limpa campos, mantém paciente selecionado
                tabela.getItems().clear();           // sem anamnese
            }
        } catch (SQLException e) {
            e.printStackTrace();
            alertErro("Erro ao buscar anamnese: " + e.getMessage());
        }
    }

    /* ==========================================================
       Botões: Carregar imagem / Remover imagem
       ========================================================== */
    @FXML
    private void onCarregarImagem() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Selecionar imagem do Odontograma");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagens (PNG/JPG)", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fc.showOpenDialog(cbPaciente.getScene().getWindow());
        if (file == null) return;

        // Limite de 5 MB
        final long MAX_BYTES = 5L * 1024 * 1024;
        if (file.length() > MAX_BYTES) {
            alertErro("Arquivo muito grande (máx. 5MB).");
            return;
        }

        String name = file.getName().toLowerCase();
        if (!(name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"))) {
            alertErro("Formato não suportado. Use PNG ou JPG.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            odontogramaBytes = fis.readAllBytes();
            imgOdonto.setImage(new Image(new ByteArrayInputStream(odontogramaBytes)));
            chkOdontoGrama.setSelected(true); // força visibilidade
        } catch (Exception ex) {
            ex.printStackTrace();
            alertErro("Falha ao carregar imagem: " + ex.getMessage());
        }
    }


    @FXML
    private void onRemoverImagem() {
        odontogramaBytes = null;
        imgOdonto.setImage(null);
        chkOdontoGrama.setSelected(false);
    }

    /** Seta bytes vindos do banco e reflete na ImageView. */
    private void setOdontogramaBytes(byte[] bytes) {
        odontogramaBytes = bytes;
        imgOdonto.setImage(bytes == null ? null : new Image(new ByteArrayInputStream(bytes)));
    }

    /* ==========================================================
       Ações principais: Salvar / Limpar / Listar
       ========================================================== */
    @FXML
    private void onSalvar() {
        Pessoa paciente = cbPaciente.getSelectionModel().getSelectedItem();
        if (paciente == null) { alertErro("Selecione um paciente."); return; }

        try {
            Anamnese a = new Anamnese();
            a.id_paciente = paciente.getId_pessoa();
            a.alergias = emptyToNull(txtAlergias.getText());
            a.historico_medico = emptyToNull(txtHistorico.getText());
            a.medicamentos = emptyToNull(txtMedicamentos.getText());
            a.detalhes = emptyToNull(txtDetalhes.getText());
            a.imagem_odontograma = chkOdontoGrama.isSelected() ? odontogramaBytes : null;

            anamneseService.salvarOuAtualizar(a);
            alertInfo("Anamnese salva com sucesso.");

            // Recarrega a seleção (atualiza campos, imagem e tabela 0/1)
            onPacienteSelecionado();

        } catch (SQLException e) {
            e.printStackTrace();
            alertErro("Erro ao salvar anamnese: " + e.getMessage());
        }
    }

    @FXML
    private void onLimpar() {
        limparCampos(true);
        tabela.getItems().clear();
    }

    /** Lista TODAS as anamneses (independente do paciente). */
    @FXML
    private void onListar() {
        try {
            var lista = anamneseService.listarTodas();
            tabela.setItems(FXCollections.observableArrayList(lista));
        } catch (SQLException e) {
            alertErro("Erro ao listar anamneses: " + e.getMessage());
        }
    }

    /* ==========================================================
       Utilidades
       ========================================================== */
    private void limparCampos(boolean limparPaciente) {
        if (limparPaciente) cbPaciente.getSelectionModel().clearSelection();
        txtAlergias.clear();
        txtHistorico.clear();
        txtMedicamentos.clear();
        txtDetalhes.clear();
        onRemoverImagem();
    }

    private void alertErro(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Ops…");
        a.showAndWait();
    }

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    // ==========================================================
// Coluna de Ações (Abrir, Baixar, Excluir Img)
// ==========================================================
    private void configurarColunaAcoes() {
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnAbrir = new Button("Abrir");
            private final Button btnBaixar = new Button("Baixar");
            private final Button btnExcluirImg = new Button("Excluir Img");
            private final HBox box = new HBox(6, btnAbrir, btnBaixar, btnExcluirImg);

            {
                btnAbrir.setOnAction(e -> {
                    Anamnese a = getTableView().getItems().get(getIndex());
                    if (a.getImagem_odontograma() == null) { alertInfo("Sem imagem neste registro."); return; }
                    ImageView iv = new ImageView(new Image(new ByteArrayInputStream(a.getImagem_odontograma())));
                    iv.setPreserveRatio(true);
                    iv.setFitWidth(520);
                    iv.setFitHeight(300);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setHeaderText("Odontograma");
                    alert.getDialogPane().setContent(iv);
                    alert.getButtonTypes().setAll(ButtonType.CLOSE);
                    alert.showAndWait();
                });

                btnBaixar.setOnAction(e -> {
                    Anamnese a = getTableView().getItems().get(getIndex());
                    if (a.getImagem_odontograma() == null) { alertInfo("Sem imagem para baixar."); return; }
                    FileChooser fc = new FileChooser();
                    fc.setTitle("Salvar odontograma");
                    fc.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("PNG", "*.png"),
                            new FileChooser.ExtensionFilter("JPEG", "*.jpg", "*.jpeg")
                    );
                    // Sugestão de nome
                    fc.setInitialFileName("odontograma_" + a.getId_anamnese() + ".png");
                    File file = fc.showSaveDialog(getTableView().getScene().getWindow());
                    if (file == null) return;
                    try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
                        fos.write(a.getImagem_odontograma());
                        alertInfo("Arquivo salvo com sucesso.");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        alertErro("Falha ao salvar arquivo: " + ex.getMessage());
                    }
                });

                btnExcluirImg.setOnAction(e -> {
                    Anamnese a = getTableView().getItems().get(getIndex());
                    if (a.getImagem_odontograma() == null) { alertInfo("Este registro já não possui imagem."); return; }
                    try {
                        // Se houver método especializado:
                        // anamneseService.removerImagemOdontograma(a.getId_anamnese());
                        // Workaround com salvarOuAtualizar:
                        Anamnese clone = new Anamnese();
                        clone.id_anamnese = a.getId_anamnese();
                        clone.id_paciente = a.getId_paciente();
                        clone.alergias = a.getAlergias();
                        clone.historico_medico = a.getHistorico_medico();
                        clone.medicamentos = a.getMedicamentos();
                        clone.detalhes = a.getDetalhes();
                        clone.imagem_odontograma = null;

                        anamneseService.salvarOuAtualizar(clone);
                        alertInfo("Imagem removida do registro.");
                        onPacienteSelecionado(); // atualiza form se for o mesmo paciente
                        onListar();              // atualiza a tabela geral
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                        alertErro("Erro ao remover imagem: " + ex.getMessage());
                    }
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }
    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }

    @FXML
    private void onRemoverImagemBD() {
        Pessoa paciente = cbPaciente.getSelectionModel().getSelectedItem();
        if (paciente == null) { alertErro("Selecione um paciente."); return; }

        try {
            Anamnese a = anamneseService.buscarPorPaciente(paciente.getId_pessoa());
            if (a == null) { alertInfo("Não há anamnese para este paciente."); return; }

            // Se houver método especializado no service, prefira-o:
            // anamneseService.removerImagemPorPaciente(paciente.getId_pessoa());

            // Caso não: salva a anamnese com imagem nula (mantendo os demais campos)
            Anamnese novo = new Anamnese();
            novo.id_anamnese = a.getId_anamnese();
            novo.id_paciente = a.getId_paciente();
            novo.alergias = a.getAlergias();
            novo.historico_medico = a.getHistorico_medico();
            novo.medicamentos = a.getMedicamentos();
            novo.detalhes = a.getDetalhes();
            novo.imagem_odontograma = null;

            anamneseService.salvarOuAtualizar(novo);

            // Atualiza UI local
            onRemoverImagem(); // limpa bytes e esconde na UI
            onPacienteSelecionado(); // recarrega
            alertInfo("Imagem removida do banco para este paciente.");

        } catch (SQLException e) {
            e.printStackTrace();
            alertErro("Erro ao remover imagem: " + e.getMessage());
        }
    }


    @FXML
    private void onExcluirPacienteAnamnese() {
        Pessoa paciente = cbPaciente.getSelectionModel().getSelectedItem();
        if (paciente == null) { alertErro("Selecione um paciente."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir a anamnese do paciente selecionado?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Confirmação");
        confirm.showAndWait();

        if (confirm.getResult() != ButtonType.YES) return;

        try {
            // Se o service tiver método específico:
            // anamneseService.excluirPorPaciente(paciente.getId_pessoa());

            // Workaround: buscar e excluir por id
            Anamnese a = anamneseService.buscarPorPaciente(paciente.getId_pessoa());
            if (a == null) { alertInfo("Este paciente não possui anamnese."); return; }

            anamneseService.excluirPorId(a.getId_anamnese()); // implemente no service se não existir
            alertInfo("Anamnese excluída.");

            limparCampos(false);
            onListar();

        } catch (SQLException e) {
            e.printStackTrace();
            alertErro("Erro ao excluir: " + e.getMessage());
        }
    }

}
