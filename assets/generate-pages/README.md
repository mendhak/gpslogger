Generates documentation using eleventy.

Docker compose volumes are used to ensure folders from parent directories are loaded into the working directory at the same level as the input folder configured in the .eleventy.js configuration.

```
docker-compose up
```

Then browse to http://localhost:8080/



## TODO

Need to read Markdown files from `../text/faq` to populate the middle section of the page. [DONE]

Need to use a chain layout so that standalone pages can use the same stylesheet and layout.

Need to generate standalone pages for `../text/opensource.md`, `../text/privacypolicy.md`, and `../text/faq/pages/gps-fix-details.md`

Need to generate standalone page for `../../LICENSE.md`

