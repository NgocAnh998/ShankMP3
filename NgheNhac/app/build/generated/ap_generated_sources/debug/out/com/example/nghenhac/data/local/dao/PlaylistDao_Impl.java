package com.example.nghenhac.data.local.dao;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.room.EntityDeleteOrUpdateAdapter;
import androidx.room.EntityInsertAdapter;
import androidx.room.RoomDatabase;
import androidx.room.util.DBUtil;
import androidx.room.util.SQLiteConnectionUtil;
import androidx.room.util.SQLiteStatementUtil;
import androidx.sqlite.SQLiteStatement;
import com.example.nghenhac.data.local.entity.PlaylistEntity;
import com.example.nghenhac.data.local.entity.PlaylistSongCrossRef;
import com.example.nghenhac.data.local.entity.SongEntity;
import java.lang.Class;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class PlaylistDao_Impl implements PlaylistDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<PlaylistEntity> __insertAdapterOfPlaylistEntity;

  private final EntityInsertAdapter<PlaylistSongCrossRef> __insertAdapterOfPlaylistSongCrossRef;

  private final EntityDeleteOrUpdateAdapter<PlaylistEntity> __deleteAdapterOfPlaylistEntity;

  private final EntityDeleteOrUpdateAdapter<PlaylistEntity> __updateAdapterOfPlaylistEntity;

  public PlaylistDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfPlaylistEntity = new EntityInsertAdapter<PlaylistEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `playlists` (`id`,`name`,`description`,`created_at`,`song_count`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final PlaylistEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getName());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getDescription());
        }
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getSongCount());
      }
    };
    this.__insertAdapterOfPlaylistSongCrossRef = new EntityInsertAdapter<PlaylistSongCrossRef>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR IGNORE INTO `playlist_songs` (`playlist_id`,`song_id`,`order_index`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement,
          final PlaylistSongCrossRef entity) {
        statement.bindLong(1, entity.getPlaylistId());
        statement.bindLong(2, entity.getSongId());
        statement.bindLong(3, entity.getOrderIndex());
      }
    };
    this.__deleteAdapterOfPlaylistEntity = new EntityDeleteOrUpdateAdapter<PlaylistEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `playlists` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final PlaylistEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfPlaylistEntity = new EntityDeleteOrUpdateAdapter<PlaylistEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `playlists` SET `id` = ?,`name` = ?,`description` = ?,`created_at` = ?,`song_count` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final PlaylistEntity entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindText(2, entity.getName());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getDescription());
        }
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getSongCount());
        statement.bindLong(6, entity.getId());
      }
    };
  }

  @Override
  public long insert(final PlaylistEntity playlist) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfPlaylistEntity.insertAndReturnId(_connection, playlist);
    });
  }

  @Override
  public void addSongToPlaylist(final PlaylistSongCrossRef crossRef) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfPlaylistSongCrossRef.insert(_connection, crossRef);
      return null;
    });
  }

  @Override
  public void addSongsToPlaylist(final List<PlaylistSongCrossRef> crossRefs) {
    DBUtil.performBlocking(__db, false, true, (_connection) -> {
      __insertAdapterOfPlaylistSongCrossRef.insert(_connection, crossRefs);
      return null;
    });
  }

  @Override
  public int delete(final PlaylistEntity playlist) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      int _result = 0;
      _result += __deleteAdapterOfPlaylistEntity.handle(_connection, playlist);
      return _result;
    });
  }

  @Override
  public int update(final PlaylistEntity playlist) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      int _result = 0;
      _result += __updateAdapterOfPlaylistEntity.handle(_connection, playlist);
      return _result;
    });
  }

  @Override
  public LiveData<List<PlaylistEntity>> getAllPlaylists() {
    final String _sql = "SELECT * FROM playlists ORDER BY name ASC";
    return __db.getInvalidationTracker().createLiveData(new String[] {"playlists"}, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final int _columnIndexOfSongCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "song_count");
        final List<PlaylistEntity> _result = new ArrayList<PlaylistEntity>();
        while (_stmt.step()) {
          final PlaylistEntity _item;
          _item = new PlaylistEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _item.setId(_tmpId);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _item.setName(_tmpName);
          final String _tmpDescription;
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null;
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription);
          }
          _item.setDescription(_tmpDescription);
          final long _tmpCreatedAt;
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
          _item.setCreatedAt(_tmpCreatedAt);
          final int _tmpSongCount;
          _tmpSongCount = (int) (_stmt.getLong(_columnIndexOfSongCount));
          _item.setSongCount(_tmpSongCount);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<PlaylistEntity> getAllPlaylistsSync() {
    final String _sql = "SELECT * FROM playlists ORDER BY name ASC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final int _columnIndexOfSongCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "song_count");
        final List<PlaylistEntity> _result = new ArrayList<PlaylistEntity>();
        while (_stmt.step()) {
          final PlaylistEntity _item;
          _item = new PlaylistEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _item.setId(_tmpId);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _item.setName(_tmpName);
          final String _tmpDescription;
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null;
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription);
          }
          _item.setDescription(_tmpDescription);
          final long _tmpCreatedAt;
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
          _item.setCreatedAt(_tmpCreatedAt);
          final int _tmpSongCount;
          _tmpSongCount = (int) (_stmt.getLong(_columnIndexOfSongCount));
          _item.setSongCount(_tmpSongCount);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public List<SongEntity> getSongsInPlaylistSync(final long playlistId) {
    final String _sql = "SELECT s.* FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.song_id WHERE ps.playlist_id = ? ORDER BY ps.order_index ASC";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfTitle = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "title");
        final int _columnIndexOfArtist = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "artist");
        final int _columnIndexOfAlbum = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "album");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfFilePath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "file_path");
        final int _columnIndexOfAlbumArtUri = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "album_art_uri");
        final int _columnIndexOfMediaStoreId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "media_store_id");
        final int _columnIndexOfIsFavorite = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "is_favorite");
        final int _columnIndexOfDateAdded = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date_added");
        final int _columnIndexOfTrackNumber = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "track_number");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mime_type");
        final int _columnIndexOfFileSize = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "file_size");
        final List<SongEntity> _result = new ArrayList<SongEntity>();
        while (_stmt.step()) {
          final SongEntity _item;
          _item = new SongEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _item.setId(_tmpId);
          final String _tmpTitle;
          if (_stmt.isNull(_columnIndexOfTitle)) {
            _tmpTitle = null;
          } else {
            _tmpTitle = _stmt.getText(_columnIndexOfTitle);
          }
          _item.setTitle(_tmpTitle);
          final String _tmpArtist;
          if (_stmt.isNull(_columnIndexOfArtist)) {
            _tmpArtist = null;
          } else {
            _tmpArtist = _stmt.getText(_columnIndexOfArtist);
          }
          _item.setArtist(_tmpArtist);
          final String _tmpAlbum;
          if (_stmt.isNull(_columnIndexOfAlbum)) {
            _tmpAlbum = null;
          } else {
            _tmpAlbum = _stmt.getText(_columnIndexOfAlbum);
          }
          _item.setAlbum(_tmpAlbum);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          _item.setDuration(_tmpDuration);
          final String _tmpFilePath;
          if (_stmt.isNull(_columnIndexOfFilePath)) {
            _tmpFilePath = null;
          } else {
            _tmpFilePath = _stmt.getText(_columnIndexOfFilePath);
          }
          _item.setFilePath(_tmpFilePath);
          final String _tmpAlbumArtUri;
          if (_stmt.isNull(_columnIndexOfAlbumArtUri)) {
            _tmpAlbumArtUri = null;
          } else {
            _tmpAlbumArtUri = _stmt.getText(_columnIndexOfAlbumArtUri);
          }
          _item.setAlbumArtUri(_tmpAlbumArtUri);
          final Long _tmpMediaStoreId;
          if (_stmt.isNull(_columnIndexOfMediaStoreId)) {
            _tmpMediaStoreId = null;
          } else {
            _tmpMediaStoreId = _stmt.getLong(_columnIndexOfMediaStoreId);
          }
          _item.setMediaStoreId(_tmpMediaStoreId);
          final boolean _tmpIsFavorite;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsFavorite));
          _tmpIsFavorite = _tmp != 0;
          _item.setFavorite(_tmpIsFavorite);
          final long _tmpDateAdded;
          _tmpDateAdded = _stmt.getLong(_columnIndexOfDateAdded);
          _item.setDateAdded(_tmpDateAdded);
          final int _tmpTrackNumber;
          _tmpTrackNumber = (int) (_stmt.getLong(_columnIndexOfTrackNumber));
          _item.setTrackNumber(_tmpTrackNumber);
          final String _tmpMimeType;
          if (_stmt.isNull(_columnIndexOfMimeType)) {
            _tmpMimeType = null;
          } else {
            _tmpMimeType = _stmt.getText(_columnIndexOfMimeType);
          }
          _item.setMimeType(_tmpMimeType);
          final long _tmpFileSize;
          _tmpFileSize = _stmt.getLong(_columnIndexOfFileSize);
          _item.setFileSize(_tmpFileSize);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public PlaylistEntity findByNameSync(final String name) {
    final String _sql = "SELECT * FROM playlists WHERE name = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        if (name == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindText(_argIndex, name);
        }
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final int _columnIndexOfSongCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "song_count");
        final PlaylistEntity _result;
        if (_stmt.step()) {
          _result = new PlaylistEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _result.setId(_tmpId);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _result.setName(_tmpName);
          final String _tmpDescription;
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null;
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription);
          }
          _result.setDescription(_tmpDescription);
          final long _tmpCreatedAt;
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
          _result.setCreatedAt(_tmpCreatedAt);
          final int _tmpSongCount;
          _tmpSongCount = (int) (_stmt.getLong(_columnIndexOfSongCount));
          _result.setSongCount(_tmpSongCount);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public LiveData<PlaylistEntity> getPlaylistById(final long id) {
    final String _sql = "SELECT * FROM playlists WHERE id = ?";
    return __db.getInvalidationTracker().createLiveData(new String[] {"playlists"}, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final int _columnIndexOfSongCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "song_count");
        final PlaylistEntity _result;
        if (_stmt.step()) {
          _result = new PlaylistEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _result.setId(_tmpId);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _result.setName(_tmpName);
          final String _tmpDescription;
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null;
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription);
          }
          _result.setDescription(_tmpDescription);
          final long _tmpCreatedAt;
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
          _result.setCreatedAt(_tmpCreatedAt);
          final int _tmpSongCount;
          _tmpSongCount = (int) (_stmt.getLong(_columnIndexOfSongCount));
          _result.setSongCount(_tmpSongCount);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public PlaylistEntity getPlaylistByIdSync(final long id) {
    final String _sql = "SELECT * FROM playlists WHERE id = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfName = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "name");
        final int _columnIndexOfDescription = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "description");
        final int _columnIndexOfCreatedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "created_at");
        final int _columnIndexOfSongCount = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "song_count");
        final PlaylistEntity _result;
        if (_stmt.step()) {
          _result = new PlaylistEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _result.setId(_tmpId);
          final String _tmpName;
          if (_stmt.isNull(_columnIndexOfName)) {
            _tmpName = null;
          } else {
            _tmpName = _stmt.getText(_columnIndexOfName);
          }
          _result.setName(_tmpName);
          final String _tmpDescription;
          if (_stmt.isNull(_columnIndexOfDescription)) {
            _tmpDescription = null;
          } else {
            _tmpDescription = _stmt.getText(_columnIndexOfDescription);
          }
          _result.setDescription(_tmpDescription);
          final long _tmpCreatedAt;
          _tmpCreatedAt = _stmt.getLong(_columnIndexOfCreatedAt);
          _result.setCreatedAt(_tmpCreatedAt);
          final int _tmpSongCount;
          _tmpSongCount = (int) (_stmt.getLong(_columnIndexOfSongCount));
          _result.setSongCount(_tmpSongCount);
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public LiveData<List<SongEntity>> getSongsInPlaylist(final long playlistId) {
    final String _sql = "SELECT s.* FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.song_id WHERE ps.playlist_id = ? ORDER BY ps.order_index ASC";
    return __db.getInvalidationTracker().createLiveData(new String[] {"songs",
        "playlist_songs"}, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfTitle = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "title");
        final int _columnIndexOfArtist = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "artist");
        final int _columnIndexOfAlbum = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "album");
        final int _columnIndexOfDuration = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "duration");
        final int _columnIndexOfFilePath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "file_path");
        final int _columnIndexOfAlbumArtUri = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "album_art_uri");
        final int _columnIndexOfMediaStoreId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "media_store_id");
        final int _columnIndexOfIsFavorite = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "is_favorite");
        final int _columnIndexOfDateAdded = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "date_added");
        final int _columnIndexOfTrackNumber = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "track_number");
        final int _columnIndexOfMimeType = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "mime_type");
        final int _columnIndexOfFileSize = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "file_size");
        final List<SongEntity> _result = new ArrayList<SongEntity>();
        while (_stmt.step()) {
          final SongEntity _item;
          _item = new SongEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _item.setId(_tmpId);
          final String _tmpTitle;
          if (_stmt.isNull(_columnIndexOfTitle)) {
            _tmpTitle = null;
          } else {
            _tmpTitle = _stmt.getText(_columnIndexOfTitle);
          }
          _item.setTitle(_tmpTitle);
          final String _tmpArtist;
          if (_stmt.isNull(_columnIndexOfArtist)) {
            _tmpArtist = null;
          } else {
            _tmpArtist = _stmt.getText(_columnIndexOfArtist);
          }
          _item.setArtist(_tmpArtist);
          final String _tmpAlbum;
          if (_stmt.isNull(_columnIndexOfAlbum)) {
            _tmpAlbum = null;
          } else {
            _tmpAlbum = _stmt.getText(_columnIndexOfAlbum);
          }
          _item.setAlbum(_tmpAlbum);
          final long _tmpDuration;
          _tmpDuration = _stmt.getLong(_columnIndexOfDuration);
          _item.setDuration(_tmpDuration);
          final String _tmpFilePath;
          if (_stmt.isNull(_columnIndexOfFilePath)) {
            _tmpFilePath = null;
          } else {
            _tmpFilePath = _stmt.getText(_columnIndexOfFilePath);
          }
          _item.setFilePath(_tmpFilePath);
          final String _tmpAlbumArtUri;
          if (_stmt.isNull(_columnIndexOfAlbumArtUri)) {
            _tmpAlbumArtUri = null;
          } else {
            _tmpAlbumArtUri = _stmt.getText(_columnIndexOfAlbumArtUri);
          }
          _item.setAlbumArtUri(_tmpAlbumArtUri);
          final Long _tmpMediaStoreId;
          if (_stmt.isNull(_columnIndexOfMediaStoreId)) {
            _tmpMediaStoreId = null;
          } else {
            _tmpMediaStoreId = _stmt.getLong(_columnIndexOfMediaStoreId);
          }
          _item.setMediaStoreId(_tmpMediaStoreId);
          final boolean _tmpIsFavorite;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsFavorite));
          _tmpIsFavorite = _tmp != 0;
          _item.setFavorite(_tmpIsFavorite);
          final long _tmpDateAdded;
          _tmpDateAdded = _stmt.getLong(_columnIndexOfDateAdded);
          _item.setDateAdded(_tmpDateAdded);
          final int _tmpTrackNumber;
          _tmpTrackNumber = (int) (_stmt.getLong(_columnIndexOfTrackNumber));
          _item.setTrackNumber(_tmpTrackNumber);
          final String _tmpMimeType;
          if (_stmt.isNull(_columnIndexOfMimeType)) {
            _tmpMimeType = null;
          } else {
            _tmpMimeType = _stmt.getText(_columnIndexOfMimeType);
          }
          _item.setMimeType(_tmpMimeType);
          final long _tmpFileSize;
          _tmpFileSize = _stmt.getLong(_columnIndexOfFileSize);
          _item.setFileSize(_tmpFileSize);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int isSongInPlaylist(final long playlistId, final long songId) {
    final String _sql = "SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, songId);
        final int _result;
        if (_stmt.step()) {
          _result = (int) (_stmt.getLong(0));
        } else {
          _result = 0;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public String getFirstSongAlbumArtUri(final long playlistId) {
    final String _sql = "SELECT s.album_art_uri FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.song_id WHERE ps.playlist_id = ? ORDER BY ps.order_index ASC LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        final String _result;
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null;
          } else {
            _result = _stmt.getText(0);
          }
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public String getFirstSongTitle(final long playlistId) {
    final String _sql = "SELECT s.title FROM songs s INNER JOIN playlist_songs ps ON s.id = ps.song_id WHERE ps.playlist_id = ? ORDER BY ps.order_index ASC LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        final String _result;
        if (_stmt.step()) {
          if (_stmt.isNull(0)) {
            _result = null;
          } else {
            _result = _stmt.getText(0);
          }
        } else {
          _result = null;
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int updateSongCount(final long playlistId) {
    final String _sql = "UPDATE playlists SET song_count = (SELECT COUNT(*) FROM playlist_songs WHERE playlist_id = ?) WHERE id = ?";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, playlistId);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int deleteById(final long id) {
    final String _sql = "DELETE FROM playlists WHERE id = ?";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int removeSongFromPlaylist(final long playlistId, final long songId) {
    final String _sql = "DELETE FROM playlist_songs WHERE playlist_id = ? AND song_id = ?";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, songId);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int clearPlaylist(final long playlistId) {
    final String _sql = "DELETE FROM playlist_songs WHERE playlist_id = ?";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, playlistId);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
