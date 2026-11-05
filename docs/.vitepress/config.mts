import { defineConfig } from 'vitepress'

export default defineConfig({
    title: "Siluet",
    description: "AST-based modular templating engine for Kotlin",
    outDir: "../build/docs/vitepress",
    head: [
        ['link', { rel: 'icon', type: 'image/svg+xml', href: '/images/logo.svg' }]
    ],
    themeConfig: {
        logo: {
            light: '/images/logo.svg',
            dark: '/images/logo.svg',
            alt: 'Siluet Logo'
        },
        nav: [
            { text: 'Guide', link: '/guide/getting-started' },
            { text: 'API Reference', link: '/api/index.html', target: '_self' }
        ],
        sidebar: [
            {
                text: 'Introduction',
                items: [
                    { text: 'Getting Started', link: '/guide/getting-started' }
                ]
            },
            {
                text: 'Template Engine',
                items: [
                    { text: 'Built-in Filters', link: '/reference/filters' },
                    { text: 'Directives', link: '/reference/directives' },
                    { text: 'Localization', link: '/reference/i18n' }
                ]
            }
        ],
        socialLinks: [
            { icon: 'github', link: 'https://github.com/Oruthh/siluet' }
        ]
    }
})