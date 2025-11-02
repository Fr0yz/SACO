package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import model.Dentista;
import model.Pessoa;
import service.CadastroPessoaService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * Controller da tela de Cadastro de Paciente/Dentista.
 * Agora utiliza CadastroPessoaService (camada de regras) em vez de DAO direto.
 */
public class PacienteController {

    // ==========================
    //  Componentes da UI (FXML)
    // ==========================
    @FXML private Button btnSalvar;

    @FXML private TextField txtNome;
    @FXML private TextField txtCpf;
    @FXML private TextField txtTelefone;
    @FXML private TextField txtEmail;
    @FXML private DatePicker dpNascimento;

    @FXML private CheckBox chkDentista;
    @FXML private TextField txtCro;
    @FXML private TextField txtEspecialidade;
    @FXML private Label lblCro;
    @FXML private Label lblEsp;

    @FXML private TableView<Pessoa> tabela;
    @FXML private TableColumn<Pessoa, String> colNome;
    @FXML private TableColumn<Pessoa, String> colCpf;
    @FXML private TableColumn<Pessoa, String> colTel;
    @FXML private TableColumn<Pessoa, Date>   colNasc;
    @FXML private TableColumn<Pessoa, Void>   colAcoes;

    // ==========================
    //  Estado e Serviço (camada de regras)
    // ==========================
    private Pessoa pacienteEmEdicao = null;
    private final CadastroPessoaService service = new CadastroPessoaService();

    // ==========================
    //  Ciclo de Vida (init)
    // ==========================
    @FXML
    public void initialize() {
        configurarColunas();
        carregarTabela();
        configurarCamposDentista();
        addAcoesButtons();
        configurarCelulasTabela();
        configurarMascaras();

        // Se desmarcar "É Dentista?", limpa CRO/Especialidade
        chkDentista.selectedProperty().addListener((o, was, isNow) -> {
            if (!isNow) { txtCro.clear(); txtEspecialidade.clear(); }
        });
    }

    // =====================================================
    //  Coluna de Ações (Editar/Excluir por registro)
    // =====================================================
    private void addAcoesButtons() {
        colAcoes.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar = new Button("Editar");
            private final Button btnExcluir = new Button("Excluir");
            private final HBox box = new HBox(8, btnEditar, btnExcluir);

            {
                btnEditar.setOnAction(e -> {
                    Pessoa p = getRowItem();
                    if (p == null) return;
                    try {
                        Dentista d = service.buscarDentista(p.id_pessoa); // via Service
                        carregarNoFormulario(p, d);
                        pacienteEmEdicao = p;
                        if (btnSalvar != null) btnSalvar.setText("Atualizar");
                    } catch (SQLException ex) {
                        alertErro("Erro ao carregar dados do dentista: " + ex.getMessage());
                    }
                });

                btnExcluir.setOnAction(e -> {
                    Pessoa p = getRowItem();
                    if (p == null) return;
                    Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                            "Excluir o paciente \"" + p.getNome() + "\"?",
                            ButtonType.YES, ButtonType.NO);
                    conf.setHeaderText("Confirmação de exclusão");
                    conf.showAndWait().ifPresent(bt -> {
                        if (bt == ButtonType.YES) {
                            try {
                                service.excluir(p.id_pessoa); // via Service
                                getTableView().getItems().remove(p);
                                alertInfo("Registro excluído.");
                            } catch (SQLException ex) {
                                alertErro("Erro ao excluir: " + ex.getMessage());
                            }
                        }
                    });
                });
            }

            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }

            private Pessoa getRowItem() {
                return (getTableRow() == null) ? null : getTableRow().getItem();
            }
        });
    }

    // ============================================
    //  Preenchimento do formulário para EDIÇÃO
    // ============================================
    private void carregarNoFormulario(Pessoa p, Dentista d) {
        txtNome.setText(p.getNome());
        txtCpf.setText(p.getCpf());
        txtTelefone.setText(p.getTelefone());
        txtEmail.setText(p.getEmail());

        if (p.dt_nascimento != null) {
            LocalDate ld = (p.dt_nascimento instanceof java.sql.Date)
                    ? ((java.sql.Date) p.dt_nascimento).toLocalDate()
                    : p.dt_nascimento.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            dpNascimento.setValue(ld);
        } else {
            dpNascimento.setValue(null);
        }

        boolean ehDentista = (d != null);
        chkDentista.setSelected(ehDentista);

        if (ehDentista) {
            txtCro.setText(d.cro != null ? d.cro : "");
            txtEspecialidade.setText(d.especialidade != null ? d.especialidade : "");
        } else {
            txtCro.clear();
            txtEspecialidade.clear();
        }
    }

    // =====================================================
    //  Ações (Salvar, Limpar, Listar)
    // =====================================================
    @FXML
    public void onSalvar() {
        try {
            // Validações mínimas
            if (txtNome.getText().isBlank()) { alertErro("Informe o nome."); return; }
            String cpfDigits = txtCpf.getText().replaceAll("\\D", "");
            if (cpfDigits.length() != 11) { alertErro("CPF deve ter 11 dígitos numéricos."); return; }

            String telDigits = txtTelefone.getText().replaceAll("\\D", "");
            if (!telDigits.isBlank() && (telDigits.length() < 10 || telDigits.length() > 11)) {
                alertErro("Telefone deve ter 10 ou 11 dígitos (DDD + número)."); return;
            }
            if (chkDentista.isSelected()) {
                if (txtCro.getText().isBlank()) { alertErro("Informe o CRO do dentista."); return; }
                if (txtEspecialidade.getText().isBlank()) { alertErro("Informe a especialidade do dentista."); return; }
            }

            // Monta objeto (novo ou edição)
            final boolean isEdicao = (pacienteEmEdicao != null);
            Pessoa p = isEdicao ? pacienteEmEdicao : new Pessoa();

            p.nome      = txtNome.getText().trim();
            p.cpf       = cpfDigits;
            p.telefone  = telDigits;
            p.email     = txtEmail.getText().trim();
            p.dentista  = chkDentista.isSelected();

            LocalDate ld = dpNascimento.getValue();
            p.dt_nascimento = (ld == null) ? null :
                    Date.from(ld.atStartOfDay(ZoneId.systemDefault()).toInstant());

            Dentista d = null;
            if (p.dentista) {
                d = new Dentista();
                d.cro = txtCro.getText().trim();
                d.especialidade = txtEspecialidade.getText().trim();
            }

            // Persistência via Service
            if (isEdicao) {
                service.atualizar(p, d); // UPDATE pessoa + upsert dentista
                alertInfo("Registro atualizado com sucesso.");
            } else {
                int novoId = Math.toIntExact(service.inserir(p, d)); // INSERT
                p.id_pessoa = novoId;
                alertInfo("Registro salvo com sucesso.");
            }

            limparCampos();
            carregarTabela();
            if (btnSalvar != null) btnSalvar.setText("Salvar");
            pacienteEmEdicao = null;

        } catch (SQLException e) {
            e.printStackTrace();
            alertErro("Erro ao salvar: " + e.getMessage());
        }
    }

    @FXML public void onLimpar() { limparCampos(); }
    @FXML public void onListar() { carregarTabela(); }

    // ============================================
    //  Tabela: listar e configurar colunas
    // ============================================
    private void carregarTabela() {
        try {
            List<Pessoa> pessoas = service.listarTodos(); // via Service
            ObservableList<Pessoa> dados = FXCollections.observableArrayList(pessoas);
            tabela.setItems(dados);
        } catch (SQLException e) {
            e.printStackTrace();
            alertErro("Erro ao carregar dados: " + e.getMessage());
        }
    }

    private void configurarColunas() {
        colNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colCpf.setCellValueFactory(new PropertyValueFactory<>("cpf"));
        colTel.setCellValueFactory(new PropertyValueFactory<>("telefone"));
        colNasc.setCellValueFactory(new PropertyValueFactory<>("dt_nascimento"));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        colNasc.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Date item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                LocalDate ld = (item instanceof java.sql.Date)
                        ? ((java.sql.Date) item).toLocalDate()
                        : item.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                setText(fmt.format(ld));
            }
        });
    }

    // ============================================
    //  UI Utils
    // ============================================
    private void limparCampos() {
        txtNome.clear();
        txtCpf.clear();
        txtTelefone.clear();
        txtEmail.clear();
        dpNascimento.setValue(null);
        chkDentista.setSelected(false);
        txtCro.clear();
        txtEspecialidade.clear();
        txtNome.requestFocus();
        btnSalvar.setText("Salvar");
    }

    private void alertInfo(String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void alertErro(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText("Ops…");
        a.showAndWait();
    }

    /**
     * Liga a visibilidade/managed dos campos de dentista ao estado do checkbox.
     * Quando desmarcado, os nós "somem" do layout (managed=false).
     */
    private void configurarCamposDentista() {
        for (Node n : new Node[]{ lblCro, txtCro, lblEsp, txtEspecialidade }) {
            n.managedProperty().bind(n.visibleProperty());
            n.visibleProperty().bind(chkDentista.selectedProperty());
        }
    }

    // ========= Máscaras de entrada =========
    private void configurarMascaras() {
        aplicarMascaraCpf(txtCpf);
        aplicarMascaraTelefone(txtTelefone);
    }

    private void aplicarMascaraCpf(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (change.isAdded() || change.isReplaced() || change.isDeleted()) {
                String digits = change.getControlNewText().replaceAll("\\D", "");
                if (digits.length() > 11) return null;
                String formatted = formatCpf(digits);
                change.setText(formatted);
                change.setRange(0, change.getControlText().length());
                change.setCaretPosition(formatted.length());
                change.setAnchor(formatted.length());
                return change;
            }
            return change;
        }));
    }

    private void aplicarMascaraTelefone(TextField field) {
        field.setTextFormatter(new TextFormatter<>(change -> {
            if (change.isAdded() || change.isReplaced() || change.isDeleted()) {
                String digits = change.getControlNewText().replaceAll("\\D", "");
                if (digits.length() > 11) return null;
                String formatted = formatTelefone(digits);
                change.setText(formatted);
                change.setRange(0, change.getControlText().length());
                change.setCaretPosition(formatted.length());
                change.setAnchor(formatted.length());
                return change;
            }
            return change;
        }));
    }

    // ========= Helpers de formatação =========
    private String formatCpf(String digits) {
        int n = digits.length();
        if (n == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            if (i == 3 || i == 6) sb.append('.');
            if (i == 9) sb.append('-');
            sb.append(digits.charAt(i));
        }
        return sb.toString();
    }

    private String formatTelefone(String digits) {
        int n = digits.length();
        if (n == 0) return "";
        StringBuilder sb = new StringBuilder();
        if (n >= 1) {
            sb.append('(').append(digits.charAt(0));
            if (n >= 2) sb.append(digits.charAt(1)).append(") ");
        }
        if (n <= 2) return sb.toString().trim();
        String meio = digits.substring(2);
        if (meio.length() <= 8) {
            sb.append(meio, 0, Math.min(4, meio.length()));
            if (meio.length() > 4) sb.append('-').append(meio.substring(4));
        } else {
            sb.append(meio, 0, 5).append('-').append(meio.substring(5));
        }
        return sb.toString();
    }

    // ========= Máscara na exibição da tabela =========
    private void configurarCelulasTabela() {
        colCpf.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setText(null); return; }
                String digits = item.replaceAll("\\D", "");
                setText(digits.length() == 11
                        ? digits.substring(0,3)+"."+digits.substring(3,6)+"."+digits.substring(6,9)+"-"+digits.substring(9)
                        : item);
            }
        });

        colTel.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.isBlank()) { setText(null); return; }
                String d = item.replaceAll("\\D", "");
                if (d.length() < 10) { setText(item); return; }
                String fmt = (d.length() == 11)
                        ? String.format("(%s) %s-%s", d.substring(0,2), d.substring(2,7), d.substring(7))
                        : String.format("(%s) %s-%s", d.substring(0,2), d.substring(2,6), d.substring(6));
                setText(fmt);
            }
        });
    }
}
