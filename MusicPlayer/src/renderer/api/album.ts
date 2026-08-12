import { getMusicServerNeteaseAlbumNew } from './musicServer';

export const getNewAlbums = (params: { limit: number; offset: number; area: string }) => {
  return getMusicServerNeteaseAlbumNew(params);
};
