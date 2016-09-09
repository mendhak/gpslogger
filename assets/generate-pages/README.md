Generates the HTML content found in [/docs](../../docs)

    npm install
    node index.js

The script reads from `filesToProcess.json` for the list of `mainpage` files and also reads the list of files from [the faq folder](../text/faq); it renders them to the main index.html and copies to [/docs](../../docs)   

The script then reads the `standalone` files, each of which become a new `.html` file in [/docs](../../docs)

Finally everything from static is copied over too.