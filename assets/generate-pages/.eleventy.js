const markdownIt = require("markdown-it");
const markdownItAnchor = require("markdown-it-anchor");

module.exports = (function (eleventyConfig) {
    // When generating headings, wrap it in a hyperlink, for easy accessibility. 
    let markdownLibrary = markdownIt({ html: true }).use(markdownItAnchor, { permalink: markdownItAnchor.permalink.headerLink() });
    eleventyConfig.setLibrary("md", markdownLibrary);

    // Copies the static and image files straight into the output folder, so that the HTML can reference it. 
    eleventyConfig.addPassthroughCopy({ "static/": "static" });
    eleventyConfig.addPassthroughCopy({ "content/images/": "images" });

    //Don't process README.md, that's for me!
    eleventyConfig.ignores.add("README.md");

    eleventyConfig.addCollection('tour', function (collectionApi) {
        return collectionApi.getFilteredByGlob('content/tour*.*');
    });

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