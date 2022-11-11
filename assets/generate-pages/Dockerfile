FROM node:16-slim

WORKDIR /home/node/app

COPY package.json package-lock.json /home/node/app/

RUN npm install

CMD ["npx", "eleventy", "--watch", "--serve"]