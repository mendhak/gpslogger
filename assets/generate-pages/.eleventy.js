const markdownIt = require("markdown-it");
const markdownItAnchor = require("markdown-it-anchor");
const slugify = require('slugify');

module.exports = (function (eleventyConfig) {
    // When generating headings, wrap it in a hyperlink, for easy accessibility. 
    // Only wrap it in hyperlink if it's h1, h2, h3.
    // Replace spaces and special chars with empty string
    // This is for backwards compatibility to match previous links.
    let markdownLibrary = markdownIt({ html: true }).use(markdownItAnchor, 
        { 
            permalink: markdownItAnchor.permalink.headerLink(), 
            level: [1, 2, 3], 
            slugify: slug => slugify(slug, { replacement: '', lower:true, }), 
            tabIndex: false,
        });
    eleventyConfig.setLibrary("md", markdownLibrary);

    // Copies the static and image files straight into the output folder, so that the HTML can reference it. 
    eleventyConfig.addPassthroughCopy({ "static/": "." });
    eleventyConfig.addPassthroughCopy({ "text/content/images/": "images" });
    eleventyConfig.addPassthroughCopy({ "text/faq/images/": "images" });

    // Don't process README.md, that's for me!
    eleventyConfig.ignores.add("README.md");

    //I'm just going to hardcode the titles here, instead of creating a "privacypolicy.11tydata.json" with the title in it. 
    eleventyConfig.addFilter("titleFormat", function (pagename) {
        switch (pagename.toLowerCase()) {
            case "privacypolicy":
                pagename = "Privacy Policy";
                break;
            case "opensource":
                pagename = "Open Source Licenses";
                break;
            case "license":
                pagename = "License";
                break;
            case "gps-fix-details":
                pagename = "Why does GPS take a long time to find a fix?";
            default:
                break;
        }
        if (pagename) {
            return `${pagename} — GPSLogger for Android`;
        }

        return "GPSLogger for Android";

    });

    // Show the current year using a shortcode
    eleventyConfig.addShortcode("year", () => `${new Date().getFullYear()}`);

    // Collect all the tour.*.md files to go into the top Quick Tour section.
    eleventyConfig.addCollection('tour', function (collectionApi) {
        return collectionApi.getFilteredByGlob('text/content/tour*.*');
    });

    // Collect all the FAQ md files to go into the FAQ section
    eleventyConfig.addCollection('faq', function (collectionApi) {
        return collectionApi.getFilteredByGlob('text/faq/*.*');
    });

    // Collect all the more.*.md files to go into the More Screenshots section
    eleventyConfig.addCollection('more', function (collectionApi) {
        return collectionApi.getFilteredByGlob('text/content/more*.*');
    });


    // Config values that could be passed via arguments but it's just easier in here. 
    return {
        dir: {
            input: ".",
            output: "_site",
            templateFormats: ["html"]
        }
    }

});