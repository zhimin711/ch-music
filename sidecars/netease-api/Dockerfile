FROM node:22-alpine

WORKDIR /app

RUN apk add --no-cache git \
    && git clone --depth=1 https://github.com/Binaryify/NeteaseCloudMusicApi.git . \
    && npm ci --omit=dev \
    && apk del git

ENV HOST=0.0.0.0
ENV PORT=3000

EXPOSE 3000

CMD ["node", "app.js"]
