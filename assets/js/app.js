(() => {
    "use strict";

    const config = window.SSMP_CONFIG || {};
    const $ = selector => document.querySelector(selector);

    const state = {
        sections: [],
        pages: [],
        activePage: config.content?.defaultPage || "getting-started-welcome",
        theme: "dark",
        expanded: {
            "Getting Started": true
        },
        sidebarOpen: false,
        editorMode: "",
        originalPage: null
    };

    const prefix = config.storage?.prefix || "ssmp_docs_";

    const el = {
        navigation: $("#documentation-navigation"),
        document: $("#document-content"),
        toc: $("#table-of-contents"),
        breadcrumbs: $("#breadcrumbs"),
        sidebar: $("#sidebar"),
        backdrop: $("#sidebar-backdrop"),
        mobileMenu: $("#mobile-menu-button"),
        theme: $("#theme-toggle"),
        searchTrigger: $("#search-trigger"),
        searchOverlay: $("#search-overlay"),
        searchInput: $("#search-input"),
        searchResults: $("#search-results"),
        addPage: $("#add-page-button"),
        editor: $("#editor-overlay"),
        editorForm: $("#page-editor-form"),
        editorId: $("#editor-page-id"),
        editorTitle: $("#editor-page-title"),
        editorCategory: $("#editor-page-category"),
        editorIcon: $("#editor-page-icon"),
        editorContent: $("#editor-page-content"),
        toast: $("#toast-container")
    };

    const escape = window.SSMP_MARKDOWN.escape;

    function read(key, fallback) {
        try {
            const value = localStorage.getItem(prefix + key);
            return value === null ? fallback : JSON.parse(value);
        } catch {
            return fallback;
        }
    }

    function write(key, value) {
        // lol
    }

    function notify(message, type = "success") {
    }

    function applyConfig() {
        const root = document.documentElement;
        const layout = config.layout || {};
        const theme = config.theme || {};
        const background = theme.background || {};
        const glass = theme.glass || {};
        const typography = theme.typography || {};
        const gradient = background.gradient || {};
        const nebula = background.nebula || {};

        const variables = {
            "--sidebar-width": `${layout.sidebarWidth || 278}px`,
            "--topbar-height": `${layout.topbarHeight || 72}px`,
            "--toc-width": `${layout.tocWidth || 220}px`,
            "--content-width": `${layout.contentWidth || 820}px`,
            "--glass-blur": `${glass.blur ?? 24}px`,
            "--glass-opacity": glass.opacity ?? 0.72,
            "--font-primary": `"${typography.primaryFont || "Inter"}", sans-serif`,
            "--font-mono": `"${typography.monospaceFont || "DM Mono"}", monospace`,
            "--space-primary": gradient.primary || "#080e1e",
            "--space-secondary": gradient.secondary || "#070b17",
            "--space-tertiary": gradient.tertiary || "#0b1020",
            "--nebula-first": nebula.first || "#1765e9",
            "--nebula-second": nebula.second || "#5839d9",
            "--grid-size": `${background.gridSize || 58}px`,
            "--grid-opacity": background.gridOpacity ?? 0.2,
            "--star-opacity": background.starOpacity ?? 0.55,
            "--nebula-opacity": background.nebulaOpacity ?? 0.13
        };

        Object.entries(variables).forEach(([key, value]) => {
            root.style.setProperty(key, value);
        });

        if (background.showGrid === false) {
            $(".space-grid")?.remove();
        }

        if (background.showStars === false) {
            $(".space-stars")?.remove();
        }

        if (background.showNebula === false) {
            document.querySelectorAll(".nebula").forEach(node => node.remove());
        }

        if (glass.enabled === false) {
            root.style.setProperty("--glass-blur", "0px");
            root.style.setProperty("--glass-opacity", "1");
        }

        if (theme.animations?.enabled === false) {
            root.classList.add("animations-disabled");
        }

        const brand = config.brand || {};

        if ($("#project-version")) {
            $("#project-version").textContent = brand.version || "";
        }

        const links = config.links || {};

        const destinations = {
            "#github-link": links.github,
            "#help-link": links.support || links.issues,
            "#modrinth-link": links.modrinth,
            "#hangar-link": links.hangar
        };

        Object.entries(destinations).forEach(([selector, url]) => {
            const link = $(selector);

            if (link && url) {
                link.href = url;
            }
        });
    }

    function applyTheme(theme) {
    state.theme = theme === "light" ? "light" : "dark";

    document.documentElement.dataset.theme = state.theme;

    if (el.theme) {
        el.theme.setAttribute(
            "aria-label",
            state.theme === "dark"
                ? "Switch to light mode"
                : "Switch to dark mode"
        );

        el.theme.title = state.theme === "dark"
            ? "Switch to light mode"
            : "Switch to dark mode";
    }

    if (config.storage?.saveTheme !== false) {
        write("theme", state.theme);
    }
}

    function getPage(id) {
        return state.pages.find(page => page.id === id);
    }

    function getMetadata(markdown, path, category) {
        const lines = markdown.split(/\r?\n/);

        const title = lines.find(line => /^#\s+/.test(line))
            ?.replace(/^#\s+/, "")
            .trim()
            || path.split("/").pop()
                .replace(/\.md$/i, "")
                .replace(/[-_]/g, " ");

        const description = lines.find(line =>
            line.trim() && !line.startsWith("#")
        )?.trim() || "";

        return {
            id: path
                .replace(/\.md$/i, "")
                .replace(/[\/]/g, "-")
                .replace(/[^a-z0-9-]/gi, "-")
                .toLowerCase(),
            title,
            category,
            icon: "book",
            description,
            keywords: [title, category],
            content: markdown,
            custom: false,
            source: "markdown"
        };
    }

    async function loadDocumentation() {
        const response = await fetch("docs/manifest.json", {
            cache: "no-cache"
        });

        if (!response.ok) {
            throw new Error(
                `Unable to load docs/manifest.json (${response.status}).`
            );
        }

        const manifest = await response.json();

        if (!Array.isArray(manifest.sections)) {
            throw new Error("The manifest has no sections array.");
        }

        state.sections = manifest.sections;
        const loadedPages = [];

        for (const section of state.sections) {
            if (!Array.isArray(section.files)) continue;

            for (const path of section.files) {
                const fileResponse = await fetch(`docs/${path}`, {
                    cache: "no-cache"
                });

                if (!fileResponse.ok) {
                    console.warn(
                        `Could not load docs/${path}: ${fileResponse.status}`
                    );
                    continue;
                }

                const markdown = await fileResponse.text();

                if (
                    section.features === true ||
                    /<StartNewFeature\b/i.test(markdown)
                ) {
                    const extracted = window.SSMP_MARKDOWN.extractFeatures(
                        markdown,
                        section.title
                    );

                    if (extracted.remainder) {
                        const page = getMetadata(
                            extracted.remainder,
                            path,
                            section.title
                        );

                        page.title = section.landingTitle || section.title;
                        page.icon = section.icon || "layers";
                        page.keywords = [section.title];

                        loadedPages.push(page);
                    }

                    loadedPages.push(...extracted.pages);
                } else {
                    const page = getMetadata(
                        markdown,
                        path,
                        section.title
                    );

                    page.icon = section.icon || "book";

                    loadedPages.push(page);
                }
            }
        }

        const ids = new Set();

        state.pages = loadedPages.filter(page => {
            if (!page.id || ids.has(page.id)) return false;

            ids.add(page.id);
            return true;
        });

        const customPages = read("pages", []);

        if (Array.isArray(customPages)) {
            for (const page of customPages) {
                if (
                    page?.custom &&
                    page.id &&
                    !ids.has(page.id)
                ) {
                    state.pages.push(page);
                    ids.add(page.id);
                }
            }
        }

        if (!state.pages.length) {
            throw new Error(
                "No documentation pages were loaded. Check your manifest paths."
            );
        }
    }

    function renderNavigation() {
        window.SSMP_NAVIGATION.render(
            el.navigation,
            state.sections,
            state.pages,
            state.activePage,
            state.expanded,
            {
                navigate,
                toggleGroup
            }
        );
    }

    function toggleGroup(id) {
        state.expanded[id] = state.expanded[id] === false;

        write("navigation", state.expanded);
        renderNavigation();
    }

    
function buildTOC() {
    if (!el.toc || config.layout?.showTableOfContents === false) {
        if (el.toc) el.toc.innerHTML = "";
        return;
    }

    const headings = [
        ...el.document.querySelectorAll(
            ".document-body h2, .document-body h3"
        )
    ];

    if (!headings.length) {
        el.toc.innerHTML = "";
        return;
    }

    const usedIds = new Set();

    headings.forEach((heading, index) => {
        let id = heading.id || window.SSMP_MARKDOWN.slugify(
            heading.textContent.trim()
        );

        const baseId = id || `section-${index + 1}`;
        let uniqueId = baseId;
        let suffix = 2;

        while (usedIds.has(uniqueId)) {
            uniqueId = `${baseId}-${suffix++}`;
        }

        usedIds.add(uniqueId);
        heading.id = uniqueId;
    });

    el.toc.innerHTML = `
        <div class="toc-heading">ON THIS PAGE</div>
        <nav class="toc-links">
            ${headings.map(heading => `
                <a
                    class="toc-link toc-level-${heading.tagName.toLowerCase()}"
                    href="#${encodeURIComponent(heading.id)}"
                >${escape(heading.textContent.trim())}</a>
            `).join("")}
        </nav>
    `;

    el.toc.querySelectorAll(".toc-link").forEach(link => {
        link.addEventListener("click", event => {
            event.preventDefault();

            const id = decodeURIComponent(link.hash.slice(1));
            const target = document.getElementById(id);

            if (!target) return;

            target.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });

            history.replaceState(null, "", `#${encodeURIComponent(id)}`);
        });
    });
}

    function renderBreadcrumbs(page) {
        if (!el.breadcrumbs) return;

        if (config.layout?.showBreadcrumbs === false) {
            el.breadcrumbs.innerHTML = "";
            return;
        }

        el.breadcrumbs.innerHTML = `
            <span>Documentation</span>
            <span class="breadcrumb-separator">/</span>
            <span>${escape(page.category)}</span>
            <span class="breadcrumb-separator">/</span>
            <strong>${escape(page.title)}</strong>
        `;
    }

    function renderFooter(page) {
        if (config.layout?.showPreviousNext === false) {
            return "";
        }

        const position = state.pages.indexOf(page);
        const previous = state.pages[position - 1];
        const next = state.pages[position + 1];

        return `
            <div class="document-navigation">
                ${
                    previous
                        ? `<button class="document-nav-card previous" data-page="${escape(previous.id)}">
                            <span>← Previous</span>
                            <strong>${escape(previous.title)}</strong>
                        </button>`
                        : "<div></div>"
                }

                ${
                    next
                        ? `<button class="document-nav-card next" data-page="${escape(next.id)}">
                            <span>Next →</span>
                            <strong>${escape(next.title)}</strong>
                        </button>`
                        : "<div></div>"
                }
            </div>
        `;
    }

    function renderDocument(page) {
        if (!el.document || !page) return;

        el.document.innerHTML = `
            <article class="document-page">
                <div class="document-meta">
                    <span class="document-category">
                        ${escape(page.category)}
                    </span>

                    ${
                        config.content?.showVersionBadge !== false
                            ? `<span class="version-badge">${escape(config.brand?.version || "")}</span>`
                            : ""
                    }
                </div>

                <h1 class="document-title">${escape(page.title)}</h1>

                ${
                    page.description
                        ? `<p class="document-description">${escape(page.description)}</p>`
                        : ""
                }

                <div class="document-body">
                    ${window.SSMP_MARKDOWN.render(page.content || "")}
                </div>

                ${renderFooter(page)}
            </article>
        `;

        el.document.querySelectorAll("[data-page]").forEach(button => {
            button.addEventListener("click", () => {
                navigate(button.dataset.page);
            });
        });

        el.document.querySelectorAll(".copy-code-button").forEach(button => {
        
        });

        renderBreadcrumbs(page);
        buildTOC();

        const editButton = $("#edit-current-page");
const deleteButton = $("#delete-current-page");

if (editButton) {
    editButton.hidden = !page.custom;
}

if (deleteButton) {
    deleteButton.hidden = !page.custom;
}
    }

    function navigate(id) {
        const page = getPage(id);

        if (!page) {
            notify("Documentation page not found.", "error");
            return;
        }

        state.activePage = id;

        renderNavigation();
        renderDocument(page);

        if (config.advanced?.scrollToTopOnNavigation !== false) {
            window.scrollTo({
                top: 0,
                behavior: "smooth"
            });
        }

        closeSearch();
        closeMobileSidebar();

        history.replaceState(
            null,
            "",
            `#${encodeURIComponent(id)}`
        );
    }

    let searchScrollY = 0;

function openSearch() {
    if (config.search?.enabled === false || !el.searchOverlay) return;

    searchScrollY = window.scrollY;

    document.documentElement.classList.add("search-open");
    document.body.classList.add("search-open");

    el.searchOverlay.classList.add("open");
    el.searchOverlay.setAttribute("aria-hidden", "false");

    if (el.searchInput) {
        el.searchInput.value = "";
        el.searchInput.focus();
    }

    renderSearch("");
}

function closeSearch() {
    el.searchOverlay?.classList.remove("open");
    el.searchOverlay?.setAttribute("aria-hidden", "true");

    document.documentElement.classList.remove("search-open");
    document.body.classList.remove("search-open");

    window.scrollTo(0, searchScrollY);
}

    function renderSearch(query) {
        if (!el.searchResults) return;

        const results = window.SSMP_SEARCH.search(
            state.pages,
            query,
            config.search?.maxResults || 12
        );

        if (!query.trim()) {
            el.searchResults.innerHTML = `
                <div class="search-empty">
                </div>
            `;
            return;
        }

        if (!results.length) {
            el.searchResults.innerHTML = `
                <div class="search-empty">
                    <p>No results found</p>
                </div>
            `;
            return;
        }

        el.searchResults.innerHTML = results.map(page => `
            <button
                class="search-result"
                type="button"
                data-search-page="${escape(page.id)}"
            >
                <span class="search-result-icon">
                    ${window.SSMP_NAVIGATION.icon(page.icon, 17)}
                </span>

                <span class="search-result-info">
                    <strong>${escape(page.title)}</strong>
<small>${escape(page.category)}</small>
<span class="search-result-description">
    ${escape(page.searchSnippet || page.description || "")}
</span>
                </span>

                <span class="search-result-enter">↵</span>
            </button>
        `).join("");

        el.searchResults.querySelectorAll("[data-search-page]").forEach(button => {
            button.addEventListener("click", () => {
                navigate(button.dataset.searchPage);
            });
        });
    }

    function openEditor(page = null) {
        if (
            config.editor?.enabled === false ||
            !el.editor ||
            !el.editorForm
        ) return;

        state.editorMode = page ? "edit" : "create";
        state.originalPage = page;

        el.editorForm.reset();

        if (el.editorId) el.editorId.value = page?.id || "";
        if (el.editorTitle) el.editorTitle.value = page?.title || "";
        if (el.editorCategory) {
            el.editorCategory.value =
                page?.category ||
                config.editor?.defaultCategory ||
                "Custom Pages";
        }

        if (el.editorIcon) el.editorIcon.value = page?.icon || "book";
        if (el.editorContent) el.editorContent.value = page?.content || "";

        const heading = $("#editor-heading");

        if (heading) {
            heading.textContent = page
                ? "Edit documentation page"
                : "Create documentation page";
        }

        el.editor.classList.add("open");
        el.editor.setAttribute("aria-hidden", "false");
    }

    function closeEditor() {
        el.editor?.classList.remove("open");
        el.editor?.setAttribute("aria-hidden", "true");
    }

    function saveEditor(event) {
        event.preventDefault();

        if (
            !el.editorTitle ||
            !el.editorCategory ||
            !el.editorContent
        ) return;

        const title = el.editorTitle.value.trim();
        const category = el.editorCategory.value.trim();
        const icon = el.editorIcon?.value.trim() || "book";
        const content = el.editorContent.value;

        if (!title || !category) {
            notify("Enter a title and category first.", "error");
            return;
        }

        const id = window.SSMP_MARKDOWN.slugify(
            el.editorId?.value.trim() || title
        );

        if (!id) {
            notify("Enter a valid page ID.", "error");
            return;
        }

        if (
            state.pages.some(page =>
                page.id === id &&
                page.id !== state.originalPage?.id
            )
        ) {
            notify("That page ID is already in use.", "error");
            return;
        }

        if (state.editorMode === "edit") {
            const page = getPage(state.originalPage.id);

            if (!page?.custom) {
                notify("Markdown source pages cannot be edited here.", "error");
                return;
            }

            Object.assign(page, {
                id,
                title,
                category,
                icon,
                content
            });
        } else {
            state.pages.push({
                id,
                title,
                category,
                icon,
                content,
                description: "Custom documentation page.",
                keywords: [],
                custom: true
            });
        }

        if (!state.sections.some(section => section.title === category)) {
            state.sections.push({
                id: window.SSMP_MARKDOWN.slugify(category),
                title: category,
                icon: "book",
                files: []
            });
        }

        saveCustomPages();
        closeEditor();
        renderNavigation();
        navigate(id);

        notify("Page saved.");
    }

    function saveCustomPages() {
        write(
            "pages",
            state.pages.filter(page => page.custom)
        );
    }

    function deletePage(id) {
        const page = getPage(id);

        if (!page?.custom) {
            notify("Only custom pages can be deleted.", "error");
            return;
        }

        if (
            config.editor?.confirmDeletion !== false &&
            !confirm(
                `Delete "${page.title}"? This cannot be undone unless you exported a backup.`
            )
        ) return;

        state.pages = state.pages.filter(item => item.id !== id);

        saveCustomPages();

        const fallback = state.pages.find(item => !item.custom)
            || state.pages[0];

        if (fallback) {
            navigate(fallback.id);
        }

        notify("Page deleted.");
    }

    function exportDocumentation() {
        const data = {
            format: "ssmp-documentation",
            version: 1,
            exportedAt: new Date().toISOString(),
            pages: state.pages.filter(page => page.custom)
        };

        const blob = new Blob(
            [JSON.stringify(data, null, 2)],
            { type: "application/json" }
        );

        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");

        link.href = url;
        link.download = "ssmp-documentation-backup.json";
        link.click();

        setTimeout(() => URL.revokeObjectURL(url), 1000);

        notify("Custom pages exported.");
    }

    function importDocumentation(file) {
        if (!file) return;

        const reader = new FileReader();

        reader.onload = () => {
            try {
                const data = JSON.parse(reader.result);

                if (
                    data.format !== "ssmp-documentation" ||
                    !Array.isArray(data.pages)
                ) {
                    throw new Error("Invalid documentation backup.");
                }

                const valid = data.pages.filter(page =>
                    page &&
                    typeof page.id === "string" &&
                    typeof page.title === "string" &&
                    typeof page.content === "string"
                );

                if (!confirm(
                    `Import ${valid.length} custom pages? Existing custom pages will be replaced.`
                )) return;

                const markdownIds = new Set(
                    state.pages
                        .filter(page => !page.custom)
                        .map(page => page.id)
                );

                const imported = valid.filter(page =>
                    !markdownIds.has(page.id)
                );

                state.pages = state.pages
                    .filter(page => !page.custom)
                    .concat(imported.map(page => ({
                        ...page,
                        custom: true
                    })));

                for (const page of imported) {
                    if (!state.sections.some(section =>
                        section.title === page.category
                    )) {
                        state.sections.push({
                            id: window.SSMP_MARKDOWN.slugify(page.category),
                            title: page.category,
                            icon: "book",
                            files: []
                        });
                    }
                }

                saveCustomPages();
                renderNavigation();

                const current = getPage(state.activePage);

                if (current) {
                    renderDocument(current);
                } else {
                    navigate(state.pages[0].id);
                }

                notify("Custom pages imported.");
            } catch (error) {
                notify(
                    error.message || "Could not import the backup.",
                    "error"
                );
            }
        };

        reader.readAsText(file);
    }

    function createEditorControls() {
    }

    function closeMobileSidebar() {
        state.sidebarOpen = false;

        el.sidebar?.classList.remove("open");
        el.backdrop?.classList.remove("visible");

        document.body.classList.remove("sidebar-open");
    }

    function toggleMobileSidebar() {
        state.sidebarOpen = !state.sidebarOpen;

        el.sidebar?.classList.toggle("open", state.sidebarOpen);
        el.backdrop?.classList.toggle("visible", state.sidebarOpen);

        document.body.classList.toggle(
            "sidebar-open",
            state.sidebarOpen
        );
    }

    function bindEvents() {
        el.theme?.addEventListener("click", () => {
            if (config.theme?.allowModeSwitch === false) return;

            applyTheme(
                state.theme === "dark" ? "light" : "dark"
            );
        });

        el.searchTrigger?.addEventListener("click", openSearch);

        el.searchInput?.addEventListener("input", () => {
            renderSearch(el.searchInput.value);
        });

        el.searchInput?.addEventListener("keydown", event => {
            if (event.key === "Escape") {
                closeSearch();
            }

            if (event.key === "Enter") {
                el.searchResults
                    ?.querySelector(".search-result")
                    ?.click();
            }
        });

        el.searchOverlay?.addEventListener("click", event => {
            if (event.target === el.searchOverlay) {
                closeSearch();
            }
        });

        el.addPage?.addEventListener("click", () => {
            openEditor();
        });

        el.editorForm?.addEventListener("submit", saveEditor);

        $("#editor-cancel")?.addEventListener("click", closeEditor);
        $("#editor-close")?.addEventListener("click", closeEditor);

        el.editor?.addEventListener("click", event => {
            if (event.target === el.editor) {
                closeEditor();
            }
        });

        el.mobileMenu?.addEventListener(
            "click",
            toggleMobileSidebar
        );

        el.backdrop?.addEventListener(
            "click",
            closeMobileSidebar
        );

        $("#edit-current-page")?.addEventListener("click", () => {
            const page = getPage(state.activePage);

            if (page?.custom) {
                openEditor(page);
            }
        });

        $("#delete-current-page")?.addEventListener("click", () => {
            deletePage(state.activePage);
        });

        document.addEventListener("keydown", event => {
            const typing =
                event.target instanceof HTMLInputElement ||
                event.target instanceof HTMLTextAreaElement ||
                event.target instanceof HTMLSelectElement ||
                event.target?.isContentEditable;

            if (
                event.key.toLowerCase() ===
                    (config.search?.keyboardShortcut || "k").toLowerCase() &&
                !typing
            ) {
                event.preventDefault();
                openSearch();
            }

            if (event.key === "Escape") {
                closeSearch();
                closeEditor();
                closeMobileSidebar();
            }
        });

        window.addEventListener("hashchange", () => {
            const id = decodeURIComponent(
                location.hash.slice(1)
            );

            if (id && id !== state.activePage) {
                navigate(id);
            }
        });
    }

    async function initialize() {
        try {
            applyConfig();

            state.theme = read("theme", state.theme);
            state.expanded = read("navigation", {});

            await loadDocumentation();

            const requested = decodeURIComponent(
                location.hash.slice(1)
            );

            if (getPage(requested)) {
                state.activePage = requested;
            } else if (!getPage(state.activePage)) {
                state.activePage = state.pages[0].id;
            }

            bindEvents();
            createEditorControls();
            applyTheme(state.theme);
            renderNavigation();
            renderDocument(getPage(state.activePage));

            console.info(
                `SSMP Documentation loaded: ${state.pages.length} pages.`
            );
        } catch (error) {
            console.error(error);

            if (el.document) {
                el.document.innerHTML = `
                    <div class="empty-state">
                        <h1>Documentation could not load</h1>
                        <p>${escape(error.message)}</p>
                        <p>
                            Check that the site is being served over HTTP
                            and docs/manifest.json exists.
                        </p>
                    </div>
                `;
            }
        }
    }

    window.SSMP_DOCS = {
        navigate,
        openEditor,
        deletePage,
        exportDocumentation,
        importDocumentation,

        get pages() {
            return structuredClone(state.pages);
        },

        get activePage() {
            return state.activePage;
        }
    };

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initialize);
    } else {
        initialize();
    }
})();