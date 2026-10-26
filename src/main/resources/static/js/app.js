import {
    definirCredenciais,
    limparCredenciais,
    estaAutenticado,
    apiFetch
} from "./api.js";

const $ = (id) => document.getElementById(id);

const telasDoApp = [
    "tela-conta",
    "tela-extrato",
    "tela-transferencia",
    "tela-admin"
];

function mostrarTela(id) {
    document.querySelectorAll(".tela, .pagina").forEach((tela) => {
        tela.hidden = tela.id !== id;
    });

    const estaNoApp = telasDoApp.includes(id);

    $("app").hidden = !estaNoApp;
    $("sidebar").hidden = !estaNoApp;

    document.querySelectorAll(".nav-item").forEach((botao) => {
        botao.classList.toggle(
            "ativo",
            `tela-${botao.dataset.page}` === id
        );
    });
}

function mostrarMensagem(id, texto, erro = false) {
    const elemento = $(id);
    if (!elemento) return;

    elemento.textContent = texto;
    elemento.classList.toggle("erro", erro && texto !== "");
    elemento.classList.toggle("sucesso", !erro && texto !== "");
}

function formatarMoeda(valor) {
    return Number(valor ?? 0).toLocaleString("pt-BR", {
        style: "currency",
        currency: "BRL"
    });
}

async function carregarConta() {
    const conta = await apiFetch("/contas/me");

    $("saldo").textContent = formatarMoeda(conta.saldo);
    $("titular").textContent = conta.titular ?? "—";
    $("cpf").textContent = conta.cpf ?? "—";
    $("tipo").textContent = conta.tipo ?? "—";

    mostrarTela("tela-conta");
}

async function carregarExtrato() {
    const extrato = await apiFetch("/contas/me/extrato");
    const lista = $("lista-extrato");

    lista.innerHTML = "";

    const transacoes = extrato.transacoes ?? [];

    if (transacoes.length === 0) {
        const item = document.createElement("li");
        item.textContent = "Nenhuma movimentação encontrada.";
        lista.appendChild(item);
        return;
    }

    transacoes.forEach((transacao) => {
        const item = document.createElement("li");

        const descricao = document.createElement("span");
        descricao.textContent = `${transacao.tipo} • ${new Date(transacao.dataHora).toLocaleString("pt-BR")}`;

        const valor = document.createElement("strong");
        valor.textContent = formatarMoeda(transacao.valor);

        item.append(descricao, valor);
        lista.appendChild(item);
    });
}

function tratarErro(idMensagem, erro) {
    console.error(erro);
    mostrarMensagem(
        idMensagem,
        erro.message || "Ocorreu um erro. Tente novamente.",
        true
    );
}

async function verificarSeEhAdmin() {
    const menuAdmin = $("menu-admin");

    try {
        await apiFetch("/contas?pagina=0&tamanho=1");
        menuAdmin.hidden = false;
        return true;
    } catch {
        menuAdmin.hidden = true;
        return false;
    }
}

async function carregarInterfaceAposLogin() {
    try {
        await carregarContasAdmin();
        $("menu-admin").hidden = false;
        mostrarTela("tela-admin");
        return;
    } catch (erro) {
        if (erro.status !== 403) {
            throw erro;
        }
    }

    await carregarConta();
    await verificarSeEhAdmin();
}

/* ---------- LOGIN ---------- */

$("form-login").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const usuario = $("login-cpf").value.trim();
    const senha = $("login-senha").value;

    mostrarMensagem("mensagem-login", "");

    try {
        definirCredenciais(usuario, senha);
        await carregarInterfaceAposLogin();
    } catch (erro) {
        limparCredenciais();
        tratarErro("mensagem-login", erro);
    }
});

/* ---------- CADASTRO ---------- */

$("botao-abrir-cadastro").addEventListener("click", () => {
    mostrarTela("tela-cadastro");
});

$("botao-voltar-login").addEventListener("click", () => {
    mostrarTela("tela-login");
});

$("form-cadastro").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const dados = {
        titular: $("cadastro-titular").value.trim(),
        cpf: $("cadastro-cpf").value.trim(),
        email: $("cadastro-email").value.trim(),
        cep: $("cadastro-cep").value.trim(),
        tipo: $("cadastro-tipo").value,
        saldoInicial: Number($("cadastro-saldo").value),
        senha: $("cadastro-senha").value
    };

    mostrarMensagem("mensagem-cadastro", "");

    try {
        await apiFetch("/contas", {
            method: "POST",
            body: JSON.stringify(dados)
        });

        $("login-cpf").value = dados.cpf;
        $("login-senha").value = "";
        $("form-cadastro").reset();

        mostrarTela("tela-login");
        mostrarMensagem(
            "mensagem-login",
            "Cadastro realizado! Entre com seu CPF e senha."
        );
    } catch (erro) {
        tratarErro("mensagem-cadastro", erro);
    }
});

/* ---------- DEPÓSITO ---------- */

$("form-deposito").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const valor = Number($("valor-deposito").value);

    if (!Number.isFinite(valor) || valor <= 0) {
        mostrarMensagem("mensagem-conta", "Informe um valor válido para o depósito.", true);
        return;
    }

    try {
        await apiFetch("/contas/me/depositos", {
            method: "POST",
            body: JSON.stringify({ valor })
        });

        $("form-deposito").reset();
        await carregarConta();

        mostrarMensagem("mensagem-conta", "Depósito realizado com sucesso!");
    } catch (erro) {
        tratarErro("mensagem-conta", erro);
    }
});

/* ---------- SAQUE ---------- */

$("form-saque").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const valor = Number($("valor-saque").value);

    if (!Number.isFinite(valor) || valor <= 0) {
        mostrarMensagem("mensagem-conta", "Informe um valor válido para o saque.", true);
        return;
    }

    try {
        await apiFetch("/contas/me/saques", {
            method: "POST",
            body: JSON.stringify({ valor })
        });

        $("form-saque").reset();
        await carregarConta();

        mostrarMensagem("mensagem-conta", "Saque realizado com sucesso!");
    } catch (erro) {
        tratarErro("mensagem-conta", erro);
    }
});

/* ---------- TRANSFERÊNCIA ---------- */

$("form-transferencia").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const cpfDestino = $("destino-transferencia").value.trim();
    const valor = Number($("valor-transferencia").value);

    if (!Number.isFinite(valor) || valor <= 0) {
        mostrarMensagem("mensagem-transferencia", "Informe um valor válido.", true);
        return;
    }

    try {
        await apiFetch("/transferencias", {
            method: "POST",
            body: JSON.stringify({
                cpfDestino,
                valor
            })
        });

        $("form-transferencia").reset();
        await carregarConta();

        mostrarMensagem("mensagem-transferencia", "Transferência realizada com sucesso!");
    } catch (erro) {
        tratarErro("mensagem-transferencia", erro);
    }
});

/* ---------- NAVEGAÇÃO ---------- */

document.querySelectorAll(".nav-item").forEach((botao) => {
    botao.addEventListener("click", async () => {
        const id = `tela-${botao.dataset.page}`;

        mostrarTela(id);

        if (id === "tela-extrato") {
            try {
                await carregarExtrato();
            } catch (erro) {
                tratarErro("mensagem-conta", erro);
            }
        }

        if (id === "tela-admin") {
            try {
                await carregarContasAdmin();
            } catch (erro) {
                tratarErro("mensagem-admin", erro);
            }
        }
    });
});

/* ---------- SAIR ---------- */

$("botao-sair").addEventListener("click", () => {
    limparCredenciais();

    $("form-login").reset();
    mostrarMensagem("mensagem-login", "");
    mostrarTela("tela-login");
});

/* ---------- ADMINISTRAÇÃO ---------- */

let paginaAdmin = 0;
const TAMANHO_PAGINA = 10;

async function carregarContasAdmin() {
    const resposta = await apiFetch(
        `/contas?pagina=${paginaAdmin}&tamanho=${TAMANHO_PAGINA}`
    );

    const corpoTabela = $("tabela-contas");
    corpoTabela.innerHTML = "";

    const contas = resposta.itens ?? [];

    if (contas.length === 0) {
        corpoTabela.innerHTML = `
            <tr>
                <td colspan="5">Nenhuma conta encontrada.</td>
            </tr>
        `;
    }

    contas.forEach((conta) => {
        const linha = document.createElement("tr");

        linha.innerHTML = `
            <td>${conta.titular ?? "—"}</td>
            <td>${conta.cpf ?? "—"}</td>
            <td>${conta.tipo ?? "—"}</td>
            <td>${formatarMoeda(conta.saldo)}</td>
        `;

        const acoes = document.createElement("td");
        const container = document.createElement("div");
        container.className = "acoes-conta";

        const botaoExtrato = document.createElement("button");
        botaoExtrato.className = "botao-acao";
        botaoExtrato.textContent = "Extrato";
        botaoExtrato.addEventListener("click", () =>
            verExtratoAdmin(conta.cpf)
        );

        const botaoTaxa = document.createElement("button");
        botaoTaxa.className = "botao-acao";
        botaoTaxa.textContent = "Cobrar taxa";
        botaoTaxa.addEventListener("click", () =>
            cobrarTaxa(conta.cpf)
        );

        const botaoRemover = document.createElement("button");
        botaoRemover.className = "botao-acao botao-perigo";
        botaoRemover.textContent = "Remover";
        botaoRemover.addEventListener("click", () =>
            removerConta(conta.cpf)
        );

        container.append(botaoExtrato, botaoTaxa, botaoRemover);
        acoes.appendChild(container);
        linha.appendChild(acoes);

        corpoTabela.appendChild(linha);
    });

    $("pagina-atual").textContent = `Página ${paginaAdmin + 1}`;

    const totalPaginas = Math.ceil((resposta.total ?? 0) / TAMANHO_PAGINA);

    $("pagina-anterior").disabled = paginaAdmin === 0;
    $("pagina-proxima").disabled = paginaAdmin + 1 >= totalPaginas;
}

async function verExtratoAdmin(cpf) {
    try {
        const extrato = await apiFetch(`/contas/admin/${cpf}/extrato`);
        const transacoes = extrato.transacoes ?? [];

        if (transacoes.length === 0) {
            mostrarMensagem("mensagem-admin", "Esta conta não possui transações.");
            return;
        }

        const ultima = transacoes[transacoes.length - 1];

        mostrarMensagem(
            "mensagem-admin",
            `Última movimentação de ${cpf}: ${ultima.tipo} de ${formatarMoeda(ultima.valor)}.`
        );
    } catch (erro) {
        tratarErro("mensagem-admin", erro);
    }
}

async function cobrarTaxa(cpf) {
    try {
        await apiFetch(`/contas/admin/${cpf}/taxa-manutencao`, {
            method: "POST"
        });

        mostrarMensagem("mensagem-admin", "Taxa de manutenção cobrada com sucesso.");
        await carregarContasAdmin();
    } catch (erro) {
        tratarErro("mensagem-admin", erro);
    }
}

async function removerConta(cpf) {
    const confirmado = confirm(`Remover a conta do CPF ${cpf}?`);

    if (!confirmado) {
        return;
    }

    try {
        await apiFetch(`/contas/admin/${cpf}`, {
            method: "DELETE"
        });

        mostrarMensagem("mensagem-admin", "Conta removida com sucesso.");
        await carregarContasAdmin();
    } catch (erro) {
        tratarErro("mensagem-admin", erro);
    }
}

$("form-buscar-admin").addEventListener("submit", async (evento) => {
    evento.preventDefault();

    const cpf = $("cpf-busca").value.trim();
    const resultado = $("resultado-busca");

    resultado.innerHTML = "";

    try {
        const conta = await apiFetch(`/contas/admin/${cpf}`);

        resultado.innerHTML = `
            <div class="dado">
                <span class="texto-secundario">Titular</span>
                <span>${conta.titular ?? "—"}</span>
            </div>

            <div class="dado">
                <span class="texto-secundario">CPF</span>
                <span>${conta.cpf ?? "—"}</span>
            </div>

            <div class="dado">
                <span class="texto-secundario">Tipo</span>
                <span>${conta.tipo ?? "—"}</span>
            </div>

            <div class="dado">
                <span class="texto-secundario">Saldo</span>
                <span>${formatarMoeda(conta.saldo)}</span>
            </div>
        `;
    } catch (erro) {
        resultado.innerHTML = "";
        tratarErro("mensagem-admin", erro);
    }
});

$("pagina-anterior").addEventListener("click", async () => {
    if (paginaAdmin > 0) {
        paginaAdmin--;
        await carregarContasAdmin();
    }
});

$("pagina-proxima").addEventListener("click", async () => {
    paginaAdmin++;
    await carregarContasAdmin();
});

/* ---------- INICIALIZAÇÃO ---------- */

document.addEventListener("DOMContentLoaded", async () => {
    if (!estaAutenticado()) {
        mostrarTela("tela-login");
        return;
    }

    try {
        await carregarConta();
        await verificarSeEhAdmin();
    } catch (erro) {
        limparCredenciais();
        mostrarTela("tela-login");
        tratarErro("mensagem-login", erro);
    }
});