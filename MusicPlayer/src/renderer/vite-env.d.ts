/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_MUSIC_SERVER_BASE_URL?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
