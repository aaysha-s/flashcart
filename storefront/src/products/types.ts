export interface Product {
    id: number
    name: string
    description: string | null
    pricePaise: number
    stock: number
    createdAt: string
    saleStartsAt: string | null
}

export interface Page<T> {
    content: T[]
    page: { size: number; number: number; totalElements: number; totalPages: number }
}