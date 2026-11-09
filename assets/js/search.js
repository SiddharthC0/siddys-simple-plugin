
(() => {
    "use strict";

    const config = window.SSMP_CONFIG?.search || {};

    function normalize(value) {
        return String(value ?? "")
            .normalize("NFD")
            .replace(/[\u0300-\u036f]/g, "")
            .toLowerCase()
            .trim();
    }

    function stripMarkdown(value) {
        return String(value ?? "")
            .replace(/```[\w-]*\n?/g, " ")
            .replace(/`([^`]+)`/g, "$1")
            .replace(/!\[([^\]]*)\]\([^)]+\)/g, "$1")
            .replace(/\[([^\]]+)\]\([^)]+\)/g, "$1")
            .replace(/<[^>]*>/g, " ")
            .replace(/^\s{0,3}#{1,6}\s+/gm, "")
            .replace(/^\s*[-*_]{3,}\s*$/gm, " ")
            .replace(/^\s*>\s?/gm, "")
            .replace(/^\s*[-*+]\s+/gm, "")
            .replace(/^\s*\d+\.\s+/gm, "")
            .replace(/[*_~]/g, "")
            .replace(/\|/g, " ")
            .replace(/\s+/g, " ")
            .trim();
    }

    function getSearchableText(page) {
        const title = page.title || "";
        const category = page.category || "";
        const description = page.description || "";
        const keywords = Array.isArray(page.keywords)
            ? page.keywords.join(" ")
            : "";

        const content = config.searchContent !== false
            ? stripMarkdown(page.content || "")
            : "";

        const descriptionText = config.searchDescriptions !== false
            ? description
            : "";

        return {
            title: normalize(title),
            category: normalize(category),
            description: normalize(descriptionText),
            keywords: normalize(keywords),
            content: normalize(content),
            combined: normalize([
                title,
                category,
                descriptionText,
                keywords,
                content
            ].join(" "))
        };
    }

    function scoreField(field, term, weight) {
        if (!field || !field.includes(term)) return 0;

        if (field === term) return weight * 5;
        if (field.startsWith(term)) return weight * 3;
        if (field.includes(` ${term}`)) return weight * 2;

        return weight;
    }

    function scorePage(page, fields, terms, phrase) {
        let score = 0;

        for (const term of terms) {
            if (!fields.combined.includes(term)) {
                return 0;
            }

            const scores = [
                scoreField(fields.title, term, 12),
                scoreField(fields.category, term, 5),
                scoreField(fields.description, term, 4),
                scoreField(fields.keywords, term, 8),
                scoreField(fields.content, term, 2)
            ];

            score += Math.max(...scores);
        }

        if (phrase) {
            const phraseFields = [
                fields.title,
                fields.category,
                fields.description,
                fields.keywords,
                fields.content
            ];

            if (phraseFields.some(field => field.includes(phrase))) {
                score += 15;
            }
        }

        if (normalize(page.title) === phrase) {
            score += 30;
        }

        return score;
    }

    function createSnippet(page, terms) {
        const content = stripMarkdown(page.content || "");
        if (!content) return page.description || "";

        const normalized = normalize(content);

        let position = -1;

        for (const term of terms) {
            position = normalized.indexOf(term);
            if (position !== -1) break;
        }

        if (position === -1) {
            return content.slice(0, 140);
        }

        const start = Math.max(0, position - 55);
        const end = Math.min(content.length, position + 100);

        return (
            (start > 0 ? "..." : "") +
            content.slice(start, end) +
            (end < content.length ? "..." : "")
        );
    }

    function search(pages, query, maxResults = 12) {
        if (!Array.isArray(pages) || !query?.trim()) {
            return [];
        }

        const normalizedQuery = normalize(query);
        const terms = normalizedQuery
            .split(/\s+/)
            .filter(Boolean);

        if (!terms.length) return [];

        const phrase = normalizedQuery;

        return pages
            .map(page => {
                const fields = getSearchableText(page);
                const score = scorePage(
                    page,
                    fields,
                    terms,
                    phrase
                );

                if (!score) return null;

                return {
                    ...page,
                    searchScore: score,
                    searchSnippet: createSnippet(page, terms)
                };
            })
            .filter(Boolean)
            .sort((a, b) => {
                return (
                    b.searchScore - a.searchScore ||
                    a.title.localeCompare(b.title)
                );
            })
            .slice(0, Math.max(1, Number(maxResults) || 12));
    }

    window.SSMP_SEARCH = {
        search,
        normalize,
        stripMarkdown
    };
})();
