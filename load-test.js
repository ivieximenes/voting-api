import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 50 },   // ramp-up
    { duration: '1m', target: 50 },    // steady
    { duration: '30s', target: 0 },    // ramp-down
  ],
  thresholds: {
    http_req_duration: ['p(95)<500'],  // p95 < 500ms
    http_req_failed: ['rate<0.01'],    // < 1% de erro
  },
};

const BASE_URL = __ENV.API_URL || 'http://localhost:8080';

export default function () {
  // Cria pauta
  const createRes = http.post(`${BASE_URL}/api/v1/topics`, JSON.stringify({
    title: `Pauta ${__VU}-${__ITER}`,
    description: 'Load test',
  }), { headers: { 'Content-Type': 'application/json' } });

  check(createRes, { 'created': (r) => r.status === 201 });
  if (createRes.status !== 201) return;

  const topicId = createRes.json('id');

  // Abre sessão
  http.post(`${BASE_URL}/api/v1/topics/${topicId}/sessions`, JSON.stringify({
    durationSeconds: 300,
  }), { headers: { 'Content-Type': 'application/json' } });

  // Vota (CPF válido pelo algoritmo)
  const memberId = '11144477' + String(__VU).padStart(3, '0');
  http.post(`${BASE_URL}/api/v1/topics/${topicId}/votes`, JSON.stringify({
    memberId: '11144477735',
    option: 'SIM',
  }), { headers: { 'Content-Type': 'application/json' } });

  // Apura
  http.get(`${BASE_URL}/api/v1/topics/${topicId}/result`);

  sleep(1);
}