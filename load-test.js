import http from 'k6/http';
import { check, sleep } from 'k6';

/*
 * Testa o fluxo de votação sob carga em UMA única pauta.
 * A pauta e a sessão são criadas uma única vez no
 * setup()
 */

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // ramp-up
    { duration: '1m', target: 50 },    // steady
    { duration: '30s', target: 0 },    // ramp-down
  ],
  thresholds: {
    // Geral: inclui todos os endpoints (criação, sessão, voto, resultado)
    http_req_duration: ['p(95)<500'],
    // Voto isolado: tolerância maior, pois inclui a chamada externa de
    // validação de CPF (rede + serviço de terceiro)
    'http_req_duration{name:vote}': ['p(95)<1000'],
    'http_req_duration{name:result}': ['p(95)<300'],
    // Falha de verdade (5xx, timeout, conexão recusada) deve ser rara.
    // 422 (inelegível) e 409 (voto duplicado) são respostas de negócio
    // válidas, não falhas de infraestrutura — ver setResponseCallback abaixo.
    http_req_failed: ['rate<0.01'],
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';
const SESSION_DURATION_SECONDS = 600;
http.setResponseCallback(http.expectedStatuses(200, 201, 404, 409, 422));

// --------------------------------------------------------------------
// Geração de CPF válido
// --------------------------------------------------------------------

function calcDigit(digits, length) {
  let sum = 0;
  for (let i = 0; i < length; i++) {
    sum += digits[i] * (length + 1 - i);
  }
  const remainder = sum % 11;
  return remainder < 2 ? 0 : 11 - remainder;
}

function generateValidCpf(seed) {
  const base = String(seed % 900000000 + 100000000)
    .padStart(9, '0')
    .split('')
    .map(Number);

  const d1 = calcDigit(base, 9);
  const withD1 = [...base, d1];
  const d2 = calcDigit(withD1, 10);
  return [...withD1, d2].join('');
}

// --------------------------------------------------------------------
// Setup: roda uma única vez, cria a pauta e abre a sessão compartilhada
// por todas as VUs durante todo o teste.
// --------------------------------------------------------------------

export function setup() {
  const createRes = http.post(
    `${BASE_URL}/api/v1/topics`,
    JSON.stringify({ title: 'Pauta de carga', description: 'Load test k6' }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'create_topic' } }
  );

  if (createRes.status !== 201) {
    throw new Error(`Falha ao criar pauta no setup: HTTP ${createRes.status}`);
  }

  const topicId = createRes.json('id');

  const sessionRes = http.post(
    `${BASE_URL}/api/v1/topics/${topicId}/sessions`,
    JSON.stringify({ durationSeconds: SESSION_DURATION_SECONDS }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'open_session' } }
  );

  if (sessionRes.status !== 201) {
    throw new Error(`Falha ao abrir sessão no setup: HTTP ${sessionRes.status}`);
  }

  return { topicId };
}

// --------------------------------------------------------------------
// Cada iteração vota na MESMA pauta com um CPF diferente,
// simulando volume real de votos concorrentes numa única pauta.
// --------------------------------------------------------------------

export default function (data) {
  const { topicId } = data;
  const cpf = generateValidCpf(__VU * 1000000 + __ITER);

  const voteRes = http.post(
    `${BASE_URL}/api/v1/topics/${topicId}/votes`,
    JSON.stringify({ memberId: cpf, option: __ITER % 2 === 0 ? 'YES' : 'NO' }),
    { headers: { 'Content-Type': 'application/json' }, tags: { name: 'vote' } }
  );

  check(voteRes, {
    'voto aceito (201) ou rejeitado por elegibilidade (422)': (r) =>
      r.status === 201 || r.status === 422,
  });

  const resultRes = http.get(`${BASE_URL}/api/v1/topics/${topicId}/result`, {
    tags: { name: 'result' },
  });

  check(resultRes, {
    'resultado consultado com sucesso (200)': (r) => r.status === 200,
  });

  sleep(1);
}