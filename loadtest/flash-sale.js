import http from 'k6/http';
import { check, sleep, fail } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const API = __ENV.API;
const STOCK = Number(__ENV.STOCK || 50);
const REQUESTS = Number(__ENV.REQUESTS || 300);

const confirmed = new Counter('orders_confirmed');
const cancelled = new Counter('orders_cancelled');
const stuck = new Counter('orders_stuck');
const sagaTime = new Trend('saga_completion_ms', true);

export const options = {
    scenarios: {
        flash_sale: {
            executor: 'shared-iterations',
            vus: 100,
            iterations: REQUESTS,
            maxDuration: '3m',
        },
    },
    thresholds: {
        'http_req_failed{name:place_order}': ['rate<0.01'],
        'http_req_duration{name:place_order}': ['p(95)<1500'],
        orders_stuck: ['count==0'],
    },
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };

// Runs once: log in as admin and create a product with limited stock
export function setup() {
    const login = http.post(`${API}/api/auth/login`,
        JSON.stringify({ email: __ENV.ADMIN_EMAIL, password: __ENV.ADMIN_PASSWORD }),
        { headers: JSON_HEADERS });
    if (login.status !== 200) fail(`Admin login failed: ${login.status}`);
    const token = login.json('accessToken');

    const product = http.post(`${API}/api/products`,
        JSON.stringify({
            name: `Flash Sale Phone ${Date.now()}`,
            description: 'Limited-stock flash sale item',
            pricePaise: 999900,
            stock: STOCK,
        }),
        { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } });
    if (product.status !== 200 && product.status !== 201) fail(`Create product failed: ${product.status}`);

    console.log(`Flash sale started: product ${product.json('id')}, stock ${STOCK}, ${REQUESTS} orders incoming`);
    return { token, productId: product.json('id') };
}

// Each iteration = one shopper: place an order, then poll until the saga finishes
export default function (data) {
    const headers = { ...JSON_HEADERS, Authorization: `Bearer ${data.token}` };
    const start = Date.now();

    const res = http.post(`${API}/api/orders`,
        JSON.stringify({ productId: data.productId, quantity: 1 }),
        { headers, tags: { name: 'place_order' } });
    if (!check(res, { 'order accepted (202)': (r) => r.status === 202 })) return;

    const id = res.json('id');
    for (let i = 0; i < 80; i++) {
        sleep(0.25);
        const status = http.get(`${API}/api/orders/${id}`,
            { headers, tags: { name: 'poll_order' } }).json('status');
        if (status === 'CONFIRMED') { confirmed.add(1); sagaTime.add(Date.now() - start); return; }
        if (status === 'CANCELLED') { cancelled.add(1); sagaTime.add(Date.now() - start); return; }
    }
    stuck.add(1);
}

// Runs once at the end: the oversell check
export function teardown(data) {
    const p = http.get(`${API}/api/products/${data.productId}`,
        { headers: { Authorization: `Bearer ${data.token}` } });
    const stock = p.json('stock');
    console.log(`FINAL STOCK: ${stock} (expected 0, never negative)`);
    check(stock, { 'no overselling (stock == 0)': (s) => s === 0 });
}