package br.com.sistemabanco.infrastructure.persistence;

import br.com.sistemabanco.infrastructure.config.ConexaoBanco;
import br.com.sistemabanco.domain.exception.CpfJaCadastradoException;
import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;
import br.com.sistemabanco.domain.factory.ContaFactory;
import br.com.sistemabanco.domain.model.*;
import br.com.sistemabanco.domain.repository.ContaDAO;
import br.com.sistemabanco.util.Validador;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ContaDAOImpl implements ContaDAO {

    @Override
    public boolean suportaTransacoesAtomicas() {
        return true;
    }

    @Override
    public Optional<ContaBancaria> buscarPorCpf(String cpf) {
        String sql = "SELECT c.titular, c.saldo, c.cpf, c.email, c.tipo, c.data_abertura,"
                + " c.saques_realizados, c.data_ultimo_saque,"
                + " e.cep, e.logradouro, e.bairro, e.cidade, e.uf"
                + " FROM contas c LEFT JOIN enderecos e ON c.cpf = e.cpf_titular WHERE c.cpf = ?";

        try (Connection conn = ConexaoBanco.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Validador.normalizarCpf(cpf));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    ContaBancaria conta = mapearConta(rs);
                    conta.limparHistorico();
                    carregarTransacoes(conn, conta);
                    return Optional.of(conta);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar conta por CPF no banco de dados", e);
        }

        return Optional.empty();
    }

    @Override
    public List<ContaBancaria> buscarTodas() {
        List<ContaBancaria> contas = new ArrayList<>();
        String sql = "SELECT c.titular, c.saldo, c.cpf, c.email, c.tipo, c.data_abertura,"
                + " c.saques_realizados, c.data_ultimo_saque,"
                + " e.cep, e.logradouro, e.bairro, e.cidade, e.uf"
                + " FROM contas c LEFT JOIN enderecos e ON c.cpf = e.cpf_titular";

        try (Connection conn = ConexaoBanco.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                ContaBancaria conta = mapearConta(rs);
                conta.limparHistorico();
                carregarTransacoes(conn, conta);
                contas.add(conta);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar todas as contas no banco de dados", e);
        }

        return contas;
    }

    @Override
    public void salvar(ContaBancaria conta) {
        String sqlConta = "INSERT INTO contas (titular, saldo, cpf, email, tipo, data_abertura, saques_realizados, data_ultimo_saque)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlEndereco = "INSERT INTO enderecos (cpf_titular, cep, logradouro, bairro, cidade, uf) VALUES (?, ?, ?, ?, ?, ?)";
        String sqlTransacao = "INSERT INTO transacoes (cpf_titular, tipo, valor, data_hora) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexaoBanco.getConnection()) {
            conn.setAutoCommit(false);

            try {

                try (PreparedStatement stmtConta = conn.prepareStatement(sqlConta)) {
                    stmtConta.setString(1, conta.getTitular());
                    stmtConta.setBigDecimal(2, conta.getSaldo());
                    stmtConta.setString(3, conta.getCpf());
                    stmtConta.setString(4, conta.getEmail());
                    stmtConta.setString(5, conta.getTipoConta().name());
                    stmtConta.setDate(6, Date.valueOf(conta.getDataAbertura()));
                    stmtConta.setInt(7, conta.getQuantidadeSaques());
                    if (conta instanceof ContaCorrente corrente && corrente.getDataUltimoSaque() != null) {
                        stmtConta.setDate(8, Date.valueOf(corrente.getDataUltimoSaque()));
                    } else {
                        stmtConta.setNull(8, Types.DATE);
                    }
                    stmtConta.executeUpdate();
                }

                if (conta.getEndereco() != null) {
                    try (PreparedStatement stmtEndereco = conn.prepareStatement(sqlEndereco)) {
                        Endereco end = conta.getEndereco();
                        stmtEndereco.setString(1, conta.getCpf());
                        stmtEndereco.setString(2, end.getCep());
                        stmtEndereco.setString(3, end.getLogradouro());
                        stmtEndereco.setString(4, end.getBairro());
                        stmtEndereco.setString(5, end.getCidade());
                        stmtEndereco.setString(6, end.getEstado());
                        stmtEndereco.executeUpdate();
                    }
                }

                try (PreparedStatement stmtTransacao = conn.prepareStatement(sqlTransacao)) {
                    for (Transacao t : conta.getHistorico()) {
                        stmtTransacao.setString(1, conta.getCpf());
                        stmtTransacao.setString(2, t.getTipo().name());
                        stmtTransacao.setBigDecimal(3, t.getValor());
                        stmtTransacao.setTimestamp(4, Timestamp.valueOf(t.getDataHora()));
                        stmtTransacao.addBatch();
                    }
                    stmtTransacao.executeBatch();
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            if ("23000".equals(e.getSQLState())) {
                throw new CpfJaCadastradoException("Já existe uma conta cadastrada para o CPF: " + conta.getCpf());
            }
            throw new RuntimeException("Erro ao salvar conta no banco de dados", e);
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar conta no banco de dados", e);
        }
    }

    @Override
    public void atualizarSaldoESaques(ContaBancaria conta) {
        String sql = "UPDATE contas SET saldo = ?, saques_realizados = ?, data_ultimo_saque = ? WHERE cpf = ?";

        try (Connection conn = ConexaoBanco.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setBigDecimal(1, conta.getSaldo());
            stmt.setInt(2, conta.getQuantidadeSaques());
            if (conta instanceof ContaCorrente corrente && corrente.getDataUltimoSaque() != null) {
                stmt.setDate(3, Date.valueOf(corrente.getDataUltimoSaque()));
            } else {
                stmt.setNull(3, Types.DATE);
            }
            stmt.setString(4, conta.getCpf());
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao atualizar saldo no banco de dados", e);
        }
    }

    @Override
    public void movimentarComTransacao(ContaBancaria conta, Transacao transacao) {
        movimentarComTransacao(conta, transacao, null);
    }

    @Override
    public void movimentarComTransacao(ContaBancaria conta, Transacao transacao, BigDecimal saldoAnterior) {
            String sqlSaldo = "UPDATE contas SET saldo = ?, saques_realizados = ?, data_ultimo_saque = ? WHERE cpf = ?";
            if (saldoAnterior != null) {
                sqlSaldo = "UPDATE contas SET saldo = ?, saques_realizados = ?, data_ultimo_saque = ?"
                        + " WHERE cpf = ? AND saldo = ?";
            }
            String sqlTransacao = "INSERT INTO transacoes (cpf_titular, tipo, valor, data_hora) VALUES (?, ?, ?, ?)";

            try (Connection conn = ConexaoBanco.getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement saldo = conn.prepareStatement(sqlSaldo);
                     PreparedStatement historico = conn.prepareStatement(sqlTransacao)) {
                    saldo.setBigDecimal(1, conta.getSaldo());
                    saldo.setInt(2, conta.getQuantidadeSaques());
                    if (conta instanceof ContaCorrente corrente && corrente.getDataUltimoSaque() != null) {
                        saldo.setDate(3, Date.valueOf(corrente.getDataUltimoSaque()));
                    } else {
                        saldo.setNull(3, Types.DATE);
                    }
                    saldo.setString(4, conta.getCpf());
                    if (saldoAnterior != null) {
                        saldo.setBigDecimal(5, saldoAnterior);
                    }
                    if (saldo.executeUpdate() != 1) {
                        throw new SQLException("Conta não encontrada ao atualizar o saldo.");
                    }

                    historico.setString(1, conta.getCpf());
                    historico.setString(2, transacao.getTipo().name());
                    historico.setBigDecimal(3, transacao.getValor());
                    historico.setTimestamp(4, Timestamp.valueOf(transacao.getDataHora()));
                    historico.executeUpdate();
                    conn.commit();
                } catch (SQLException e) {
                    conn.rollback();
                    throw new RuntimeException("Erro na transação. Rollback executado.", e);
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException e) {
                throw new RuntimeException("Erro de conexão durante a movimentação", e);
        }
    }

    @Override
    public void registrarTransacao(String cpf, Transacao transacao) {
        String sql = "INSERT INTO transacoes (cpf_titular, tipo, valor, data_hora) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexaoBanco.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, cpf);
            stmt.setString(2, transacao.getTipo().name());
            stmt.setBigDecimal(3, transacao.getValor());
            stmt.setTimestamp(4, Timestamp.valueOf(transacao.getDataHora()));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao registrar transação", e);
        }
    }

    @Override
    public void transferirComTransacao(ContaBancaria origem, ContaBancaria destino, BigDecimal valor) {
        transferirComTransacao(origem, destino, valor, null, null);
    }

    @Override
    public void transferirComTransacao(ContaBancaria origem, ContaBancaria destino, BigDecimal valor,
                                       BigDecimal saldoOrigemAnterior, BigDecimal saldoDestinoAnterior) {
        String sqlUpdate = "UPDATE contas SET saldo = ?, saques_realizados = ?, data_ultimo_saque = ? WHERE cpf = ?";
        String sqlUpdateComVerificacao = "UPDATE contas SET saldo = ?, saques_realizados = ?, data_ultimo_saque = ?"
                + " WHERE cpf = ? AND saldo = ?";
        String sqlTransacao = "INSERT INTO transacoes (cpf_titular, tipo, valor, data_hora) VALUES (?, ?, ?, ?)";

        try (Connection conn = ConexaoBanco.getConnection()) {
            conn.setAutoCommit(false);

            String sqlOrigem = saldoOrigemAnterior == null ? sqlUpdate : sqlUpdateComVerificacao;
            String sqlDestino = saldoDestinoAnterior == null ? sqlUpdate : sqlUpdateComVerificacao;
            try (PreparedStatement stmtOrigem = conn.prepareStatement(sqlOrigem);
                 PreparedStatement stmtDestino = conn.prepareStatement(sqlDestino);
                 PreparedStatement stmtTransacao = conn.prepareStatement(sqlTransacao)) {

                stmtOrigem.setBigDecimal(1, origem.getSaldo());
                preencherEstadoSaque(stmtOrigem, 2, origem);
                stmtOrigem.setString(4, origem.getCpf());
                if (saldoOrigemAnterior != null) {
                    stmtOrigem.setBigDecimal(5, saldoOrigemAnterior);
                }
                if (stmtOrigem.executeUpdate() != 1) {
                    throw new SQLException("Saldo da conta de origem foi alterado por outra operação.");
                }

                stmtDestino.setBigDecimal(1, destino.getSaldo());
                preencherEstadoSaque(stmtDestino, 2, destino);
                stmtDestino.setString(4, destino.getCpf());
                if (saldoDestinoAnterior != null) {
                    stmtDestino.setBigDecimal(5, saldoDestinoAnterior);
                }
                if (stmtDestino.executeUpdate() != 1) {
                    throw new SQLException("Saldo da conta de destino foi alterado por outra operação.");
                }

                stmtTransacao.setString(1, origem.getCpf());
                stmtTransacao.setString(2, TipoTransacao.TRANSFERENCIA_ENVIADA.name());
                stmtTransacao.setBigDecimal(3, valor);
                Timestamp agora = Timestamp.valueOf(LocalDateTime.now());
                stmtTransacao.setTimestamp(4, agora);
                stmtTransacao.executeUpdate();

                stmtTransacao.setString(1, destino.getCpf());
                stmtTransacao.setString(2, TipoTransacao.TRANSFERENCIA_RECEBIDA.name());
                stmtTransacao.setBigDecimal(3, valor);
                stmtTransacao.setTimestamp(4, agora);
                stmtTransacao.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException("Erro na transação. Rollback executado.", e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro de conexão durante a transferência", e);
        }
    }

    private void preencherEstadoSaque(PreparedStatement stmt, int indice, ContaBancaria conta) throws SQLException {
        stmt.setInt(indice, conta.getQuantidadeSaques());
        if (conta instanceof ContaCorrente corrente && corrente.getDataUltimoSaque() != null) {
            stmt.setDate(indice + 1, Date.valueOf(corrente.getDataUltimoSaque()));
        } else {
            stmt.setNull(indice + 1, Types.DATE);
        }
    }

    @Override
    public void deletar(String cpf) {
        String sql = "DELETE FROM contas WHERE cpf = ?";

        try (Connection conn = ConexaoBanco.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, Validador.normalizarCpf(cpf));
            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar conta", e);
        }
    }

    private void carregarTransacoes(Connection conn, ContaBancaria conta) throws SQLException {
        String sql = "SELECT tipo, valor, data_hora FROM transacoes WHERE cpf_titular = ? ORDER BY data_hora ASC, id ASC";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, conta.getCpf());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    TipoTransacao tipo = TipoTransacao.valueOf(rs.getString("tipo"));
                    BigDecimal valor = rs.getBigDecimal("valor");
                    Timestamp dataHora = rs.getTimestamp("data_hora");

                    conta.adicionarTransacaoExistente(new Transacao(tipo, valor, dataHora.toLocalDateTime()));
                }
            }
        }
    }

    private ContaBancaria mapearConta(ResultSet rs) throws SQLException {
        try {
            String titular = rs.getString("titular");
            BigDecimal saldo = rs.getBigDecimal("saldo");
            String cpf = rs.getString("cpf");
            String email = rs.getString("email");
            Date dataAberturaSql = rs.getDate("data_abertura");
            java.time.LocalDate dataAbertura = dataAberturaSql != null
                    ? dataAberturaSql.toLocalDate()
                    : java.time.LocalDate.now();
            int saquesRealizados = rs.getInt("saques_realizados");
            Date dataUltimoSaqueSql = rs.getDate("data_ultimo_saque");
            java.time.LocalDate dataUltimoSaque = dataUltimoSaqueSql != null
                    ? dataUltimoSaqueSql.toLocalDate()
                    : null;

            String tipoStr = rs.getString("tipo");
            TipoConta tipo = TipoConta.valueOf(tipoStr);

            Endereco endereco = null;
            String cep = rs.getString("cep");
            if (cep != null) {
                endereco = new Endereco(
                        cep,
                        rs.getString("logradouro"),
                        rs.getString("bairro"),
                        rs.getString("cidade"),
                        rs.getString("uf")
                );
            }

            return ContaFactory.criarContaComEstado(tipo, titular, saldo, cpf, email, dataAbertura, endereco,
                    saquesRealizados, dataUltimoSaque);
        } catch (CpfInvalidoException | EmailInvalidoException e) {
            throw new SQLException("Erro ao mapear conta: dados no banco estão num formato inválido", e);
        }
    }
}