package poche.fm.potunes.Model;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.media.session.MediaSession;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import com.google.android.gms.cast.MediaMetadata;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.json.JSONException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import poche.fm.potunes.R;
import poche.fm.potunes.utils.LogHelper;
import poche.fm.potunes.utils.MediaIDHelper;

/**
 * Created by purchas on 2017/2/6.
 */

public class MusicProvider {
    private static final String TAG = LogHelper.makeLogTag(MusicProvider.class);
    private MusicProviderSource mSource;

    private final HashMap<String, MediaMetadataCompat> mMusicListById;
    private final ReentrantLock initializationLock = new ReentrantLock();
    private Playlist mPlaylist;


    enum State {
        NON_INITIALIZED, INITIALIZING, INITIALIZED
    }

    private volatile State mCurrentState = State.NON_INITIALIZED;

    public interface Callback {
        void onMusicCatalogReady(boolean success);
    }

    public MusicProvider(Playlist playlist) {
        mPlaylist = playlist;
        mMusicListById = new HashMap<>();
    }

    public MediaMetadataCompat getMusic(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId) : null;
    }
    public boolean isInitialized() {
        return mCurrentState == State.INITIALIZED;
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by mediaId and grouping by genre.
     *
     * @return
     */
    public void retrieveMedia(final Callback callback) {
        if (mCurrentState == State.INITIALIZED) {
            callback.onMusicCatalogReady(true);
            return;
        }

        // Asynchronously load the music catalog in a separate thread
        new AsyncTask<Void, Void, State>() {
            @Override
            protected State doInBackground(Void... params) {
                retrieveMediaAsync(callback);
                return null;
            }
        }.execute();
    }

    private MediaMetadataCompat buildFromJSON(Track track, int count) throws JSONException {
        String title = track.getTitle();
        String album = mPlaylist.getTitle();
        String artist = track.getArtist();
        String url = track.getUrl();
        String cover = track.getCover();
        String trackNumber = track.getID() + "";

        // Adding the music source to the MediaMetadata (and consequently using it in the
        // mediaSession.setMetadata) is not a good idea for a real world music app, because
        // the session metadata can be accessed by notification listeners. This is done in this
        // sample for convenience only.
        //noinspection ResourceType
        return new MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, trackNumber)
                .putString(MusicProviderSource.CUSTOM_METADATA_TRACK_SOURCE, url)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, album)
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, artist)
                .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, cover)
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, title)
                .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, track.getID())
                .putLong(MediaMetadataCompat.METADATA_KEY_NUM_TRACKS, count)
                .build();
    }

    /**
     * Get the list of music tracks from a server and caches the track information
     * for future reference, keying tracks by musicId and grouping by album.
     */
    public void retrieveMediaAsync(final Callback callback) {
        initializationLock.lock();
        ArrayList<MediaMetadataCompat> tracks = new ArrayList<>();
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://poche.fm/api/app/playlists/" + mPlaylist.getPlaylist_id())
                    .build();
            Response response = client.newCall(request).execute();
            String responseData = response.body().string();
            Gson gson = new Gson();
            ArrayList<Track> datas = gson.fromJson(responseData, new TypeToken<List<Track>>(){}.getType());
            for (Track track : datas) {
                MediaMetadataCompat item = buildFromJSON(track, datas.size());
                mMusicListById.put(item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID), item);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Iterable<MediaMetadataCompat> getShuffledMusic() {
        if (mCurrentState != State.INITIALIZED) {
            return Collections.emptyList();
        }
        List<MediaMetadataCompat> shuffled = new ArrayList<>(mMusicListById.size());
        for (MediaMetadataCompat mutableMetadata: mMusicListById.values()) {
            shuffled.add(mutableMetadata);
        }
        Collections.shuffle(shuffled);
        return shuffled;
    }
    private synchronized void retrieveMedia() {
//        try {
//            if (mCurrentState == State.NON_INITIALIZED) {
//                mCurrentState = State.INITIALIZING;
//
//                Iterator<MediaMetadataCompat> tracks = mSource.iterator();
//                while (tracks.hasNext()) {
//                    MediaMetadataCompat item = tracks.next();
//                    String musicId = item.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID);
//                    mTrackList.add(item);
//                }
//
//                mCurrentState = State.INITIALIZED;
//            }
//        } finally {
//            if (mCurrentState != State.INITIALIZED) {
//                // Something bad happened, so we reset state to NON_INITIALIZED to allow
//                // retries (eg if the network connection is temporary unavailable)
//                mCurrentState = State.NON_INITIALIZED;
//            }
//        }
    }
//    public MediaMetadataCompat getMusic(String musicId) {
//        return mTrackList.containsKey(musicId) ? mTrackList.get(musicId).metadata : null;
//    }
    public synchronized void updateMusicArt(String musicId, Bitmap albumArt, Bitmap icon) {
//        MediaMetadataCompat metadata = getMusic(musicId);
//        metadata = new MediaMetadataCompat.Builder(metadata)
//
//                // set high resolution bitmap in METADATA_KEY_ALBUM_ART. This is used, for
//                // example, on the lockscreen background when the media session is active.
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, albumArt)
//
//                // set small version of the album art in the DISPLAY_ICON. This is used on
//                // the MediaDescription and thus it should be small to be serialized if
//                // necessary
//                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, icon)
//
//                .build();
//
//        MutableMediaMetadata mutableMetadata = mTrackList.get(musicId);
//        if (mutableMetadata == null) {
//            throw new IllegalStateException("Unexpected error: Inconsistent data structures in " +
//                    "MusicProvider");
//        }
//
//        mutableMetadata.metadata = metadata;
    }
    public List<MediaBrowserCompat.MediaItem> getChildren(String mediaId, Resources resources) {
        List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();
        if (!MediaIDHelper.isBrowseable(mediaId)) {
            return mediaItems;
        }

        if (MediaIDHelper.MEDIA_ID_ROOT.equals(mediaId)) {
//            mediaItems.add(createBrowsableMediaItemForRoot(resources));
        } else {
            Log.d(TAG, "Skipping unmatched mediaId:" + mediaId);
        }
        return mediaItems;
    }

    private MediaBrowserCompat.MediaItem createBrowsableMediaItemForRoot(Resources resources) {
        MediaDescriptionCompat description = new MediaDescriptionCompat.Builder()
                .setMediaId(MediaIDHelper.MEDIA_ID_MUSICS_BY_GENRE)
                .setTitle(resources.getString(R.string.browse_genres))
                .setSubtitle(resources.getString(R.string.browse_genre_subtitle))
                .setIconUri(Uri.parse("android.resource://" +
                        "com.example.android.uamp/drawable/ic_by_genre"))
                .build();
        return new MediaBrowserCompat.MediaItem(description,
                MediaBrowserCompat.MediaItem.FLAG_BROWSABLE);
    }
}
