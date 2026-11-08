(() => {
    "use strict";

    const icons = {
        sparkles: '<path d="m12 3 1.9 5.8L20 11l-6.1 2.2L12 19l-1.9-5.8L4 11l6.1-2.2L12 3Z"/><path d="m19 14 1.2 2.8L23 18l-2.8 1.2L19 22l-1.2-2.8L15 18l2.8-1.2L19 14Z"/>',

        layers: '<path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="m3 12 9 5 9-5"/><path d="m3 16 9 5 9-5"/>',

        command: '<path d="M9 6V5a2 2 0 1 0-2 2h10a2 2 0 1 0-2-2v14a2 2 0 1 0 2-2H7a2 2 0 1 0 2 2V6Z"/>',

        home: '<path d="m3 10 9-7 9 7v10a1 1 0 0 1-1 1h-5v-7H9v7H4a1 1 0 0 1-1-1V10Z"/>',

        download: '<path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><path d="m7 10 5 5 5-5"/><path d="M12 15V3"/>',

        rocket: '<path d="M4.5 16.5c-1.5 1.2-2 4.5-2 4.5s3.3-.5 4.5-2c.7-.8.7-2.1-.1-2.9-.8-.8-2.1-.8-2.9.4Z"/><path d="m12 15-3-3a22 22 0 0 1 2-3.5C14.5 4 19 3 21 3c0 2-1 6.5-5.5 10A22 22 0 0 1 12 15Z"/><path d="m9 12-4 1 3 3"/><path d="m12 15-1 4-3-3"/>',

        puzzle: '<path d="M19 13h-2a2 2 0 1 1-4 0V5h-2a2 2 0 1 0-4 0v2H5a2 2 0 1 0 0 4h2v8h8v-2a2 2 0 1 1 4 0v-2Z"/>',

        package: '<path d="m12 3 9 5-9 5-9-5 9-5Z"/><path d="M3 8v9l9 5 9-5V8"/><path d="M12 13v9"/>',

        shuffle: '<path d="m18 14 4 4-4 4"/><path d="m18 2 4 4-4 4"/><path d="M2 18h2.5a6 6 0 0 0 4.2-1.7L17 8"/><path d="M2 6h2.5a6 6 0 0 1 4.2 1.7L17 16"/><path d="M18 18h4"/><path d="M18 6h4"/>',

        settings: '<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.9l.1.1-1.8 1.8-.1-.1a1.7 1.7 0 0 0-1.9-.3 1.7 1.7 0 0 0-1 1.5v.1h-2.6v-.1a1.7 1.7 0 0 0-1-1.5 1.7 1.7 0 0 0-1.9.3l-.1.1-1.8-1.8.1-.1a1.7 1.7 0 0 0 .3-1.9 1.7 1.7 0 0 0-1.5-1H6.4v-2.6h.1a1.7 1.7 0 0 0 1.5-1 1.7 1.7 0 0 0-.3-1.9l-.1-.1 1.8-1.8.1.1a1.7 1.7 0 0 0 1.9.3 1.7 1.7 0 0 0 1-1.5v-.1H15v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.9-.3l.1-.1 1.8 1.8-.1.1a1.7 1.7 0 0 0-.3 1.9 1.7 1.7 0 0 0 1.5 1h.1V14h-.1a1.7 1.7 0 0 0-1.5 1Z"/>',

        book: '<path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H20v17H6.5A2.5 2.5 0 0 1 4 17.5v-12Z"/><path d="M4 17.5A2.5 2.5 0 0 1 6.5 15H20"/><path d="M8 7h8"/>',

        plus: '<path d="M12 5v14M5 12h14"/>',

        chevron: '<path d="m7 10 5 5 5-5"/>'
    };

    function escape(value) {
        return String(value ?? "").replace(/[&<>"']/g, character => ({
            "&": "&amp;",
            "<": "&lt;",
            ">": "&gt;",
            '"': "&quot;",
            "'": "&#39;"
        })[character]);
    }

    function icon(name, size = 18) {
        const drawing = icons[name] || icons.book;

        return `
            <svg
                width="${Number(size) || 18}"
                height="${Number(size) || 18}"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
            >
                ${drawing}
            </svg>
        `;
    }

    function render(container, sections, pages, activePage, expanded, callbacks) {
        if (!container) return;

        container.innerHTML = sections.map(section => {
            const sectionPages = pages.filter(
                page => page.category === section.title
            );

            if (!sectionPages.length) return "";

            const isExpanded = expanded[section.id] !== false;

            return `
                <section class="nav-group" data-section="${escape(section.id)}">

                    <button
                        class="nav-group-header ${isExpanded ? "expanded" : "collapsed"}"
                        type="button"
                        data-group="${escape(section.id)}"
                        aria-expanded="${isExpanded}"
                    >
                        <span class="nav-group-icon">
                            ${icon(section.icon, 16)}
                        </span>

                        <span class="nav-group-title">
                            ${escape(section.title)}
                        </span>

                        <span class="nav-group-chevron">
                            ${icon("chevron", 15)}
                        </span>
                    </button>

                    <div
                        class="nav-group-pages"
                        ${isExpanded ? "" : "hidden"}
                    >
                        ${sectionPages.map(page => `
                            <button
                                class="nav-item ${page.id === activePage ? "active" : ""}"
                                type="button"
                                data-page="${escape(page.id)}"
                                ${page.id === activePage ? 'aria-current="page"' : ""}
                            >
                                <span class="nav-item-icon">
                                    ${icon(page.icon, 17)}
                                </span>

                                <span class="nav-item-title">
                                    ${escape(page.title)}
                                </span>
                            </button>
                        `).join("")}
                    </div>

                </section>
            `;
        }).join("");

        container.querySelectorAll("[data-page]").forEach(button => {
            button.addEventListener("click", () => {
                callbacks.navigate(button.dataset.page);
            });
        });

        container.querySelectorAll("[data-group]").forEach(button => {
            button.addEventListener("click", () => {
                callbacks.toggleGroup(button.dataset.group);
            });
        });
    }

    window.SSMP_NAVIGATION = {
        render,
        icon,
        escape
    };
})();