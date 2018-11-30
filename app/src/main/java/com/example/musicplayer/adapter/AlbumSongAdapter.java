package com.example.musicplayer.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.musicplayer.R;
import com.example.musicplayer.entiy.AlbumSong;
import com.example.musicplayer.util.FileHelper;

import java.util.List;

/**
 * Created by 残渊 on 2018/11/27.
 */

public class AlbumSongAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<AlbumSong.DataBean.SongsBean> mSongsBeanList;
    private int mLastPosition = -1;
    private SongClick mSongClick;
    private final int songType = 1;
    private final int footerType = 2;

    public AlbumSongAdapter(List<AlbumSong.DataBean.SongsBean> songsBeans) {
        mSongsBeanList = songsBeans;
    }

    public void setSongClick(SongClick songClick) {
        mSongClick = songClick;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == songType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycler_song_search_item, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }else{
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.footer_view_player_height, parent, false);
            FooterHolder footerHolder = new FooterHolder(view);
            return footerHolder;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int position) {
        if (viewHolder instanceof ViewHolder) {
            ViewHolder holder = (ViewHolder) viewHolder;
            AlbumSong.DataBean.SongsBean songsBean = mSongsBeanList.get(position);
            holder.artistTv.setText(songsBean.getSinger());
            holder.titleTv.setText(songsBean.getName());

            //根据点击显示
            mLastPosition = FileHelper.getSong().getCurrent();
            holder.playLine.setVisibility((songsBean.getId().equals(FileHelper.getSong().getOnlineId())
                    ? View.VISIBLE : View.INVISIBLE));
            holder.mItemView.setBackgroundResource((songsBean.getId().equals(FileHelper.getSong().getOnlineId())
                    ? R.color.click : R.color.translucent));
            holder.mItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mSongClick.onClick(position);
                    equalPosition(position);
                }
            });
        }
    }


    @Override
    public int getItemCount() {
        return mSongsBeanList.size() + 1;
    }
    @Override
    public int getItemViewType(int position) {
        return position + 1 == getItemCount() ? footerType : songType;
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView titleTv;
        TextView artistTv;
        View mItemView;
        View playLine;

        public ViewHolder(View itemView) {
            super(itemView);
            titleTv = itemView.findViewById(R.id.tv_title);
            artistTv = itemView.findViewById(R.id.tv_artist);
            playLine = itemView.findViewById(R.id.line_play);
            mItemView = itemView;
        }
    }

    class FooterHolder extends RecyclerView.ViewHolder {
        public FooterHolder(View itemView) {
            super(itemView);
        }
    }

    //判断点击的是否为上一个点击的项目
    private void equalPosition(int position) {
        if (position != mLastPosition) {
            if (mLastPosition != -1) notifyItemChanged(mLastPosition);
            mLastPosition = position;
        }
        notifyItemChanged(position);
    }

    public interface SongClick {
        void onClick(int position);
    }
}