import { getMusicServerNeteaseSearch, getMusicServerNeteaseSearchSuggest } from './musicServer';

interface IParams {
  keywords: string;
  type: number;
  limit?: number;
  offset?: number;
}
// 搜索内容
export const getSearch = (params: IParams) => {
  return getMusicServerNeteaseSearch(params);
};

/**
 * 搜索建议接口返回的数据结构
 */
// 搜索建议返回的数据结构（部分字段）
interface NeteaseSuggestResult {
  result?: {
    songs?: Array<{ name: string }>;
    artists?: Array<{ name: string }>;
    albums?: Array<{ name: string }>;
  };
  code?: number;
}

/**
 * 获取搜索建议
 * @param keyword 搜索关键词
 */
export const getSearchSuggestions = async (keyword: string) => {
  console.log('[API] getSearchSuggestions: 开始执行');

  if (!keyword || !keyword.trim()) {
    return Promise.resolve([]);
  }

  console.log(`[API] getSearchSuggestions: 准备请求，关键词: "${keyword}"`);

  try {
    const res = await getMusicServerNeteaseSearchSuggest({ keywords: keyword });
    const data = res?.data as NeteaseSuggestResult;
    const result = data?.result || {};
    const names: string[] = [];
    if (Array.isArray(result.songs)) names.push(...result.songs.map((song) => song.name));
    if (Array.isArray(result.artists)) names.push(...result.artists.map((artist) => artist.name));
    if (Array.isArray(result.albums)) names.push(...result.albums.map((album) => album.name));

    const unique = Array.from(new Set(names)).slice(0, 10);
    console.log('[API] getSearchSuggestions: 解析成功:', unique);
    return unique;
  } catch (error) {
    console.error('[API] getSearchSuggestions: 请求失败，错误信息:', error);
    return [];
  }
};
