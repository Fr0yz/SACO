package controller;

import dao.CadastroPessoaDao;
import dao.TratamentoDao;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;
import model.Agendamento;
import model.Dentista;
import model.Pessoa;       // paciente
import model.Tratamento;
import model.StatusAgendamento;
import service.AgendamentoService;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.TimeZone;

public class AgendamentoController {

    // ====== Tabela ======
    @FXML private TableView<Agendamento> tabela;
    @FXML private TableColumn<Agendamento, Number> colId;
    @FXML private TableColumn<Agendamento, String> colPaciente;
    @FXML private TableColumn<Agendamento, String> colDentista;
    @FXML private TableColumn<Agendamento, String> colTratamento;
    @FXML private TableColumn<Agendamento, String> colDataHora;
    @FXML private TableColumn<Agendamento, String> colStatus;
    @FXML private TableColumn<Agendamento, String> colObs;

    // ====== Formulário ======
    @FXML private TextField txtId;
    @FXML private ComboBox<Pessoa> cbPaciente;
    @FXML private ComboBox<Dentista> cbDentista;
    @FXML private ComboBox<Tratamento> cbTratamento;
    @FXML private ComboBox<StatusAgendamento> cbStatus;
    @FXML private DatePicker dpData;
    @FXML private TextField txtHora; // HH:mm
    @FXML private TextArea txtObs;

    // ====== Botões (ids opcionais, mas úteis para testes) ======
    @FXML private Button btnNovo, btnSalvar, btnAtualizar, btnExcluir, btnRecarregar, btnLimpar;

    // ====== Infra ======
    private final ObservableList<Agendamento> dados = FXCollections.observableArrayList();
    private final AgendamentoService service = new AgendamentoService(); // usa DurationProvider default (60min)
    private final CadastroPessoaDao cadastroDao = new CadastroPessoaDao();
    private final TratamentoDao tratamentoDao = new TratamentoDao();
    private final SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    private final DateTimeFormatter dpFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML
    public void initialize() {
        // Config tabela
        colId.setCellValueFactory(new PropertyValueFactory<>("id_agendamento"));
        colPaciente.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNomePaciente())
        );
        colDentista.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNomeDentista())
        );
        colTratamento.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().getNomeTratamento())
        );
        colDataHora.setCellValueFactory(new PropertyValueFactory<>("data_hora"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colObs.setCellValueFactory(new PropertyValueFactory<>("observacoes"));



        tabela.setItems(dados);
        tabela.getSelectionModel().selectedItemProperty().addListener((obs, old, sel) -> {
            if (sel != null) preencherFormulario(sel);
        });

        // DatePicker no formato dd/MM/yyyy
        dpData.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date == null ? "" : dpFmt.format(date); }
            @Override public LocalDate fromString(String s) { return (s == null || s.isBlank()) ? null : LocalDate.parse(s, dpFmt); }
        });

        // Carregar combos
        carregarCombos();

        // Carregar tabela
        carregarDados();
    }

    private void carregarCombos() {
        try {
            cbPaciente.setItems(FXCollections.observableArrayList(cadastroDao.listarPacientes()));
            cbDentista.setItems(FXCollections.observableArrayList(cadastroDao.listarDentista()));
            cbTratamento.setItems(FXCollections.observableArrayList(tratamentoDao.listar()));
        } catch (SQLException e) {
            mostrarErro("Erro ao carregar listas", e.getMessage());
        }

        cbStatus.setItems(FXCollections.observableArrayList(StatusAgendamento.values()));

        // Renderização dos itens
        setupCombo(cbPaciente, Pessoa::getNome);
        setupCombo(cbDentista, Dentista::getNome);
        setupCombo(cbTratamento, Tratamento::getDescricao);

        // Status mostra descrição
        cbStatus.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(StatusAgendamento it, boolean empty) {
                super.updateItem(it, empty); setText(empty || it == null ? "" : it.getDescricao());
            }
        });
        cbStatus.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(StatusAgendamento it, boolean empty) {
                super.updateItem(it, empty); setText(empty || it == null ? "" : it.getDescricao());
            }
        });
    }

    private <T> void setupCombo(ComboBox<T> cb, java.util.function.Function<T, String> toStringFn) {
        cb.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(T it, boolean empty) {
                super.updateItem(it, empty); setText(empty || it == null ? "" : toStringFn.apply(it));
            }
        });
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(T it, boolean empty) {
                super.updateItem(it, empty); setText(empty || it == null ? "" : toStringFn.apply(it));
            }
        });
    }

    private void carregarDados() {
        try {
            List<Agendamento> lista = service.listar();
            dados.setAll(lista);
        } catch (AgendamentoService.ServiceException e) {
            mostrarErro("Erro ao listar", e.getMessage());
        }
    }

    private void preencherFormulario(Agendamento a) {
        txtId.setText(a.getId_agendamento() == null ? "" : String.valueOf(a.getId_agendamento()));
        selecionarComboPorId(cbPaciente, a.getId_paciente());
        selecionarComboPorId(cbDentista, a.getId_dentista());
        selecionarComboPorId(cbTratamento, a.getId_tratamento());
        cbStatus.setValue(a.getStatus());
        txtObs.setText(nvl(a.getObservacoes()));

        if (a.getData_hora() != null) {
            // funciona para java.util.Date, java.sql.Date e java.sql.Timestamp
            long epochMillis = a.getData_hora().getTime();
            LocalDateTime ldt = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMillis),
                    java.time.ZoneId.systemDefault());
            dpData.setValue(ldt.toLocalDate());
            txtHora.setText(String.format("%02d:%02d", ldt.getHour(), ldt.getMinute()));
        } else {
            dpData.setValue(null);
            txtHora.clear();
        }
    }


    private <T> void selecionarComboPorId(ComboBox<T> cb, Integer id) {
        if (id == null) { cb.getSelectionModel().clearSelection(); return; }
        for (T item : cb.getItems()) {
            Integer itemId = null;
            if (item instanceof Pessoa p) itemId = p.getId_pessoa();
            else if (item instanceof Dentista d) itemId = d.getId_dentista();
            else if (item instanceof Tratamento t) itemId = t.getId_tratamento();
            if (Objects.equals(itemId, id)) { cb.getSelectionModel().select(item); return; }
        }
        cb.getSelectionModel().clearSelection();
    }

    // ================== Ações ==================
    @FXML
    private void onNovo() {
        tabela.getSelectionModel().clearSelection();
        limparFormulario();
        cbPaciente.requestFocus();
    }

    @FXML
    private void onSalvar() {
        try {
            Agendamento a = lerFormulario(false);
            int id = service.criar(a);
            mostrarInfo("Sucesso", "Agendamento criado. ID=" + id);
            carregarDados();
            selecionarNaTabela(id);
        } catch (AgendamentoService.ServiceException | IllegalArgumentException e) {
            mostrarErro("Erro ao salvar", e.getMessage());
        }
    }

    @FXML
    private void onAtualizar() {
        try {
            Agendamento a = lerFormulario(true);
            service.atualizar(a);
            mostrarInfo("Sucesso", "Agendamento atualizado.");
            carregarDados();
            selecionarNaTabela(a.getId_agendamento());
        } catch (AgendamentoService.ServiceException | IllegalArgumentException e) {
            mostrarErro("Erro ao atualizar", e.getMessage());
        }
    }

    @FXML
    private void onExcluir() {
        Agendamento sel = tabela.getSelectionModel().getSelectedItem();
        if (sel == null) { mostrarAviso("Atenção", "Selecione um agendamento."); return; }

        Alert conf = new Alert(Alert.AlertType.CONFIRMATION,
                "Excluir o agendamento ID=" + sel.getId_agendamento() + "?",
                ButtonType.YES, ButtonType.NO);
        conf.setHeaderText("Confirmação");
        conf.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    service.excluir(sel.getId_agendamento());
                    mostrarInfo("Sucesso", "Agendamento excluído.");
                    carregarDados();
                    limparFormulario();
                } catch (AgendamentoService.ServiceException e) {
                    mostrarErro("Erro ao excluir", e.getMessage());
                }
            }
        });
    }

    @FXML private void onRecarregar() { carregarDados(); }
    @FXML private void onLimpar() { limparFormulario(); }

    // ================== Helpers ==================
    private Agendamento lerFormulario(boolean requerId) {
        Agendamento a = new Agendamento();

        // ID
        String idStr = txtId.getText();
        if (requerId) {
            if (idStr == null || idStr.isBlank())
                throw new IllegalArgumentException("ID é obrigatório para atualizar.");
            a.setId_agendamento(Integer.parseInt(idStr));
        }

        // Combos obrigatórios
        Pessoa pac = cbPaciente.getValue();
        Dentista den = cbDentista.getValue();
        Tratamento trt = cbTratamento.getValue();
        StatusAgendamento st = cbStatus.getValue();

        if (pac == null) throw new IllegalArgumentException("Paciente é obrigatório.");
        if (den == null) throw new IllegalArgumentException("Dentista é obrigatório.");
        if (trt == null) throw new IllegalArgumentException("Tratamento é obrigatório.");
        if (st == null)  throw new IllegalArgumentException("Status é obrigatório.");

        a.setId_paciente(pac.getId_pessoa());
        a.setId_dentista(den.getId_dentista());
        a.setId_tratamento(trt.getId_tratamento());
        a.setStatus(st);
        a.setObservacoes(txtObs.getText());

        // Data/Hora
        LocalDate data = dpData.getValue();
        String hhmm = txtHora.getText();
        a.setData_hora(montarDate(data, hhmm));

        return a;
    }

    private Date montarDate(LocalDate data, String hhmm) {
        if (data == null) throw new IllegalArgumentException("Data é obrigatória.");
        if (hhmm == null || !hhmm.matches("^\\d{2}:\\d{2}$"))
            throw new IllegalArgumentException("Hora inválida. Use HH:mm.");

        LocalTime lt = LocalTime.parse(hhmm);
        LocalDateTime ldt = LocalDateTime.of(data, lt);
        ZonedDateTime zdt = ldt.atZone(ZoneId.systemDefault());
        return Date.from(zdt.toInstant());
    }

    private void selecionarNaTabela(int id) {
        for (Agendamento a : dados) {
            if (a.getId_agendamento() != null && a.getId_agendamento() == id) {
                tabela.getSelectionModel().select(a);
                tabela.scrollTo(a);
                break;
            }
        }
    }

    private void limparFormulario() {
        txtId.clear();
        cbPaciente.getSelectionModel().clearSelection();
        cbDentista.getSelectionModel().clearSelection();
        cbTratamento.getSelectionModel().clearSelection();
        cbStatus.getSelectionModel().clearSelection();
        dpData.setValue(null);
        txtHora.clear();
        txtObs.clear();
    }

    private void mostrarErro(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(titulo); a.showAndWait();
    }
    private void mostrarInfo(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(titulo); a.showAndWait();
    }
    private void mostrarAviso(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(titulo); a.showAndWait();
    }
    private String nvl(String s) { return s == null ? "" : s; }
}
