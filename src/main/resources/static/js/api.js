const API_BASE_URL = "";

let credenciais = null;
let usuarioLogado = null;

export function definirCredenciais(usuario, senha) {
    credenciais = btoa(`${usuario}:${senha}`);
    usuarioLogado = usuario;
}

export function limparCredenciais() {
    credenciais = null;
    usuarioLogado = null;
}

export function obterUsuarioLogado() {
    return usuarioLogado;
}

export function estaAutenticado() {
    return credenciais !== null;
}

export async function apiFetch(caminho, opcoes = {}) {
    const headers = {
        "Content-Type": "application/json",
        ...opcoes.headers
    };

    if (credenciais) {
        headers.Authorization = `Basic ${credenciais}`;
    }

    let resposta;
    try {
        resposta = await fetch(`${API_BASE_URL}${caminho}`, {
            ...opcoes,
            headers
        });
    } catch (e) {
        const erroConexao = new Error("Não foi possível conectar ao servidor. Verifique se a aplicação está rodando.");
        erroConexao.status = 0;
        throw erroConexao;
    }

    if (resposta.status === 401) {
        limparCredenciais();
        const erroAuth = new Error("Credenciais inválidas ou sessão expirada. Faça login novamente.");
        erroAuth.status = 401;
        throw erroAuth;
    }

    if (!resposta.ok) {
        let mensagem = "Ocorreu um erro ao processar sua solicitação.";
        let detalhes = null;

        try {
            const problema = await resposta.json();
            mensagem = problema.detail || problema.title || mensagem;
            if (problema.errors && Array.isArray(problema.errors)) {
                detalhes = problema.errors.map(e => e.defaultMessage || e.message || e).join(". ");
            }
        } catch {
            // fallback para mensagem genérica
        }

        const erro = new Error(detalhes ? `${mensagem}: ${detalhes}` : mensagem);
        erro.status = resposta.status;
        throw erro;
    }

    if (resposta.status === 204) {
        return null;
    }

    return resposta.json();
}
