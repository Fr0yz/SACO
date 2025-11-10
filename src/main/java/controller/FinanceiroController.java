package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;
import model.*;
import service.FinanceiroService;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class FinanceiroController {

    // ====== Tabela Financeiro ======
    @FXML private TableView<Financeiro> tblFinanceiro;
    @FXML private TableColumn<Financeiro, Number> colFinId;
    @FXML private TableColumn<Financeiro, Number> colFinAgendamento;
    @FXML private TableColumn<Financeiro, String> colFinValor;
    @FXML private TableColumn<Financeiro, String> colFinEmissao;
    @FXML private TableColumn<Financeiro, String> colFinStatus;
    @FXML private TableColumn<Financeiro, String> colFinMetodo;

    // ====== Form: emitir título ======
    @FXML private TextField txtFinId;
    @FXML private TextField txtFinAgendamento;
    @FXML private TextField txtFinValor;
    @FXML private DatePicker dpFinEmissao;
    @FXML private TextField txtFinHora; // HH:mm
    @FXML private ComboBox<MetodoPagamento> cbFinMetodo;
    @FXML private ComboBox<StatusFinanceiro> cbFinStatus;
    @FXML private TextArea txtFinObsFake; // se quiser Observações futuramente (placeholder)

    // ====== Ações Financeiro ======
    @FXML private Button btnFinNovo, btnFinEmitir, btnFinAtualizar, btnFinCancelar, btnFinRecarregar, btnFinLimpar;

    // ====== Subtabela Pagamentos ======
    @FXML private TableView<Pagamento> tblPagamentos;
    @FXML private TableColumn<Pagamento, Number> colPagId;
    @FXML private TableColumn<Pagamento, String> colPagValor;
    @FXML private TableColumn<Pagamento, String> colPagData;
    @FXML private TableColumn<Pagamento, String> colPagFatura;
    @FXML private TableColumn<Pagamento, String> colPagBoleto;
    @FXML private TableColumn<Pagamento, String> colPagStatus;

    // ====== Form: registrar pagamento ======
    @FXML private TextField txtPagValor;
    @FXML private DatePicker dpPagData;
    @FXML private TextField txtPagHora; // HH:mm
    @FXML private TextField txtPagFatura;
    @FXML private TextField txtPagBoleto;
    @FXML private ComboBox<StatusPagamento> cbPagStatus;
    @FXML private Button btnPagRegistrar, btnPagEstornar;

    // ====== Infra ======
    private final FinanceiroService service = new FinanceiroService();
    private final ObservableList<Financeiro> dadosFin = FXCollections.observableArrayList();
    private final ObservableList<Pagamento> dadosPag = FXCollections.observableArrayList();

    private final DateTimeFormatter fmtData = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @FXML
    public void initialize() {
        // Colunas Financeiro
        colFinId.setCellValueFactory(c -> new javafx.beans.property.SimpleLongProperty(
                c.getValue().getId_financeiro() == null ? 0L : c.getValue().getId_financeiro()));
        colFinAgendamento.setCellValueFactory(c -> new javafx.beans.property.SimpleLongProperty(
                c.getValue().getId_agendamento() == null ? 0L : c.getValue().getId_agendamento()));
        colFinValor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getValor_total() == null ? "" : nf.format(c.getValue().getValor_total())));
        colFinEmissao.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDt_emissao() == null ? "" : formatDateTime(c.getValue().getDt_emissao())));
        colFinStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getStatus() == null ? "" : c.getValue().getStatus().name()));
        colFinMetodo.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getMetodo_pagamento() == null ? "" : c.getValue().getMetodo_pagamento().name()));

        tblFinanceiro.setItems(dadosFin);
        tblFinanceiro.getSelectionModel().selectedItemProperty().addListener((obs, o, sel) -> {
            if (sel != null) preencherFormFinanceiro(sel);
            carregarPagamentosDoSelecionado();
        });

        // Colunas Pagamentos
        colPagId.setCellValueFactory(c -> new javafx.beans.property.SimpleLongProperty(
                c.getValue().getId_pagamento() == null ? 0L : c.getValue().getId_pagamento()));
        colPagValor.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getValor() == null ? "" : nf.format(c.getValue().getValor())));
        colPagData.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getDt_pagamento() == null ? "" : formatDateTime(c.getValue().getDt_pagamento())));
        colPagFatura.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nvl(c.getValue().getNum_fatura())));
        colPagBoleto.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(nvl(c.getValue().getNum_boleto())));
        colPagStatus.setCellValueFactory(c -> new javafx.beans.property.SimpleStringProperty(
                c.getValue().getStatus() == null ? "" : c.getValue().getStatus().name()));

        tblPagamentos.setItems(dadosPag);

        // Combos
        cbFinMetodo.setItems(FXCollections.observableArrayList(MetodoPagamento.values()));
        cbFinStatus.setItems(FXCollections.observableArrayList(StatusFinanceiro.values()));
        cbPagStatus.setItems(FXCollections.observableArrayList(StatusPagamento.values()));

        // DatePicker converter
        dpFinEmissao.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date == null ? "" : fmtData.format(date); }
            @Override public LocalDate fromString(String s) { return (s == null || s.isBlank()) ? null : LocalDate.parse(s, fmtData); }
        });
        dpPagData.setConverter(new StringConverter<>() {
            @Override public String toString(LocalDate date) { return date == null ? "" : fmtData.format(date); }
            @Override public LocalDate fromString(String s) { return (s == null || s.isBlank()) ? null : LocalDate.parse(s, fmtData); }
        });

        // defaults
        cbFinStatus.getSelectionModel().select(StatusFinanceiro.ABERTO);
        cbFinMetodo.getSelectionModel().select(MetodoPagamento.DINHEIRO);
        cbPagStatus.getSelectionModel().select(StatusPagamento.LIQUIDADO);

        carregarFinanceiros();
    }

    // ========= Financeiro =========
    private void carregarFinanceiros() {
        try {
            dadosFin.setAll(service.listar());
        } catch (FinanceiroService.ServiceException e) {
            erro("Erro ao listar financeiros", e.getMessage());
        }
    }

    private void preencherFormFinanceiro(Financeiro f) {
        txtFinId.setText(f.getId_financeiro() == null ? "" : String.valueOf(f.getId_financeiro()));
        txtFinAgendamento.setText(f.getId_agendamento() == null ? "" : String.valueOf(f.getId_agendamento()));
        txtFinValor.setText(f.getValor_total() == null ? "" : f.getValor_total().toPlainString());
        cbFinMetodo.setValue(f.getMetodo_pagamento());
        cbFinStatus.setValue(f.getStatus());

        if (f.getDt_emissao() != null) {
            LocalDateTime ldt = LocalDateTime.ofInstant(f.getDt_emissao().toInstant(), ZoneId.systemDefault());
            dpFinEmissao.setValue(ldt.toLocalDate());
            txtFinHora.setText(String.format("%02d:%02d", ldt.getHour(), ldt.getMinute()));
        } else {
            dpFinEmissao.setValue(null);
            txtFinHora.clear();
        }
    }

    private Financeiro lerFormFinanceiro(boolean requerId) {
        Financeiro f = new Financeiro();

        if (requerId) {
            if (txtFinId.getText() == null || txtFinId.getText().isBlank())
                throw new IllegalArgumentException("ID é obrigatório.");
            f.setId_financeiro(Long.parseLong(txtFinId.getText().trim()));
        }

        if (txtFinAgendamento.getText() == null || txtFinAgendamento.getText().isBlank())
            throw new IllegalArgumentException("ID do agendamento é obrigatório.");
        f.setId_agendamento(Long.parseLong(txtFinAgendamento.getText().trim()));

        if (txtFinValor.getText() == null || txtFinValor.getText().isBlank())
            throw new IllegalArgumentException("Valor total é obrigatório.");
        f.setValor_total(new BigDecimal(txtFinValor.getText().trim()));

        // Data/hora (opcionais na emissão; se vazio, usa agora)
        LocalDate data = dpFinEmissao.getValue();
        String hhmm = txtFinHora.getText();
        Date emissao = (data == null || hhmm == null || hhmm.isBlank())
                ? new Date()
                : toDate(data, hhmm);
        f.setDt_emissao(emissao);

        if (cbFinMetodo.getValue() == null) throw new IllegalArgumentException("Método de pagamento é obrigatório.");
        f.setMetodo_pagamento(cbFinMetodo.getValue());

        if (cbFinStatus.getValue() == null) throw new IllegalArgumentException("Status é obrigatório.");
        f.setStatus(cbFinStatus.getValue());

        return f;
    }

    @FXML private void onFinNovo() { limparFormFinanceiro(); }
    @FXML private void onFinRecarregar() { carregarFinanceiros(); }
    @FXML
    private void onFinLimpar() {
        // 1) limpa seleção e dados da tela
        tblFinanceiro.getSelectionModel().clearSelection();
        tblPagamentos.getItems().clear();

        // 2) limpa formulários
        limparFormFinanceiro();
        limparFormPagamento();

        // 3) limpa filtros/busca (se existirem no FXML)
        limparFiltrosFinanceiro();

        // 4) recarrega lista "full" sem filtro
        carregarFinanceiros();
    }

    @FXML
    private void onFinEmitir() {
        try {
            // para emitir, usamos apenas alguns campos: agendamento, valor, método
            Long idAg = Long.parseLong(txtFinAgendamento.getText().trim());
            BigDecimal valor = new BigDecimal(txtFinValor.getText().trim());
            MetodoPagamento metodo = cbFinMetodo.getValue();

            long idGerado = service.emitir(idAg, valor, metodo);
            info("Sucesso", "Financeiro emitido. ID=" + idGerado);
            carregarFinanceiros();
            selecionarFinanceiroNaTabela(idGerado);
        } catch (Exception e) {
            erro("Não foi possível emitir", e.getMessage());
        }
    }

    @FXML
    private void onFinAtualizar() {
        try {
            Financeiro f = lerFormFinanceiro(true);
            // Reaproveita a própria lógica do DAO via service: atualizar
            // (não criei método específico; reaproveitamos inserir/atualizar do DAO)
            if (!new dao.FinanceiroDAO().atualizar(f)) {
                throw new FinanceiroService.ServiceException("Atualização não efetuada.");
            }
            info("Sucesso", "Financeiro atualizado.");
            carregarFinanceiros();
            selecionarFinanceiroNaTabela(f.getId_financeiro());
        } catch (Exception e) {
            erro("Não foi possível atualizar", e.getMessage());
        }
    }

    @FXML
    private void onFinCancelar() {
        Financeiro sel = tblFinanceiro.getSelectionModel().getSelectedItem();
        if (sel == null) { aviso("Selecione um título financeiro."); return; }
        try {
            service.cancelar(sel.getId_financeiro());
            info("Sucesso", "Financeiro cancelado.");
            carregarFinanceiros();
        } catch (FinanceiroService.ServiceException e) {
            erro("Não foi possível cancelar", e.getMessage());
        }
    }

    private void limparFormFinanceiro() {
        txtFinId.clear();
        txtFinAgendamento.clear();
        txtFinValor.clear();
        dpFinEmissao.setValue(null);
        txtFinHora.clear();
        if (cbFinMetodo != null) cbFinMetodo.getSelectionModel().clearSelection();
        if (cbFinStatus != null) cbFinStatus.getSelectionModel().clearSelection();
        if (txtFinObsFake != null) txtFinObsFake.clear();
    }

    // NOVO: limpar form de pagamento
    private void limparFormPagamento() {
        if (txtPagValor != null) txtPagValor.clear();
        if (dpPagData != null) dpPagData.setValue(null);
        if (txtPagHora != null) txtPagHora.clear();
        if (txtPagFatura != null) txtPagFatura.clear();
        if (txtPagBoleto != null) txtPagBoleto.clear();
        if (cbPagStatus != null) cbPagStatus.getSelectionModel().clearSelection();
    }

    // NOVO: limpar campos de busca/filtro (se você os tiver no FXML)
// deixe as @FXML abaixo se tiver filtros; se não tiver, pode ignorar
    @FXML private TextField txtFiltroBusca;           // opcional
    @FXML private DatePicker dpFiltroIni, dpFiltroFim; // opcional
    @FXML private ComboBox<StatusFinanceiro> cbFiltroStatus; // opcional

    private void limparFiltrosFinanceiro() {
        if (txtFiltroBusca != null) txtFiltroBusca.clear();
        if (dpFiltroIni != null) dpFiltroIni.setValue(null);
        if (dpFiltroFim != null) dpFiltroFim.setValue(null);
        if (cbFiltroStatus != null) cbFiltroStatus.getSelectionModel().clearSelection();
    }

    private void selecionarFinanceiroNaTabela(long id) {
        for (Financeiro f : dadosFin) {
            if (f.getId_financeiro() != null && f.getId_financeiro() == id) {
                tblFinanceiro.getSelectionModel().select(f);
                tblFinanceiro.scrollTo(f);
                break;
            }
        }
    }

    private void carregarPagamentosDoSelecionado() {
        Financeiro sel = tblFinanceiro.getSelectionModel().getSelectedItem();
        dadosPag.clear();
        if (sel == null) return;
        try {
            dadosPag.setAll(service.listarPagamentos(sel.getId_financeiro()));
        } catch (FinanceiroService.ServiceException e) {
            erro("Erro ao listar pagamentos", e.getMessage());
        }
    }

    // ========= Pagamentos =========
    @FXML
    private void onPagRegistrar() {
        Financeiro sel = tblFinanceiro.getSelectionModel().getSelectedItem();
        if (sel == null) { aviso("Selecione um título financeiro para registrar pagamento."); return; }

        try {
            BigDecimal valor = new BigDecimal(txtPagValor.getText().trim());
            LocalDate data = dpPagData.getValue();
            String hhmm = txtPagHora.getText();
            Date dt = (data == null || hhmm == null || hhmm.isBlank()) ? new Date() : toDate(data, hhmm);
            String nf = txtPagFatura.getText();
            String nb = txtPagBoleto.getText();
            StatusPagamento st = cbPagStatus.getValue();

            service.registrarPagamento(sel.getId_financeiro(), valor, dt, nf, nb, st);
            info("Sucesso", "Pagamento registrado.");
            carregarPagamentosDoSelecionado();
            carregarFinanceiros(); // atualiza status (pode virar QUITADO)
        } catch (Exception e) {
            erro("Não foi possível registrar pagamento", e.getMessage());
        }
    }

    @FXML
    private void onPagEstornar() {
        Financeiro selFin = tblFinanceiro.getSelectionModel().getSelectedItem();
        Pagamento selPag = tblPagamentos.getSelectionModel().getSelectedItem();
        if (selFin == null || selPag == null) { aviso("Selecione um pagamento para estornar."); return; }

        try {
            service.estornarPagamento(selPag.getId_pagamento(), selFin.getId_financeiro());
            info("Sucesso", "Pagamento estornado.");
            carregarPagamentosDoSelecionado();
            carregarFinanceiros();
        } catch (FinanceiroService.ServiceException e) {
            erro("Não foi possível estornar", e.getMessage());
        }
    }

    // ========= Utils =========
    private String formatDateTime(Date d) {
        LocalDateTime ldt = LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
        return ldt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
    }

    private Date toDate(LocalDate data, String hhmm) {
        if (data == null) throw new IllegalArgumentException("Data é obrigatória.");
        if (hhmm == null || !hhmm.matches("^\\d{2}:\\d{2}$"))
            throw new IllegalArgumentException("Hora inválida. Use HH:mm.");
        LocalTime lt = LocalTime.parse(hhmm);
        LocalDateTime ldt = LocalDateTime.of(data, lt);
        return Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
    }

    private void erro(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.setHeaderText(titulo); a.showAndWait();
    }
    private void info(String titulo, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setHeaderText(titulo); a.showAndWait();
    }
    private void aviso(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText("Atenção"); a.showAndWait();
    }
    private String nvl(String s) { return s == null ? "" : s; }
}
