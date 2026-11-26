// A small icon and a tint for each product card, picked from the product name.
// Purely decorative: the product data has no images.

const ICONS: Array<[RegExp, string]> = [
    [/ear ?buds?|headphone|headset/i, '🎧'],
    [/watch/i, '⌚'],
    [/keyboard/i, '⌨️'],
    [/mouse/i, '🖱️'],
    [/power ?bank|charger|battery/i, '🔋'],
    [/speaker|sound/i, '🔊'],
    [/tv|television|monitor|screen/i, '📺'],
    [/laptop|notebook/i, '💻'],
    [/phone|mobile/i, '📱'],
    [/camera/i, '📷'],
    [/lamp|light|bulb/i, '💡'],
    [/backpack|bag/i, '🎒'],
    [/shoe|sneaker/i, '👟'],
    [/book/i, '📚'],
    [/coffee|mug/i, '☕'],
    [/game|console|controller/i, '🎮'],
]

const TINTS = 6

export function productVisual(name: string, id: number): { icon: string; tint: number } {
    const match = ICONS.find(([pattern]) => pattern.test(name))
    return { icon: match ? match[1] : '🛍️', tint: id % TINTS }
}
