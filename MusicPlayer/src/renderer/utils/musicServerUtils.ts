import type { MusicServerMusic } from '@/types/musicServer';
import type { Artist, SongResult } from '@/types/music';
import { buildMusicServerStreamUrl } from '@/api/musicServer';
import { DEFAULT_COVER_URL } from '@/utils';

const UNKNOWN_ARTIST = '未知艺术家';
const UNKNOWN_ALBUM = '私有音乐';

const createArtist = (name: string): Artist => ({
  name,
  id: 0,
  picId: 0,
  img1v1Id: 0,
  briefDesc: '',
  picUrl: '',
  img1v1Url: '',
  albumSize: 0,
  alias: [],
  trans: '',
  musicSize: 0,
  topicPerson: 0
});

export function toMusicServerSongResult(music: MusicServerMusic): SongResult {
  const artistName = music.artist || UNKNOWN_ARTIST;
  const albumName = music.album || UNKNOWN_ALBUM;
  const artist = createArtist(artistName);

  return {
    id: music.id,
    name: music.title,
    picUrl: DEFAULT_COVER_URL,
    ar: [artist],
    artists: [artist],
    al: {
      name: albumName,
      id: 0,
      type: '',
      size: 0,
      picId: 0,
      blurPicUrl: '',
      companyId: 0,
      pic: 0,
      picUrl: DEFAULT_COVER_URL,
      publishTime: 0,
      description: '',
      tags: '',
      company: '',
      briefDesc: '',
      artist,
      songs: [],
      alias: [],
      status: 0,
      copyrightId: 0,
      commentThreadId: '',
      artists: [artist],
      subType: '',
      transName: null,
      onSale: false,
      mark: 0,
      picId_str: ''
    },
    song: {
      id: music.id,
      name: music.title,
      artists: [artist],
      album: { name: albumName, picUrl: DEFAULT_COVER_URL }
    },
    playMusicUrl: buildMusicServerStreamUrl(music.id),
    source: 'musicServer',
    count: 0,
    createdAt: Date.now(),
    expiredAt: Date.now() + 30 * 24 * 60 * 60 * 1000
  };
}
