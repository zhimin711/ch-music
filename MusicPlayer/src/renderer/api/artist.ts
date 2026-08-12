import {
  getMusicServerNeteaseArtist,
  getMusicServerNeteaseArtistAlbum,
  getMusicServerNeteaseArtistNewSong,
  getMusicServerNeteaseArtistSongs
} from './musicServer';

// 获取歌手详情
export const getArtistDetail = (id) => {
  return getMusicServerNeteaseArtist(id);
};

// 获取歌手热门歌曲
export const getArtistTopSongs = (params) => {
  return getMusicServerNeteaseArtistSongs({ ...params, order: 'hot' });
};

// 获取歌手专辑
export const getArtistAlbums = (params) => {
  return getMusicServerNeteaseArtistAlbum(params);
};

// 获取关注歌手新歌
export const getArtistNewSongs = (limit: number = 20) => {
  return getMusicServerNeteaseArtistNewSong(limit);
};
