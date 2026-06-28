package com.example.nghenhac.ui.library;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.example.nghenhac.R;
import com.example.nghenhac.data.repository.PlaylistRepository;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

/**
 * CreatePlaylistDialog — Dialog tạo playlist mới.
 * <p>
 * Nguyên lý:
 * - MaterialAlertDialog với EditText cho tên playlist.
 * - Nút "Tạo" disabled khi tên trống, vi phạm unique constraint được xử lý qua callback error.
 * - Sau khi tạo thành công, hiển thị Snackbar thông báo.
 * <p>
 * Luồng xử lý:
 * 1. User nhập tên playlist → nút "Tạo" enabled (tên không rỗng).
 * 2. User ấn "Tạo" → PlaylistRepository.createPlaylist() async.
 * 3. Callback onCreated() → Snackbar + tự động đóng dialog.
 * 4. Callback onError() → hiển thị lỗi (unique constraint, network...).
 */
public class CreatePlaylistDialog {

    /**
     * Hiển thị dialog tạo playlist mới.
     *
     * @param context    Context (Activity hoặc Fragment's requireContext()).
     * @param anchorView View để hiển thị Snackbar (thường là RecyclerView hoặc root layout).
     */
    public static void show(@NonNull Context context, @NonNull android.view.View anchorView) {
        // Inflate dialog layout
        android.view.View dialogView = android.view.LayoutInflater.from(context)
                .inflate(R.layout.dialog_create_playlist, null);

        EditText nameInput = dialogView.findViewById(R.id.dialog_playlist_name_input);

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context)
                .setTitle(R.string.dialog_create_playlist_title)
                .setView(dialogView)
                .setPositiveButton(R.string.action_create, null) // Set later to prevent auto-dismiss
                .setNegativeButton(android.R.string.cancel, null);

        AlertDialog dialog = builder.create();
        dialog.show();

        // Disable positive button initially
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setEnabled(false);

        // Listen for text changes to enable/disable the create button
        nameInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                positiveButton.setEnabled(s.toString().trim().length() > 0);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No-op
            }
        });

        // Handle create button click manually
        positiveButton.setOnClickListener(v -> {
            String name = nameInput.getText().toString().trim();
            if (name.isEmpty()) {
                return;
            }

            // Show loading state
            positiveButton.setEnabled(false);
            positiveButton.setText(R.string.action_creating);

            Handler mainHandler = new Handler(Looper.getMainLooper());
            PlaylistRepository repository = PlaylistRepository.getInstance(context);
            repository.createPlaylist(name, null, new PlaylistRepository.OnPlaylistCreated() {
                @Override
                public void onCreated(long playlistId) {
                    // Callback chạy trên background thread → post UI operations lên main thread
                    mainHandler.post(() -> {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                        Snackbar.make(anchorView,
                                context.getString(R.string.playlist_created, name),
                                Snackbar.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onError(Exception e) {
                    mainHandler.post(() -> {
                        positiveButton.setEnabled(true);
                        positiveButton.setText(R.string.action_create);
                        Snackbar.make(anchorView,
                                context.getString(R.string.playlist_create_error, e.getMessage()),
                                Snackbar.LENGTH_LONG).show();
                    });
                }
            });
        });
    }
}
