package com.kabouzeid.gramophone.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.kabouzeid.gramophone.R;
import com.kabouzeid.gramophone.adapter.PlayingQueueAdapterDeprecated;
import com.kabouzeid.gramophone.helper.MusicPlayerRemote;
import com.kabouzeid.gramophone.model.Song;
import com.mobeta.android.dslv.DragSortListView;

import java.util.ArrayList;

/**
 * @author Karim Abou Zeid (kabouzeid), Aidan Follestad (afollestad)
 */
public class PlayingQueueDialog extends LeakDetectDialogFragment {

    public static PlayingQueueDialog create() {
        final ArrayList<Song> playingQueue = MusicPlayerRemote.getPlayingQueue();
        PlayingQueueDialog dialog = new PlayingQueueDialog();
        Bundle args = new Bundle();
        args.putParcelableArrayList("queue", playingQueue);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                .title(getActivity().getResources().getString(R.string.label_current_playing_queue))
                .customView(R.layout.dialog_playlist, false)
                .positiveText(R.string.save_as_playlist)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if (getActivity() == null)
                            return;
                        //noinspection unchecked
                        ArrayList<Song> playingQueue = getArguments().getParcelableArrayList("queue");
                        AddToPlaylistDialog.create(playingQueue).show(getActivity().getSupportFragmentManager(), "ADD_PLAYLIST");
                    }
                })
                .build();

        //noinspection unchecked
        final ArrayList<Song> playingQueue = getArguments().getParcelableArrayList("queue");
        final DragSortListView dragSortListView = (DragSortListView) dialog.getCustomView().findViewById(R.id.dragSortListView);
        final PlayingQueueAdapterDeprecated playingQueueAdapterDeprecated =
                new PlayingQueueAdapterDeprecated((AppCompatActivity) getActivity(), playingQueue);
        dragSortListView.setAdapter(playingQueueAdapterDeprecated);
        dragSortListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MusicPlayerRemote.playSongAt(position);
                playingQueueAdapterDeprecated.notifyDataSetChanged();
            }
        });
        dragSortListView.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                MusicPlayerRemote.moveSong(from, to);
                playingQueueAdapterDeprecated.notifyDataSetChanged();
            }
        });
        dragSortListView.post(new Runnable() {
            @Override
            public void run() {
                dragSortListView.requestFocus();
                dragSortListView.setSelection(MusicPlayerRemote.getPosition());
            }
        });
        return dialog;
    }
}
