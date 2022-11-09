const markdownIt = require("markdown-it");
const markdownItAnchor = require("markdown-it-anchor");

module.exports = (function (eleventyConfig) {
    // When generating headings, wrap it in a hyperlink, for easy accessibility. 
    let markdownLibrary = markdownIt({ html: true }).use(markdownItAnchor, { permalink: markdownItAnchor.permalink.headerLink() });
    eleventyConfig.setLibrary("md", markdownLibrary);

    // Copies the static and image files straight into the output folder, so that the HTML can reference it. 
    eleventyConfig.addPassthroughCopy({ "static/": "static" });
    eleventyConfig.addPassthroughCopy({ "content/images/": "images" });

    // Don't process README.md, that's for me!
    eleventyConfig.ignores.add("README.md");

    // Show the current year using a shortcode
    eleventyConfig.addShortcode("year", () => `${new Date().getFullYear()}`);

    // Collect all the tour.*.md files to go into the top Quick Tour section.
    eleventyConfig.addCollection('tour', function (collectionApi) {
        return collectionApi.getFilteredByGlob('content/tour*.*');
    });

    // Collect all the more.*.md files to go into the More Screenshots section
    eleventyConfig.addCollection('more', function (collectionApi) {
        return collectionApi.getFilteredByGlob('content/more*.*');
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