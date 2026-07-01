package com.example.nghenhac.data.local;

import androidx.annotation.NonNull;
import androidx.room.InvalidationTracker;
import androidx.room.RoomOpenDelegate;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.SQLite;
import androidx.sqlite.SQLiteConnection;
import com.example.nghenhac.data.local.dao.CachedSongDao;
import com.example.nghenhac.data.local.dao.CachedSongDao_Impl;
import com.example.nghenhac.data.local.dao.PlaylistDao;
import com.example.nghenhac.data.local.dao.PlaylistDao_Impl;
import com.example.nghenhac.data.local.dao.SongDao;
import com.example.nghenhac.data.local.dao.SongDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation", "removal"})
public final class AppDatabase_Impl extends AppDatabase {
  private volatile SongDao _songDao;

  private volatile PlaylistDao _playlistDao;

  private volatile CachedSongDao _cachedSongDao;

  @Override
  @NonNull
  protected RoomOpenDelegate createOpenDelegate() {
    final RoomOpenDelegate _openDelegate = new RoomOpenDelegate(1, "86ab0e441a587fdb2ea067193799de25", "6a0209373375e18eb40d5f8d2af60801") {
      @Override
      public void createAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `songs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `artist` TEXT NOT NULL, `album` TEXT NOT NULL, `duration` INTEGER NOT NULL, `file_path` TEXT, `album_art_uri` TEXT, `media_store_id` INTEGER, `is_favorite` INTEGER NOT NULL DEFAULT 0, `date_added` INTEGER NOT NULL, `track_number` INTEGER NOT NULL, `mime_type` TEXT, `file_size` INTEGER NOT NULL)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_songs_media_store_id` ON `songs` (`media_store_id`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_songs_title` ON `songs` (`title`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_songs_artist` ON `songs` (`artist`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_songs_album` ON `songs` (`album`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_songs_is_favorite` ON `songs` (`is_favorite`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `playlists` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT, `description` TEXT, `created_at` INTEGER NOT NULL, `song_count` INTEGER NOT NULL DEFAULT 0)");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_playlists_name` ON `playlists` (`name`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `playlist_songs` (`playlist_id` INTEGER NOT NULL, `song_id` INTEGER NOT NULL, `order_index` INTEGER NOT NULL DEFAULT 0, PRIMARY KEY(`playlist_id`, `song_id`), FOREIGN KEY(`playlist_id`) REFERENCES `playlists`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE , FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlist_id` ON `playlist_songs` (`playlist_id`)");
        SQLite.execSQL(connection, "CREATE INDEX IF NOT EXISTS `index_playlist_songs_song_id` ON `playlist_songs` (`song_id`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS `cached_songs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `song_id` INTEGER NOT NULL, `local_file_path` TEXT, `file_size` INTEGER NOT NULL, `cached_at` INTEGER NOT NULL, `is_full_download` INTEGER NOT NULL DEFAULT 0, FOREIGN KEY(`song_id`) REFERENCES `songs`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        SQLite.execSQL(connection, "CREATE UNIQUE INDEX IF NOT EXISTS `index_cached_songs_song_id` ON `cached_songs` (`song_id`)");
        SQLite.execSQL(connection, "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        SQLite.execSQL(connection, "INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '86ab0e441a587fdb2ea067193799de25')");
      }

      @Override
      public void dropAllTables(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `songs`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `playlists`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `playlist_songs`");
        SQLite.execSQL(connection, "DROP TABLE IF EXISTS `cached_songs`");
      }

      @Override
      public void onCreate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      public void onOpen(@NonNull final SQLiteConnection connection) {
        SQLite.execSQL(connection, "PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(connection);
      }

      @Override
      public void onPreMigrate(@NonNull final SQLiteConnection connection) {
        DBUtil.dropFtsSyncTriggers(connection);
      }

      @Override
      public void onPostMigrate(@NonNull final SQLiteConnection connection) {
      }

      @Override
      @NonNull
      public RoomOpenDelegate.ValidationResult onValidateSchema(
          @NonNull final SQLiteConnection connection) {
        final Map<String, TableInfo.Column> _columnsSongs = new HashMap<String, TableInfo.Column>(13);
        _columnsSongs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("artist", new TableInfo.Column("artist", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("album", new TableInfo.Column("album", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("duration", new TableInfo.Column("duration", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("file_path", new TableInfo.Column("file_path", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("album_art_uri", new TableInfo.Column("album_art_uri", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("media_store_id", new TableInfo.Column("media_store_id", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("is_favorite", new TableInfo.Column("is_favorite", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("date_added", new TableInfo.Column("date_added", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("track_number", new TableInfo.Column("track_number", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("mime_type", new TableInfo.Column("mime_type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSongs.put("file_size", new TableInfo.Column("file_size", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysSongs = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesSongs = new HashSet<TableInfo.Index>(5);
        _indicesSongs.add(new TableInfo.Index("index_songs_media_store_id", true, Arrays.asList("media_store_id"), Arrays.asList("ASC")));
        _indicesSongs.add(new TableInfo.Index("index_songs_title", false, Arrays.asList("title"), Arrays.asList("ASC")));
        _indicesSongs.add(new TableInfo.Index("index_songs_artist", false, Arrays.asList("artist"), Arrays.asList("ASC")));
        _indicesSongs.add(new TableInfo.Index("index_songs_album", false, Arrays.asList("album"), Arrays.asList("ASC")));
        _indicesSongs.add(new TableInfo.Index("index_songs_is_favorite", false, Arrays.asList("is_favorite"), Arrays.asList("ASC")));
        final TableInfo _infoSongs = new TableInfo("songs", _columnsSongs, _foreignKeysSongs, _indicesSongs);
        final TableInfo _existingSongs = TableInfo.read(connection, "songs");
        if (!_infoSongs.equals(_existingSongs)) {
          return new RoomOpenDelegate.ValidationResult(false, "songs(com.example.nghenhac.data.local.entity.SongEntity).\n"
                  + " Expected:\n" + _infoSongs + "\n"
                  + " Found:\n" + _existingSongs);
        }
        final Map<String, TableInfo.Column> _columnsPlaylists = new HashMap<String, TableInfo.Column>(5);
        _columnsPlaylists.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylists.put("song_count", new TableInfo.Column("song_count", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysPlaylists = new HashSet<TableInfo.ForeignKey>(0);
        final Set<TableInfo.Index> _indicesPlaylists = new HashSet<TableInfo.Index>(1);
        _indicesPlaylists.add(new TableInfo.Index("index_playlists_name", true, Arrays.asList("name"), Arrays.asList("ASC")));
        final TableInfo _infoPlaylists = new TableInfo("playlists", _columnsPlaylists, _foreignKeysPlaylists, _indicesPlaylists);
        final TableInfo _existingPlaylists = TableInfo.read(connection, "playlists");
        if (!_infoPlaylists.equals(_existingPlaylists)) {
          return new RoomOpenDelegate.ValidationResult(false, "playlists(com.example.nghenhac.data.local.entity.PlaylistEntity).\n"
                  + " Expected:\n" + _infoPlaylists + "\n"
                  + " Found:\n" + _existingPlaylists);
        }
        final Map<String, TableInfo.Column> _columnsPlaylistSongs = new HashMap<String, TableInfo.Column>(3);
        _columnsPlaylistSongs.put("playlist_id", new TableInfo.Column("playlist_id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistSongs.put("song_id", new TableInfo.Column("song_id", "INTEGER", true, 2, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlaylistSongs.put("order_index", new TableInfo.Column("order_index", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysPlaylistSongs = new HashSet<TableInfo.ForeignKey>(2);
        _foreignKeysPlaylistSongs.add(new TableInfo.ForeignKey("playlists", "CASCADE", "NO ACTION", Arrays.asList("playlist_id"), Arrays.asList("id")));
        _foreignKeysPlaylistSongs.add(new TableInfo.ForeignKey("songs", "CASCADE", "NO ACTION", Arrays.asList("song_id"), Arrays.asList("id")));
        final Set<TableInfo.Index> _indicesPlaylistSongs = new HashSet<TableInfo.Index>(2);
        _indicesPlaylistSongs.add(new TableInfo.Index("index_playlist_songs_playlist_id", false, Arrays.asList("playlist_id"), Arrays.asList("ASC")));
        _indicesPlaylistSongs.add(new TableInfo.Index("index_playlist_songs_song_id", false, Arrays.asList("song_id"), Arrays.asList("ASC")));
        final TableInfo _infoPlaylistSongs = new TableInfo("playlist_songs", _columnsPlaylistSongs, _foreignKeysPlaylistSongs, _indicesPlaylistSongs);
        final TableInfo _existingPlaylistSongs = TableInfo.read(connection, "playlist_songs");
        if (!_infoPlaylistSongs.equals(_existingPlaylistSongs)) {
          return new RoomOpenDelegate.ValidationResult(false, "playlist_songs(com.example.nghenhac.data.local.entity.PlaylistSongCrossRef).\n"
                  + " Expected:\n" + _infoPlaylistSongs + "\n"
                  + " Found:\n" + _existingPlaylistSongs);
        }
        final Map<String, TableInfo.Column> _columnsCachedSongs = new HashMap<String, TableInfo.Column>(6);
        _columnsCachedSongs.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedSongs.put("song_id", new TableInfo.Column("song_id", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedSongs.put("local_file_path", new TableInfo.Column("local_file_path", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedSongs.put("file_size", new TableInfo.Column("file_size", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedSongs.put("cached_at", new TableInfo.Column("cached_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCachedSongs.put("is_full_download", new TableInfo.Column("is_full_download", "INTEGER", true, 0, "0", TableInfo.CREATED_FROM_ENTITY));
        final Set<TableInfo.ForeignKey> _foreignKeysCachedSongs = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCachedSongs.add(new TableInfo.ForeignKey("songs", "CASCADE", "NO ACTION", Arrays.asList("song_id"), Arrays.asList("id")));
        final Set<TableInfo.Index> _indicesCachedSongs = new HashSet<TableInfo.Index>(1);
        _indicesCachedSongs.add(new TableInfo.Index("index_cached_songs_song_id", true, Arrays.asList("song_id"), Arrays.asList("ASC")));
        final TableInfo _infoCachedSongs = new TableInfo("cached_songs", _columnsCachedSongs, _foreignKeysCachedSongs, _indicesCachedSongs);
        final TableInfo _existingCachedSongs = TableInfo.read(connection, "cached_songs");
        if (!_infoCachedSongs.equals(_existingCachedSongs)) {
          return new RoomOpenDelegate.ValidationResult(false, "cached_songs(com.example.nghenhac.data.local.entity.CachedSongEntity).\n"
                  + " Expected:\n" + _infoCachedSongs + "\n"
                  + " Found:\n" + _existingCachedSongs);
        }
        return new RoomOpenDelegate.ValidationResult(true, null);
      }
    };
    return _openDelegate;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final Map<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final Map<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "songs", "playlists", "playlist_songs", "cached_songs");
  }

  @Override
  public void clearAllTables() {
    super.performClear(true, "songs", "playlists", "playlist_songs", "cached_songs");
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final Map<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(SongDao.class, SongDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlaylistDao.class, PlaylistDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CachedSongDao.class, CachedSongDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final Set<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public SongDao songDao() {
    if (_songDao != null) {
      return _songDao;
    } else {
      synchronized(this) {
        if(_songDao == null) {
          _songDao = new SongDao_Impl(this);
        }
        return _songDao;
      }
    }
  }

  @Override
  public PlaylistDao playlistDao() {
    if (_playlistDao != null) {
      return _playlistDao;
    } else {
      synchronized(this) {
        if(_playlistDao == null) {
          _playlistDao = new PlaylistDao_Impl(this);
        }
        return _playlistDao;
      }
    }
  }

  @Override
  public CachedSongDao cachedSongDao() {
    if (_cachedSongDao != null) {
      return _cachedSongDao;
    } else {
      synchronized(this) {
        if(_cachedSongDao == null) {
          _cachedSongDao = new CachedSongDao_Impl(this);
        }
        return _cachedSongDao;
      }
    }
  }
}
