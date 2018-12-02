package com.example.musicplayer.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.example.musicplayer.constant.BroadcastName;
import com.example.musicplayer.constant.Constant;
import com.example.musicplayer.entiy.LocalSong;
import com.example.musicplayer.entiy.Love;
import com.example.musicplayer.entiy.OnlineSong;
import com.example.musicplayer.entiy.Song;
import com.example.musicplayer.util.FileHelper;
import com.example.musicplayer.util.MediaUtil;

import org.litepal.LitePal;

import java.util.ArrayList;
import java.util.List;

@SuppressLint("NewApi")
public class PlayerService extends Service {

    private static final String TAG = "PlayerService";
    private PlayStatusBinder mPlayStatusBinder = new PlayStatusBinder();
    private MediaPlayer mediaPlayer = new MediaPlayer();       //媒体播放器对象
    private boolean isPause;                    //暂停状态
    private boolean isPlaying; //是否播放
    private List<LocalSong> mLocalSongList;
    private List<OnlineSong> mSongList;
    private List<Love> mLoveList;
    private int mCurrent;
    private int mListType;


    @Override
    public void onCreate() {
        mListType = FileHelper.getSong().getListType();
        if(mListType ==Constant.LIST_TYPE_ONLINE){
            mSongList = LitePal.findAll(OnlineSong.class);
        }else if(mListType ==Constant.LIST_TYPE_LOCAL){
            mLocalSongList = LitePal.findAll(LocalSong.class);
        }else if (mListType == Constant.LIST_TYPE_LOVE){
            mLoveList = LitePal.findAll(Love.class);
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {


        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mCurrent = FileHelper.getSong().getCurrent();
                mCurrent++;
                //将歌曲的信息保存起来
                if (mListType == Constant.LIST_TYPE_LOCAL) {
                    if (mCurrent < mLocalSongList.size()) {
                        saveLocalSongInfo(mCurrent);
                        mPlayStatusBinder.play(Constant.LIST_TYPE_LOCAL);
                        sendBroadcast(new Intent(BroadcastName.LOCAL_SONG_CHANGE_LIST));//发送广播改变当地列表的显示
                    } else {
                        mPlayStatusBinder.stop();
                        sendBroadcast(new Intent(BroadcastName.SONG_PAUSE)); //发送暂停的广播
                    }
                } else if (mListType == Constant.LIST_TYPE_ONLINE) {
                    if (mCurrent < mSongList.size()) {
                        saveOnlineSongInfo(mCurrent);
                        mPlayStatusBinder.play(Constant.LIST_TYPE_ONLINE);
                        sendBroadcast(new Intent(BroadcastName.ONLINE_ALBUM_SONG_Change));//专辑列表的改变
                    } else {
                        mPlayStatusBinder.stop();
                        sendBroadcast(new Intent(BroadcastName.SONG_PAUSE)); //发送暂停的广播
                    }

                } else {
                    if (mCurrent < mLoveList.size()) {
                        saveLoveInfo(mCurrent);
                        mPlayStatusBinder.play(Constant.LIST_TYPE_LOVE);
                        sendBroadcast(new Intent(BroadcastName.LOVE_SONG_CHANGE));//专辑列表的改变
                    } else {
                        mPlayStatusBinder.stop();
                        sendBroadcast(new Intent(BroadcastName.SONG_PAUSE)); //发送暂停的广播
                    }
                }
                sendBroadcast(new Intent(BroadcastName.ONLINE_SONG_FINISH));//发送网络歌曲播放结束的广播改变网络搜索列表的改变
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });
        return mPlayStatusBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        Log.d(TAG, "-------onRebind: ");

    }

    public class PlayStatusBinder extends Binder {


        /**
         * 播放音乐
         *
         * @param
         */

        public void play(int listType) {
            try {

                mListType = listType;
                if (mListType == Constant.LIST_TYPE_ONLINE) {
                    mSongList = LitePal.findAll(OnlineSong.class);
                } else if (mListType == Constant.LIST_TYPE_LOCAL) {
                    mLocalSongList = LitePal.findAll(LocalSong.class);
                } else if (mListType == Constant.LIST_TYPE_LOVE) {
                    mLoveList = orderList(LitePal.findAll(Love.class));
                }
                mCurrent = FileHelper.getSong().getCurrent();
                mediaPlayer.reset();//把各项参数恢复到初始状态
                if (mListType == Constant.LIST_TYPE_LOCAL) {
                    mediaPlayer.setDataSource(mLocalSongList.get(mCurrent).getUrl());
                    mediaPlayer.prepare();    //进行缓冲
                } else if (mListType == Constant.LIST_TYPE_ONLINE) {
                    mediaPlayer.setDataSource(mSongList.get(mCurrent).getUrl());
                    mediaPlayer.prepare();    //进行缓冲
                } else {
                    mediaPlayer.setDataSource(mLoveList.get(mCurrent).getUrl());
                    mediaPlayer.prepare();    //进行缓冲
                    Song song = FileHelper.getSong();
                    song.setDuration(mediaPlayer.getDuration());
                    FileHelper.saveSong(song);
                }
                isPlaying = true;
                mediaPlayer.start();
                sendBroadcast(new Intent(BroadcastName.ONLINE_ALBUM_SONG_Change));
                sendBroadcast(new Intent(BroadcastName.SONG_CHANGE));
            } catch (Exception e) {
                e.printStackTrace();

            }
        }

        //播放搜索歌曲
        public void playOnline() {
            try {
                mediaPlayer.reset();
                mediaPlayer.setDataSource(FileHelper.getSong().getUrl());
                mediaPlayer.prepare();
                Song song = FileHelper.getSong();
                song.setDuration(mediaPlayer.getDuration());
                FileHelper.saveSong(song);
                mediaPlayer.start();
                isPlaying = true;
                sendBroadcast(new Intent(BroadcastName.ONLINE_SONG));
            } catch (Exception e) {
                sendBroadcast(new Intent(BroadcastName.ONLINE_SONG_ERROR));
                e.printStackTrace();
            }
        }

        /**
         * 暂停音乐
         */

        public void pause() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                isPlaying = false;
                mediaPlayer.pause();
                isPause = true;
                sendBroadcast(new Intent(BroadcastName.SONG_PAUSE));//发送暂停的广播给主活动
            }
        }

        public void resume() {
            if (isPause) {
                mediaPlayer.start();
                isPlaying = true;
                isPause = false;
                sendBroadcast(new Intent(BroadcastName.SONG_RESUME));
            }
        }

        public void next() {
            mCurrent = FileHelper.getSong().getCurrent();
            mCurrent++;

            if (mListType == Constant.LIST_TYPE_LOCAL) {
                if (mCurrent >= mLocalSongList.size()) {
                    mCurrent = 0;
                }
                saveLocalSongInfo(mCurrent);
                mPlayStatusBinder.play(Constant.LIST_TYPE_LOCAL);
                sendBroadcast(new Intent(BroadcastName.LOCAL_SONG_CHANGE_LIST));//发送广播改变当地列表的显示
            } else if (mListType == Constant.LIST_TYPE_ONLINE) {
                if (mCurrent >= mSongList.size()) {
                    mCurrent = 0;
                }
                saveOnlineSongInfo(mCurrent);
                mPlayStatusBinder.play(Constant.LIST_TYPE_ONLINE);
                sendBroadcast(new Intent(BroadcastName.ONLINE_ALBUM_SONG_Change));//专辑列表的改变
            } else {
                if (mCurrent >= mLoveList.size()) {
                    mCurrent = 0;
                }
                Log.d(TAG, "next: " + mCurrent);
                saveLoveInfo(mCurrent);
                mPlayStatusBinder.play(Constant.LIST_TYPE_LOVE);
                sendBroadcast(new Intent(BroadcastName.LOVE_SONG_CHANGE));
            }
            sendBroadcast(new Intent(BroadcastName.ONLINE_SONG_FINISH));//发送网络歌曲播放结束的广播改变网络搜索列表的改变
        }

        public void last() {
            mCurrent = FileHelper.getSong().getCurrent();
            mCurrent--;
            if (mCurrent == -1) {
                if (mListType == Constant.LIST_TYPE_LOCAL) {
                    mCurrent = mLocalSongList.size() - 1;
                } else if (mListType == Constant.LIST_TYPE_ONLINE) {
                    mCurrent = mSongList.size() - 1;
                } else {
                    mCurrent = mLoveList.size() - 1;
                }
            }
            if (mListType == Constant.LIST_TYPE_LOCAL) {
                saveLocalSongInfo(mCurrent);
                mPlayStatusBinder.play(mListType);
                sendBroadcast(new Intent(BroadcastName.LOCAL_SONG_CHANGE_LIST));//发送广播改变当地列表的显示
            } else if (mListType == Constant.LIST_TYPE_ONLINE) {
                saveOnlineSongInfo(mCurrent);
                mPlayStatusBinder.play(mListType);
                sendBroadcast(new Intent(BroadcastName.ONLINE_ALBUM_SONG_Change));//专辑列表的改变
            } else {
                saveLoveInfo(mCurrent);
                mPlayStatusBinder.play(mListType);
                sendBroadcast(new Intent(BroadcastName.LOVE_SONG_CHANGE));
            }
            sendBroadcast(new Intent(BroadcastName.ONLINE_SONG_FINISH));//发送网络歌曲播放结束的广播改变网络搜索列表的改变
        }

        /**
         * 停止音乐
         */

        public void stop() {
            if (mediaPlayer != null) {
                isPlaying = false;
                mediaPlayer.stop();
                try {
                    mediaPlayer.prepare(); // 在调用stop后如果需要再次通过start进行播放,需要之前调用prepare函数
                } catch (Exception e) {
                    e.printStackTrace();
                }


            }

        }

        public boolean isPlaying() {

            return isPlaying;
        }

        public MediaPlayer getMediaPlayer() {

            return mediaPlayer;
        }

        public long getCurrentTime() {
            return mediaPlayer.getCurrentPosition();
        }
    }


    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        Log.d(TAG, "----onDestroy:PlayerService ");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "-----onUnbind: ");
        return true;
    }

    /**
     * 实现一个OnPrepareLister接口,当音乐准备好的时候开始播放
     */
    private final class PreparedListener implements MediaPlayer.OnPreparedListener {
        private int position;

        public PreparedListener(int position) {
            this.position = position;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            mediaPlayer.start();    //开始播放
            if (position > 0) {    //如果音乐不是从头播放
                mediaPlayer.seekTo(position);
            }
        }
    }

    //保存本地音乐列表的信息
    private void saveLocalSongInfo(int current) {
        //将歌曲的信息保存起来
        mLocalSongList = LitePal.findAll(LocalSong.class);
        Song song = new Song();
        LocalSong localSong = mLocalSongList.get(current);
        song.setCurrent(current);
        song.setSongName(localSong.getName());
        song.setSinger(localSong.getSinger());
        song.setDuration(localSong.getDuration());
        song.setUrl(localSong.getUrl());
        song.setImgUrl(localSong.getPic());
        song.setOnlineId(localSong.getSongId());
        song.setOnline(false);
        song.setListType(Constant.LIST_TYPE_LOCAL);
        FileHelper.saveSong(song);
    }
    //保存网络专辑列表的信息
    private void saveOnlineSongInfo(int current) {
        mSongList = LitePal.findAll(OnlineSong.class);
        Song song = new Song();
        song.setCurrent(current);
        song.setOnlineId(mSongList.get(current).getSongId());
        song.setSongName(mSongList.get(current).getName());
        song.setSinger(mSongList.get(current).getSinger());
        song.setDuration(mediaPlayer.getDuration());
        song.setUrl(mSongList.get(current).getUrl());
        song.setImgUrl(mSongList.get(current).getPic());
        song.setOnline(true);
        song.setListType(Constant.LIST_TYPE_ONLINE);
        FileHelper.saveSong(song);
    }

    //保存我的收藏的列表的信息
    private void saveLoveInfo(int current) {
        mLoveList = orderList(LitePal.findAll(Love.class));
        Love love = mLoveList.get(current);
        Song song = new Song();
        song.setCurrent(current);
        song.setOnlineId(love.getSongId());
        song.setSongName(love.getName());
        song.setSinger(love.getSinger());
        song.setUrl(love.getUrl());
        song.setImgUrl(love.getPic());
        song.setListType(Constant.LIST_TYPE_LOVE);
        song.setOnline(love.isOnline());
        FileHelper.saveSong(song);
    }
    //对数据库进行倒叙排序
    private List<Love> orderList(List<Love> tempList) {
        List<Love> loveList = new ArrayList<>();
        loveList.clear();
        for (int i = tempList.size() - 1; i >= 0; i--) {
            loveList.add(tempList.get(i));
        }
        return loveList;
    }
    //将歌曲保存到最近播放的列表
}


