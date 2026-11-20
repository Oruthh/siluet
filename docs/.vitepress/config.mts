import { defineConfig } from 'vitepress'

export default defineConfig({
    title: "Siluet",
    description: "AST-based templating engine for Kotlin: pages composed in JSON, views in plain HTML",
    outDir: "../build/docs/vitepress",
    head: [
        ['link', { rel: 'icon', type: 'image/svg+xml', href: '/images/logo.svg' }],
        ['meta', { property: 'og:title', content: 'Siluet' }],
        ['meta', { property: 'og:description', content: 'AST-based templating engine for Kotlin: pages composed in JSON, views in plain HTML' }]
    ],
    themeConfig: {
        logo: {
            light: '/images/logo.svg',
            dark: '/images/logo.svg',
            alt: 'Siluet Logo'
        },
        nav: [
            { text: 'Guide', link: '/guide/getting-started' },
            { text: 'Reference', link: '/reference/syntax', activeMatch: '/reference/' },
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
                text: 'Writing Templates',
                items: [
                    { text: 'Template Syntax', link: '/reference/syntax' },
                    { text: 'Directives', link: '/reference/directives' },
                    { text: 'Built-in Filters', link: '/reference/filters' },
                    { text: 'Template Configuration', link: '/reference/template-configuration' },
                    { text: 'Localization', link: '/reference/i18n' }
                ]
            },
            {
                text: 'Kotlin Integration',
                items: [
                    { text: 'Data Model & Security', link: '/reference/data-model' },
                    { text: 'Extending Siluet', link: '/reference/extending' },
                    { text: 'Error Handling', link: '/reference/errors' },
                    { text: 'API Reference (KDoc)', link: '/api/index.html', target: '_self' }
                ]
            }
        ],
        outline: [2, 3],
        search: {
            provider: 'local'
        },
        editLink: {
            pattern: 'https://github.com/Oruthh/siluet/edit/master/docs/:path',
            text: 'Edit this page on GitHub'
        },
        socialLinks: [
            { icon: 'github', link: 'https://github.com/Oruthh/siluet' }
        ],
        footer: {
            message: 'Released under the Mozilla Public License 2.0.',
            copyright: 'The Siluet name and logo are not covered by the MPL.'
        }
    }
})
