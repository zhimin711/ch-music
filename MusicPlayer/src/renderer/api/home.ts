import { IData } from '@/types';
import { IAlbumNew } from '@/types/album';
import { IDayRecommend } from '@/types/day_recommend';
import { IRecommendMusic } from '@/types/music';
import { IPlayListSort } from '@/types/playlist';
import { IHotSearch, ISearchKeyword } from '@/types/search';
import { IHotSinger } from '@/types/singer';
import request from '@/utils/request';
import {
  getMusicServerNeteaseAlbumNewest,
  getMusicServerNeteaseBanner,
  getMusicServerNeteasePersonalized,
  getMusicServerNeteasePersonalizedDjprogram,
  getMusicServerNeteasePersonalizedMv,
  getMusicServerNeteasePersonalizedNewsong,
  getMusicServerNeteasePersonalizedPrivatecontent,
  getMusicServerNeteasePlaylistCatlist,
  getMusicServerNeteaseSearchDefault,
  getMusicServerNeteaseSearchHotDetail,
  getMusicServerNeteaseTopAlbum,
  getMusicServerNeteaseTopArtists
} from './musicServer';

interface IHotSingerParams {
  offset: number;
  limit: number;
}

interface IRecommendMusicParams {
  limit: number;
}

// 获取热门歌手
export const getHotSinger = (params: IHotSingerParams) => {
  return getMusicServerNeteaseTopArtists(params) as Promise<{ data: IHotSinger }>;
};

// 获取搜索推荐词
export const getSearchKeyword = () => {
  return getMusicServerNeteaseSearchDefault() as Promise<{ data: ISearchKeyword }>;
};

// 获取热门搜索
export const getHotSearch = () => {
  return getMusicServerNeteaseSearchHotDetail() as Promise<{ data: IHotSearch }>;
};

// 获取歌单分类
export const getPlaylistCategory = () => {
  return getMusicServerNeteasePlaylistCatlist() as Promise<{ data: IPlayListSort }>;
};

// 获取推荐音乐
export const getRecommendMusic = (params: IRecommendMusicParams) => {
  return getMusicServerNeteasePersonalizedNewsong(params) as Promise<{ data: IRecommendMusic }>;
};

// 获取每日推荐
export const getDayRecommend = () => {
  return request.get<IData<IData<IDayRecommend>>>('/recommend/songs');
};

// 获取最新专辑推荐
export const getNewAlbum = () => {
  return getMusicServerNeteaseAlbumNewest() as Promise<{ data: IAlbumNew }>;
};

// 获取轮播图
export const getBanners = (type: number = 0) => {
  return getMusicServerNeteaseBanner(type);
};

// 获取推荐歌单
export const getPersonalizedPlaylist = (limit: number = 30) => {
  return getMusicServerNeteasePersonalized(limit);
};

// 获取私人漫游（request 拦截器已自动添加 timestamp）
export const getPersonalFM = () => {
  return request.get<any>('/personal_fm');
};

// 获取独家放送
export const getPrivateContent = () => {
  return getMusicServerNeteasePersonalizedPrivatecontent();
};

// 获取推荐MV
export const getPersonalizedMV = () => {
  return getMusicServerNeteasePersonalizedMv();
};

// 获取新碟上架
export const getTopAlbum = (params?: { limit?: number; offset?: number; area?: string }) => {
  return getMusicServerNeteaseTopAlbum(params);
};

// 获取推荐电台
export const getPersonalizedDJ = () => {
  return getMusicServerNeteasePersonalizedDjprogram();
};
