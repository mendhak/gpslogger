Generates documentation using eleventy.

Docker compose volumes are used to ensure folders from parent directories are loaded into the working directory at the same level as the input folder configured in the .eleventy.js configuration.

```
docker-compose up
```

Then browse to http://localhost:8080/


To build, as a one-off, 

```
docker-compose run --rm --entrypoint "/bin/bash -c 'npm install;npx -p @11ty/eleventy eleventy'" eleventy
```

or with npm:

```
npm run build
```


### Caveats and TODO

Ctrl+C on the docker container doesn't really work, it times out after 10 seconds. 

Some tests to ensure the page generated properly?






