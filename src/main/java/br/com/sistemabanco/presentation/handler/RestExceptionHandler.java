package br.com.sistemabanco.presentation.handler;

import br.com.sistemabanco.domain.exception.ConflitoConcorrenciaException;
import br.com.sistemabanco.domain.exception.ContaComHistoricoException;
import br.com.sistemabanco.domain.exception.ContaComSaldoException;
import br.com.sistemabanco.domain.exception.ContaNaoEncontradaException;
import br.com.sistemabanco.domain.exception.CpfInvalidoException;
import br.com.sistemabanco.domain.exception.CpfJaCadastradoException;
import br.com.sistemabanco.domain.exception.EmailInvalidoException;
import br.com.sistemabanco.domain.exception.SaldoInsuficienteException;
import br.com.sistemabanco.domain.exception.ServicoCepException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;
import java.util.Map;


@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);

    private static final HttpStatusCode UNPROCESSABLE_CONTENT = HttpStatusCode.valueOf(422);

    // ---- 400 ----

    @ExceptionHandler({CpfInvalidoException.class, EmailInvalidoException.class, IllegalArgumentException.class})
    public ResponseEntity<ProblemDetail> handleRequisicaoInvalida(RuntimeException ex) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição inválida", ex.getMessage());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(erro -> Map.of(
                        "campo", erro.getField(),
                        "mensagem", erro.getDefaultMessage() != null ? erro.getDefaultMessage() : "valor inválido"))
                .toList();

        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Um ou mais campos são inválidos.");
        problema.setTitle("Requisição inválida");
        problema.setProperty("erros", erros);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problema);
    }

    // ---- 404 ----

    @ExceptionHandler(ContaNaoEncontradaException.class)
    public ResponseEntity<ProblemDetail> handleContaNaoEncontrada(ContaNaoEncontradaException ex) {
        return problema(HttpStatus.NOT_FOUND, "Conta não encontrada", ex.getMessage());
    }

    // ---- 409 ----

    @ExceptionHandler({
            CpfJaCadastradoException.class,
            ContaComSaldoException.class,
            ContaComHistoricoException.class,
            ConflitoConcorrenciaException.class
    })
    public ResponseEntity<ProblemDetail> handleConflito(RuntimeException ex) {
        return problema(HttpStatus.CONFLICT, "Conflito", ex.getMessage());
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ProblemDetail> handleFalhaDeLock(RuntimeException ex) {
        log.warn("Falha de lock no banco: {}", ex.getMessage());
        return problema(HttpStatus.CONFLICT, "Conflito",
                "A conta está sendo usada por outra operação. Tente novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ProblemDetail> handleIntegridade(DataIntegrityViolationException ex) {
        log.warn("Violação de integridade no banco", ex);
        return problema(HttpStatus.CONFLICT, "Conflito",
                "A operação viola uma regra de integridade dos dados.");
    }

    // ---- 422 ----

    @ExceptionHandler(SaldoInsuficienteException.class)
    public ResponseEntity<ProblemDetail> handleSaldoInsuficiente(SaldoInsuficienteException ex) {
        return problema(UNPROCESSABLE_CONTENT, "Operação não permitida", ex.getMessage());
    }

    // ---- 502 / 503 ----

    @ExceptionHandler(ServicoCepException.class)
    public ResponseEntity<ProblemDetail> handleServicoCep(ServicoCepException ex) {
        // A mensagem original pode conter detalhes internos: vai só para o log.
        log.warn("Falha ao consultar o serviço de CEP", ex);
        return problema(HttpStatus.BAD_GATEWAY, "Serviço de CEP indisponível",
                "Não foi possível consultar o endereço do CEP agora. Tente novamente em instantes.");
    }

    @ExceptionHandler({DataAccessResourceFailureException.class, CannotCreateTransactionException.class})
    public ResponseEntity<ProblemDetail> handleBancoIndisponivel(RuntimeException ex) {
        log.error("Banco de dados indisponível", ex);
        return problema(HttpStatus.SERVICE_UNAVAILABLE, "Serviço indisponível",
                "O serviço está temporariamente indisponível. Tente novamente em instantes.");
    }

    // ---- 500 ----

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleInesperada(Exception ex) {
        log.error("Erro inesperado", ex);
        return problema(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.");
    }

    private ResponseEntity<ProblemDetail> problema(HttpStatusCode status, String titulo, String detalhe) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, detalhe);
        problema.setTitle(titulo);
        return ResponseEntity.status(status).body(problema);
    }
}