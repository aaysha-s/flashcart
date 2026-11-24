import http from 'k6/http'
import { check, fail } from 'k6'

const API = __ENV.API
const JSON_HEADERS = { 'Content-Type': 'application/json' }

export const options = {
    setupTimeout: '180s',
    scenarios: {
        steady: {
            executor: 'constant-arrival-rate',
            rate: Number(__ENV.RATE || 15),
            timeUnit: '1s',
            duration: __ENV.DURATION || '4m',
            preAllocatedVUs: 50,
            maxVUs: 150,
        },
    },
    thresholds: {
        'http_req_failed{name:place_order}': ['rate<0.01'],
    },
}

export function setup() {
    const login = http.post(`${API}/api/auth/login`,
        JSON.stringify({ email: __ENV.ADMIN_EMAIL, password: __ENV.ADMIN_PASSWORD }),
        { headers: JSON_HEADERS })
    if (login.status !== 200) fail(`Admin login failed: ${login.status}`)
    const token = login.json('accessToken')

    if (__ENV.PRODUCT_ID) {
        return { token, productId: Number(__ENV.PRODUCT_ID) }
    }

    const product = http.post(`${API}/api/products`,
        JSON.stringify({ name: `Load Test ${Date.now()}`, description: 'Sustained load', pricePaise: 49900, stock: 100000 }),
        { headers: { ...JSON_HEADERS, Authorization: `Bearer ${token}` } })
    if (product.status !== 201 && product.status !== 200) fail(`Create product failed: ${product.status}`)
    return { token, productId: product.json('id') }
}

export default function (data) {
    const res = http.post(`${API}/api/orders`,
        JSON.stringify({ productId: data.productId, quantity: 1 }),
        { headers: { ...JSON_HEADERS, Authorization: `Bearer ${data.token}` }, tags: { name: 'place_order' } })
    check(res, { 'order accepted (202)': (r) => r.status === 202 })
}