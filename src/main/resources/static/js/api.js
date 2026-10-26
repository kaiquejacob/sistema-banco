const API_BASE_URL = "";

let credenciais = null;

export function definirCredenciais(cpf, senha) {
    credenciais = btoa(`${cpf}:${senha}`);
}

export function limparCredenciais() {
    credenciais = null;
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

    const resposta = await fetch(`${API_BASE_URL}${caminho}`, {
        ...opcoes,
        headers
    });

    if (resposta.status === 401) {
        limparCredenciais();
        throw new Error("Credenciais inválidas ou sessão expirada.");
    }

    if (!resposta.ok) {
        let mensagem = "Ocorreu um erro inesperado.";

        try {
            const problema = await resposta.json();
            mensagem = problema.detail || mensagem;
        } catch {
            // mantém a mensagem padrão
        }

        const erro = new Error(mensagem);
        erro.status = resposta.status;
        throw erro;
    }

    if (resposta.status === 204) {
        return null;
    }

    return resposta.json();
}