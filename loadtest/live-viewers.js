import http from 'k6/http'
import ws from 'k6/ws'
import { check, fail, sleep } from 'k6'
import { Trend, Counter } from 'k6/metrics'

const API = __ENV.API || 'http://localhost:8080'
const WS_URL = __ENV.WS_URL || 'ws://localhost:8085/ws/stock'
const VIEWERS = Number(__ENV.VIEWERS || 200)
const SECONDS = Number(__ENV.SECONDS || 120)
const JSON_HEADERS = { 'Content-Type': 'application/json' }

// commit -> screen latency, measured on every viewer for every update
const latency = new Trend('live_update_latency', true)
const received = new Counter('live_updates_received')
const connected = new Counter('ws_connected')

export const options = {
    setupTimeout: '60s',
    scenarios: {
        viewers: {
            executor: 'per-vu-iterations',
            vus: VIEWERS,
            iterations: 1,
            maxDuration: '10m',
            exec: 'viewer',
        },
        buyers: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || 5),
            timeUnit: '1s',
            duration: `${SECONDS}s`,
            startTime: '15s', // give every viewer time to connect first
            preAllocatedVUs: 10,
            maxVUs: 30,
            exec: 'buyer',
        },
    },
    thresholds: {
        live_update_latency: ['p(95)<500'],
        'http_req_failed{name:place_order}': ['rate<0.01'],
    },
}

export function setup() {
    const login = http.post(`${API}/api/auth/login`,
        JSON.stringify({ email: __ENV.ADMIN_EMAIL, password: __ENV.ADMIN_PASSWORD }),
        { headers: JSON_HEADERS })
    if (login.status !== 200) fail(`Admin login failed: ${login.status}`)
    const token = login.json('accessToken')

    const product = http.post(`${API}/api/products`,
        JSON.stringify({ name: `Live Test ${Date.now()}`, description: 'Live latency', pricePaise: 49900, stock: 1000000 }),
        { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } })
    if (product.status !== 201 && product.status !== 200) fail(`Create product failed: ${product.status}`)
    return { token, productId: product.json('id') }
}

export function viewer(data) {
    sleep(Math.random() * 10) // viewers arrive over 10 s, like real users
    const res = ws.connect(WS_URL, {}, (socket) => {
        socket.on('open', () => connected.add(1))
        socket.on('message', (raw) => {
            const now = Date.now()
            const msg = JSON.parse(raw)
            if (msg.type === 'stock' && msg.productId === data.productId) {
                latency.add(now - msg.changedAtEpochMs)
                received.add(1)
            }
        })
        socket.setTimeout(() => socket.close(), (SECONDS + 35) * 1000)
    })
    check(res, { 'websocket upgraded (101)': (r) => r && r.status === 101 })
}

export function buyer(data) {
    const res = http.post(`${API}/api/orders`,
        JSON.stringify({ productId: data.productId, quantity: 1 }),
        { headers: { ...JSON_HEADERS, Authorization: `Bearer ${data.token}` }, tags: { name: 'place_order' } })
    check(res, { 'order accepted (202)': (r) => r.status === 202 })
}