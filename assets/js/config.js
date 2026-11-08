
window.SSMP_CONFIG = {
    brand: {
        name: "SSMP",
        fullName: "Siddy's Simple Plugin",
        subtitle: "DOCUMENTATION",
        logoText: "S",
        version: "2.0.0",
        author: "Siddharth C",
        copyright: "© 2026 Siddharth C",
        license: "MIT",
        description: "A powerful, lightweight Minecraft server plugin designed to replace them all whilst being so simple that even a child understands."
    },

    theme: {
        defaultMode: "dark",
        allowModeSwitch: true,

        colors: {
            dark: {
                background: "#070b16",
                backgroundSecondary: "#0b1121",
                accent: "#76a9ff",
                accentStrong: "#4389ff"
            },
            light: {
                background: "#edf2fb",
                backgroundSecondary: "#f5f7fc",
                accent: "#326fd1",
                accentStrong: "#245fc0"
            }
        },

        background: {
            style: "cosmic",
            showGrid: true,
            showStars: true,
            showNebula: true,
            gridSize: 58,
            gridOpacity: 0.2,
            starOpacity: 0.55,
            nebulaOpacity: 0.13,

            gradient: {
                primary: "#080e1e",
                secondary: "#070b17",
                tertiary: "#0b1020"
            },

            nebula: {
                first: "#1765e9",
                second: "#5839d9"
            }
        },

        glass: {
            enabled: true,
            blur: 24,
            opacity: 0.72
        },

        animations: {
            enabled: true,
            pageTransitions: true,
            hoverEffects: true,
            backgroundEffects: true
        },

        typography: {
            primaryFont: "Inter",
            monospaceFont: "DM Mono"
        }
    },

    layout: {
        sidebarWidth: 278,
        topbarHeight: 72,
        tocWidth: 220,
        contentWidth: 820,
        showTableOfContents: true,
        showBreadcrumbs: true,
        showPreviousNext: true,
        showDocumentFooter: true,
        mobileSidebar: true
    },

    navigation: {
        showAddPageButton: true,
        allowPageEditing: true,
        allowPageDeletion: true,
        allowPageReordering: true,
        allowNestedPages: true,
        rememberExpandedGroups: true
    },

    search: {
        enabled: true,
        keyboardShortcut: "k",
        placeholder: "Search documentation...",
        searchDescriptions: true,
        searchContent: true,
        maxResults: 12
    },

    editor: {
        enabled: true,
        defaultCategory: "Custom Pages",
        allowMarkdown: true,
        saveAutomatically: false,
        confirmDeletion: true
    },

    storage: {
        prefix: "ssmp_docs_",
        saveTheme: true,
        savePages: true,
        saveNavigation: true
    },

    links: {
        github: "https://github.com/SiddharthC0/siddys-simple-plugin",
        issues: "https://github.com/SiddharthC0/siddys-simple-plugin/issues",
        modrinth: "https://modrinth.com/plugin/siddys-simple-plugin",
        hangar: "https://hangar.papermc.io/",
        support: "https://github.com/SiddharthC0/siddys-simple-plugin/issues"
    },

    footer: {
        left: "Designed for SSMP",
        right: "Made by Siddharth C",
        showLicense: true
    },

    content: {
        defaultPage: "getting-started-welcome",
        showLastUpdated: true,
        showVersionBadge: true,
        headingAnchorLinks: true,
        codeCopyButtons: true,
        syntaxHighlighting: true
    },

    advanced: {
        debug: false,
        allowExternalLinks: true,
        externalLinksNewTab: true,
        scrollToTopOnNavigation: true
    }
};
