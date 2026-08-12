import { IList } from '@/types/list';
import {
  getMusicServerNeteaseAlbum,
  getMusicServerNeteasePersonalized,
  getMusicServerNeteasePlaylistDetail,
  getMusicServerNeteaseToplist,
  getMusicServerNeteaseTopPlaylist,
  getMusicServerNeteaseTopPlaylistHighquality
} from './musicServer';

interface IListByTagParams {
  tag: string;
  before: number;
  limit: number;
}

interface IListByCatParams {
  cat: string;
  offset: number;
  limit: number;
}

// 根据tag 获取歌单列表
export function getListByTag(params: IListByTagParams) {
  return getMusicServerNeteaseTopPlaylistHighquality(params) as Promise<{ data: IList }>;
}

// 根据cat 获取歌单列表
export function getListByCat(params: IListByCatParams) {
  return getMusicServerNeteaseTopPlaylist(params);
}

// 获取推荐歌单
export function getRecommendList(limit: number = 30) {
  return getMusicServerNeteasePersonalized(limit);
}

// 获取歌单详情
export function getListDetail(id: number | string) {
  return getMusicServerNeteasePlaylistDetail(id);
}

// 获取专辑内容
export function getAlbum(id: number | string) {
  return getMusicServerNeteaseAlbum(id);
}

// 获取排行榜列表
export function getToplist() {
  return getMusicServerNeteaseToplist();
}
