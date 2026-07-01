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
import com.example.nghenhac.data.local.entity.CachedSongEntity;
import java.lang.Class;
import java.lang.Integer;
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
public final class CachedSongDao_Impl implements CachedSongDao {
  private final RoomDatabase __db;

  private final EntityInsertAdapter<CachedSongEntity> __insertAdapterOfCachedSongEntity;

  private final EntityDeleteOrUpdateAdapter<CachedSongEntity> __deleteAdapterOfCachedSongEntity;

  private final EntityDeleteOrUpdateAdapter<CachedSongEntity> __updateAdapterOfCachedSongEntity;

  public CachedSongDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertAdapterOfCachedSongEntity = new EntityInsertAdapter<CachedSongEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `cached_songs` (`id`,`song_id`,`local_file_path`,`file_size`,`cached_at`,`is_full_download`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final CachedSongEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSongId());
        if (entity.getLocalFilePath() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getLocalFilePath());
        }
        statement.bindLong(4, entity.getFileSize());
        statement.bindLong(5, entity.getCachedAt());
        final int _tmp = entity.isFullDownload() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__deleteAdapterOfCachedSongEntity = new EntityDeleteOrUpdateAdapter<CachedSongEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `cached_songs` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final CachedSongEntity entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfCachedSongEntity = new EntityDeleteOrUpdateAdapter<CachedSongEntity>() {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `cached_songs` SET `id` = ?,`song_id` = ?,`local_file_path` = ?,`file_size` = ?,`cached_at` = ?,`is_full_download` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SQLiteStatement statement, final CachedSongEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getSongId());
        if (entity.getLocalFilePath() == null) {
          statement.bindNull(3);
        } else {
          statement.bindText(3, entity.getLocalFilePath());
        }
        statement.bindLong(4, entity.getFileSize());
        statement.bindLong(5, entity.getCachedAt());
        final int _tmp = entity.isFullDownload() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindLong(7, entity.getId());
      }
    };
  }

  @Override
  public long insert(final CachedSongEntity cachedSong) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      return __insertAdapterOfCachedSongEntity.insertAndReturnId(_connection, cachedSong);
    });
  }

  @Override
  public int delete(final CachedSongEntity cachedSong) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      int _result = 0;
      _result += __deleteAdapterOfCachedSongEntity.handle(_connection, cachedSong);
      return _result;
    });
  }

  @Override
  public int update(final CachedSongEntity cachedSong) {
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      int _result = 0;
      _result += __updateAdapterOfCachedSongEntity.handle(_connection, cachedSong);
      return _result;
    });
  }

  @Override
  public LiveData<List<CachedSongEntity>> getAllCachedSongs() {
    final String _sql = "SELECT * FROM cached_songs ORDER BY cached_at DESC";
    return __db.getInvalidationTracker().createLiveData(new String[] {"cached_songs"}, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfSongId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "song_id");
        final int _columnIndexOfLocalFilePath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "local_file_path");
        final int _columnIndexOfFileSize = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "file_size");
        final int _columnIndexOfCachedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cached_at");
        final int _columnIndexOfIsFullDownload = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "is_full_download");
        final List<CachedSongEntity> _result = new ArrayList<CachedSongEntity>();
        while (_stmt.step()) {
          final CachedSongEntity _item;
          _item = new CachedSongEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _item.setId(_tmpId);
          final long _tmpSongId;
          _tmpSongId = _stmt.getLong(_columnIndexOfSongId);
          _item.setSongId(_tmpSongId);
          final String _tmpLocalFilePath;
          if (_stmt.isNull(_columnIndexOfLocalFilePath)) {
            _tmpLocalFilePath = null;
          } else {
            _tmpLocalFilePath = _stmt.getText(_columnIndexOfLocalFilePath);
          }
          _item.setLocalFilePath(_tmpLocalFilePath);
          final long _tmpFileSize;
          _tmpFileSize = _stmt.getLong(_columnIndexOfFileSize);
          _item.setFileSize(_tmpFileSize);
          final long _tmpCachedAt;
          _tmpCachedAt = _stmt.getLong(_columnIndexOfCachedAt);
          _item.setCachedAt(_tmpCachedAt);
          final boolean _tmpIsFullDownload;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsFullDownload));
          _tmpIsFullDownload = _tmp != 0;
          _item.setFullDownload(_tmpIsFullDownload);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public CachedSongEntity getBySongId(final long songId) {
    final String _sql = "SELECT * FROM cached_songs WHERE song_id = ? LIMIT 1";
    return DBUtil.performBlocking(__db, true, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, songId);
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfSongId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "song_id");
        final int _columnIndexOfLocalFilePath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "local_file_path");
        final int _columnIndexOfFileSize = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "file_size");
        final int _columnIndexOfCachedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cached_at");
        final int _columnIndexOfIsFullDownload = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "is_full_download");
        final CachedSongEntity _result;
        if (_stmt.step()) {
          _result = new CachedSongEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _result.setId(_tmpId);
          final long _tmpSongId;
          _tmpSongId = _stmt.getLong(_columnIndexOfSongId);
          _result.setSongId(_tmpSongId);
          final String _tmpLocalFilePath;
          if (_stmt.isNull(_columnIndexOfLocalFilePath)) {
            _tmpLocalFilePath = null;
          } else {
            _tmpLocalFilePath = _stmt.getText(_columnIndexOfLocalFilePath);
          }
          _result.setLocalFilePath(_tmpLocalFilePath);
          final long _tmpFileSize;
          _tmpFileSize = _stmt.getLong(_columnIndexOfFileSize);
          _result.setFileSize(_tmpFileSize);
          final long _tmpCachedAt;
          _tmpCachedAt = _stmt.getLong(_columnIndexOfCachedAt);
          _result.setCachedAt(_tmpCachedAt);
          final boolean _tmpIsFullDownload;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsFullDownload));
          _tmpIsFullDownload = _tmp != 0;
          _result.setFullDownload(_tmpIsFullDownload);
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
  public LiveData<List<CachedSongEntity>> getFullDownloads() {
    final String _sql = "SELECT * FROM cached_songs WHERE is_full_download = 1 ORDER BY cached_at DESC";
    return __db.getInvalidationTracker().createLiveData(new String[] {"cached_songs"}, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final int _columnIndexOfId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "id");
        final int _columnIndexOfSongId = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "song_id");
        final int _columnIndexOfLocalFilePath = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "local_file_path");
        final int _columnIndexOfFileSize = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "file_size");
        final int _columnIndexOfCachedAt = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "cached_at");
        final int _columnIndexOfIsFullDownload = SQLiteStatementUtil.getColumnIndexOrThrow(_stmt, "is_full_download");
        final List<CachedSongEntity> _result = new ArrayList<CachedSongEntity>();
        while (_stmt.step()) {
          final CachedSongEntity _item;
          _item = new CachedSongEntity();
          final long _tmpId;
          _tmpId = _stmt.getLong(_columnIndexOfId);
          _item.setId(_tmpId);
          final long _tmpSongId;
          _tmpSongId = _stmt.getLong(_columnIndexOfSongId);
          _item.setSongId(_tmpSongId);
          final String _tmpLocalFilePath;
          if (_stmt.isNull(_columnIndexOfLocalFilePath)) {
            _tmpLocalFilePath = null;
          } else {
            _tmpLocalFilePath = _stmt.getText(_columnIndexOfLocalFilePath);
          }
          _item.setLocalFilePath(_tmpLocalFilePath);
          final long _tmpFileSize;
          _tmpFileSize = _stmt.getLong(_columnIndexOfFileSize);
          _item.setFileSize(_tmpFileSize);
          final long _tmpCachedAt;
          _tmpCachedAt = _stmt.getLong(_columnIndexOfCachedAt);
          _item.setCachedAt(_tmpCachedAt);
          final boolean _tmpIsFullDownload;
          final int _tmp;
          _tmp = (int) (_stmt.getLong(_columnIndexOfIsFullDownload));
          _tmpIsFullDownload = _tmp != 0;
          _item.setFullDownload(_tmpIsFullDownload);
          _result.add(_item);
        }
        return _result;
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public LiveData<Integer> getCacheCount() {
    final String _sql = "SELECT COUNT(*) FROM cached_songs";
    return __db.getInvalidationTracker().createLiveData(new String[] {"cached_songs"}, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final Integer _result;
        if (_stmt.step()) {
          final Integer _tmp;
          if (_stmt.isNull(0)) {
            _tmp = null;
          } else {
            _tmp = (int) (_stmt.getLong(0));
          }
          _result = _tmp;
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
  public LiveData<Long> getTotalCacheSize() {
    final String _sql = "SELECT COALESCE(SUM(file_size), 0) FROM cached_songs";
    return __db.getInvalidationTracker().createLiveData(new String[] {"cached_songs"}, false, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        final Long _result;
        if (_stmt.step()) {
          final Long _tmp;
          if (_stmt.isNull(0)) {
            _tmp = null;
          } else {
            _tmp = _stmt.getLong(0);
          }
          _result = _tmp;
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
  public int deleteBySongId(final long songId) {
    final String _sql = "DELETE FROM cached_songs WHERE song_id = ?";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, songId);
        _stmt.step();
        return SQLiteConnectionUtil.getTotalChangedRows(_connection);
      } finally {
        _stmt.close();
      }
    });
  }

  @Override
  public int deleteAll() {
    final String _sql = "DELETE FROM cached_songs";
    return DBUtil.performBlocking(__db, false, true, (_connection) -> {
      final SQLiteStatement _stmt = _connection.prepare(_sql);
      try {
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
