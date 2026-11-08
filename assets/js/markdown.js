
(() => {
    "use strict";

    const boundRoots = new WeakSet();

    const escape = value => String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[character]);

    const slugify = value => String(value ?? "")
        .normalize("NFKD")
        .toLowerCase()
        .trim()
        .replace(/[\u0300-\u036f]/g, "")
        .replace(/[^\w\s-]/g, "")
        .replace(/\s+/g, "-")
        .replace(/-+/g, "-")
        .replace(/^-|-$/g, "") || "section";

    const safeURL = value => {
        try {
            const url = new URL(String(value).trim(), location.href);

            if (!["http:", "https:", "mailto:"].includes(url.protocol)) {
                return "";
            }

            return url.href;
        } catch {
            return "";
        }
    };

    const externalURL = url => {
        try {
            return new URL(url).origin !== location.origin;
        } catch {
            return false;
        }
    };

    function parseLink(label, destination, allowSpecial = true) {
        const target = destination.trim();
        
if (allowSpecial && /^Long Button:\s*/i.test(label)) {
    const text = label.replace(/^Long Button:\s*/i, "");
    const url = safeURL(target);

    if (!url && !target.startsWith("#")) {
        return escape(text);
    }

    const href = target.startsWith("#")
        ? "#" + slugify(decodeURIComponent(target.slice(1)))
        : url;

    return `<a class="doc-button doc-button-long" href="${escape(href)}"${externalURL(href) ? ' target="_blank" rel="noopener noreferrer"' : ""}>${inline(text, false)}</a>`;
}

        if (allowSpecial && /^Button:\s*/i.test(label)) {
            const text = label.replace(/^Button:\s*/i, "");
            const url = safeURL(target);

            if (!url && !target.startsWith("#")) {
                return escape(text);
            }

            const href = target.startsWith("#")
                ? "#" + slugify(decodeURIComponent(target.slice(1)))
                : url;

            return `<a class="doc-button" href="${escape(href)}"${externalURL(href) ? ' target="_blank" rel="noopener noreferrer"' : ""}>${inline(text, false)}</a>`;
        }

        if (allowSpecial && /^Teleport:\s*/i.test(label)) {
            const text = label.replace(/^Teleport:\s*/i, "");
            const id = target.startsWith("#")
                ? slugify(decodeURIComponent(target.slice(1)))
                : slugify(target);

            return `<a class="doc-teleport" href="#${escape(id)}" data-teleport="${escape(id)}">${inline(text, false)}</a>`;
        }

        const url = target.startsWith("#")
            ? "#" + slugify(decodeURIComponent(target.slice(1)))
            : safeURL(target);

        if (!url) return escape(label);

        const external = externalURL(url);

        return `<a href="${escape(url)}"${external ? ' target="_blank" rel="noopener noreferrer"' : ""}>${inline(label, false)}</a>`;
    }

    function inline(value, allowLinks = true) {
        const source = String(value ?? "");

        const pattern = /(!?\[[^\]]+\]\([^)]+\)|`[^`]+`|\*\*.+?\*\*|__.+?__|~~.+?~~|(?<!\*)\*[^*\n]+\*|(?<!_)_[^_\n]+_)/g;

        let result = "";
        let lastIndex = 0;
        let match;

        while ((match = pattern.exec(source)) !== null) {
            result += escape(source.slice(lastIndex, match.index));

            const token = match[0];

            if (token.startsWith("`")) {
                result += `<code>${escape(token.slice(1, -1))}</code>`;
            } else if (token.startsWith("![")) {
                const image = token.match(/^!\[([^\]]*)\]\(([^)]+)\)$/);
                const url = image ? safeURL(image[2]) : "";

                result += url
                    ? `<img class="doc-image" src="${escape(url)}" alt="${escape(image[1])}" loading="lazy">`
                    : "";
            } else if (token.startsWith("[")) {
                const link = token.match(/^\[([^\]]+)\]\(([^)]+)\)$/);

                result += link && allowLinks
                    ? parseLink(link[1], link[2])
                    : escape(token);
            } else if (token.startsWith("**") || token.startsWith("__")) {
                result += `<strong>${inline(token.slice(2, -2), allowLinks)}</strong>`;
            } else if (token.startsWith("~~")) {
                result += `<del>${inline(token.slice(2, -2), allowLinks)}</del>`;
            } else {
                result += `<em>${inline(token.slice(1, -1), allowLinks)}</em>`;
            }

            lastIndex = pattern.lastIndex;
        }

        result += escape(source.slice(lastIndex));

        return result;
    }

    function createHeadingId(title, headings) {
        const base = slugify(title);
        const count = headings.get(base) || 0;

        headings.set(base, count + 1);

        return count === 0 ? base : `${base}-${count + 1}`;
    }

    function parseTableRow(line) {
        return line.trim()
            .replace(/^\|/, "")
            .replace(/\|$/, "")
            .split(/(?<!\\)\|/)
            .map(cell => cell.replace(/\\\|/g, "|").trim());
    }

    function isTableSeparator(line) {
        return /^\|?[\s:|-]+\|?$/.test(line.trim()) &&
            line.includes("-") &&
            line.includes("|");
    }

    function parseList(lines, start) {
        const root = [];
        const stack = [];

        let index = start;

        while (index < lines.length) {
            const match = lines[index].match(/^(\s*)([-*+]|\d+\.)\s+(.+)$/);

            if (!match) break;

            const indent = match[1].replace(/\t/g, "    ").length;
            const type = /^\d/.test(match[2]) ? "ol" : "ul";
            const content = match[3];

            while (stack.length && indent < stack[stack.length - 1].indent) {
                stack.pop();
            }

            if (!stack.length || indent > stack[stack.length - 1].indent) {
                const parent = stack.length
                    ? stack[stack.length - 1].lastItem
                    : null;

                const list = { type, items: [], indent };

                if (parent) {
                    parent.children.push(list);
                } else {
                    root.push(list);
                }

                stack.push({
                    indent,
                    list,
                    lastItem: null
                });
            } else if (stack[stack.length - 1].list.type !== type) {
                const parent = stack[stack.length - 1].lastItem;
                const list = { type, items: [], indent };

                if (parent) {
                    parent.children.push(list);
                } else {
                    root.push(list);
                }

                stack[stack.length - 1].list = list;
                stack[stack.length - 1].lastItem = null;
            }

            const current = stack[stack.length - 1];
            const item = {
                content,
                children: []
            };

            current.list.items.push(item);
            current.lastItem = item;

            index++;
        }

        const serialize = lists => lists.map(list =>
            `<${list.type}>${list.items.map(item =>
                `<li>${inline(item.content)}${serialize(item.children)}</li>`
            ).join("")}</${list.type}>`
        ).join("");

        return {
            html: serialize(root),
            next: index
        };
    }

    function findDirectiveEnd(lines, start) {
        let depth = 1;
        let fenced = false;

        for (let i = start + 1; i < lines.length; i++) {
            const line = lines[i].trim();

            if (line.startsWith("```") || line.startsWith("~~~")) {
                fenced = !fenced;
                continue;
            }

            if (fenced) continue;

            if (/^:::(spoiler|dropdown)\b/i.test(line)) {
                depth++;
            } else if (line === ":::") {
                depth--;

                if (depth === 0) return i;
            }
        }

        return lines.length;
    }

    function parseBlocks(markdown, headings) {
        const lines = String(markdown ?? "")
            .replace(/\r/g, "")
            .split("\n");

        const output = [];

        let paragraph = [];
        let index = 0;

        const flushParagraph = () => {
            if (!paragraph.length) return;

            output.push(`<p>${paragraph.map(line => inline(line)).join("<br>")}</p>`);
            paragraph = [];
        };

        const parseTable = () => {
            const rows = [];

            while (index < lines.length && lines[index].trim().startsWith("|")) {
                rows.push(lines[index].trim());
                index++;
            }

            if (!rows.length) return;

            const filtered = rows.filter((row, i) =>
                !(i === 1 && isTableSeparator(row))
            );

            if (!filtered.length) return;

            const headers = parseTableRow(filtered[0]);
            const body = filtered.slice(1);

            output.push(`
                <div class="table-wrapper">
                    <table class="doc-table">
                        <thead>
                            <tr>${headers.map(cell =>
                                `<th>${inline(cell)}</th>`
                            ).join("")}</tr>
                        </thead>
                        <tbody>
                            ${body.map(row =>
                                `<tr>${parseTableRow(row).map(cell =>
                                    `<td>${inline(cell)}</td>`
                                ).join("")}</tr>`
                            ).join("")}
                        </tbody>
                    </table>
                </div>
            `);
        };

        while (index < lines.length) {
            const line = lines[index];
            const trimmed = line.trim();

            if (!trimmed) {
                flushParagraph();
                index++;
                continue;
            }

            const fence = trimmed.match(/^(```|~~~)(.*)$/);

            if (fence) {
                flushParagraph();

                const delimiter = fence[1];
                const language = fence[2].trim();
                const codeLines = [];

                index++;

                while (index < lines.length &&
                    !lines[index].trim().startsWith(delimiter)) {
                    codeLines.push(lines[index]);
                    index++;
                }

                if (index < lines.length) index++;

                output.push(`
                    <div class="code-block">
                        <pre><code${language ? ` class="language-${escape(language)}"` : ""}>${escape(codeLines.join("\n"))}</code>
                        </pre>
                    </div>
                `);

                continue;
            }

            const directive = trimmed.match(/^:::(spoiler|dropdown)\s*(.*)$/i);

            if (directive) {
                flushParagraph();

                const end = findDirectiveEnd(lines, index);
                const content = lines.slice(index + 1, end).join("\n");
                const title = directive[2].trim() ||
                    (directive[1].toLowerCase() === "spoiler" ? "Show more" : "Options");

                const body = parseBlocks(content, headings);
                const type = directive[1].toLowerCase();

                output.push(`
                    <details class="doc-collapse doc-${type}">
                        <summary>${inline(title)}</summary>
                        <div class="doc-collapse-content">
                            ${body}
                        </div>
                    </details>
                `);

                index = end < lines.length ? end + 1 : end;
                continue;
            }

            const heading = trimmed.match(/^(#{1,6})\s+(.+)$/);

            if (heading) {
                flushParagraph();

                const level = heading[1].length;
                const title = heading[2].trim();
                const id = createHeadingId(title, headings);

                output.push(`
                    <h${level} id="${escape(id)}" class="doc-heading">
                        ${inline(title)}
                        <a class="heading-anchor" href="#${escape(id)}" aria-label="Link to ${escape(title)}"></a>
                    </h${level}>
                `);

                index++;
                continue;
            }

            if (trimmed.startsWith("|") &&
                index + 1 < lines.length &&
                isTableSeparator(lines[index + 1])) {
                flushParagraph();
                parseTable();
                continue;
            }

            const listMatch = line.match(/^(\s*)([-*+]|\d+\.)\s+(.+)$/);

            if (listMatch) {
                flushParagraph();

                const list = parseList(lines, index);
                output.push(list.html);
                index = list.next;
                continue;
            }

            if (trimmed.startsWith(">")) {
                flushParagraph();

                const quoteLines = [];

                while (index < lines.length &&
                    lines[index].trim().startsWith(">")) {
                    quoteLines.push(
                        lines[index].trim().replace(/^>\s?/, "")
                    );
                    index++;
                }

                const callout = quoteLines[0].match(
                    /^\[!(NOTE|INFO|WARNING|DANGER|SUCCESS)\]\s*(.*)$/i
                );

                if (callout) {
                    const type = callout[1].toLowerCase();
                    const title = callout[2] ||
                        type.charAt(0).toUpperCase() + type.slice(1);

                    output.push(`
                        <aside class="callout callout-${type}">
                            <div class="callout-content">
                                <strong class="callout-title">${inline(title)}</strong>
                                <div>${parseBlocks(quoteLines.slice(1).join("\n"), headings)}</div>
                            </div>
                        </aside>
                    `);
                } else {
                    output.push(`
                        <blockquote>
                            ${parseBlocks(quoteLines.join("\n"), headings)}
                        </blockquote>
                    `);
                }

                continue;
            }

            if (trimmed === "---" || trimmed === "***" || trimmed === "___") {
                flushParagraph();
                output.push("<hr class=\"doc-divider\">");
                index++;
                continue;
            }

            if (/^<StartNewFeature\b/i.test(trimmed) ||
                /^<EndNewFeature/i.test(trimmed)) {
                flushParagraph();
                output.push(escape(trimmed));
                index++;
                continue;
            }

            paragraph.push(trimmed);
            index++;
        }

        flushParagraph();

        return output.join("\n");
    }

    function render(markdown = "") {
        const headings = new Map();
        return parseBlocks(markdown, headings);
    }

    function extractFeatures(markdown, category = "Features") {
        const pages = [];
        const pattern = /<StartNewFeature\b([^>]*)>([\s\S]*?)<EndNewFeature\s*>/gi;

        let match;

        while ((match = pattern.exec(String(markdown))) !== null) {
            const attributes = {};
            const attributePattern = /([\w-]+)\s*=\s*["']([^"']*)["']/g;

            let attribute;

            while ((attribute = attributePattern.exec(match[1])) !== null) {
                attributes[attribute[1]] = attribute[2];
            }

            const content = match[2].trim();
            const heading = content.match(/^#\s+(.+)$/m);

            const title = attributes.title || heading?.[1] || "Untitled Feature";
            const id = slugify(attributes.id || title);

            pages.push({
                id: `feature-${id}`,
                title,
                category,
                icon: attributes.icon || "puzzle",
                description: attributes.description || "",
                content,
                keywords: [title, category, id],
                custom: false,
                source: "markdown"
            });
        }

        const remainder = String(markdown).replace(pattern, "").trim();

        return { pages, remainder };
    }

    function bindInteractions(root = document) {
        if (!root || boundRoots.has(root)) return;

        boundRoots.add(root);

        root.addEventListener("click", async event => {

            const teleport = event.target.closest(".doc-teleport");

            if (teleport && root.contains(teleport)) {
                const id = teleport.dataset.teleport;
                const target = document.getElementById(id);

                if (!target) return;

                event.preventDefault();

                target.scrollIntoView({
                    behavior: "smooth",
                    block: "start"
                });

                history.replaceState(null, "", `#${encodeURIComponent(id)}`);
            }
        });
    }

    window.SSMP_MARKDOWN = {
        render,
        extractFeatures,
        slugify,
        escape,
        safeURL,
        bindInteractions
    };
})();
