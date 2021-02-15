package com.pkbolo.securesms.database.loaders;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import com.pkbolo.securesms.database.AttachmentDatabase;
import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.database.MediaDatabase;
import com.pkbolo.securesms.mms.PartAuthority;
import com.pkbolo.securesms.util.AsyncLoader;
import com.pkbolo.securesms.attachments.AttachmentId;

public final class PagingMediaLoader extends AsyncLoader<Pair<Cursor, Integer>> {

  @SuppressWarnings("unused")
  private static final String TAG = PagingMediaLoader.class.getSimpleName();

  private final Uri     uri;
  private final boolean leftIsRecent;
  private final MediaDatabase.Sorting sorting;
  private final long    threadId;

  public PagingMediaLoader(@NonNull Context context, long threadId, @NonNull Uri uri, boolean leftIsRecent, @NonNull MediaDatabase.Sorting sorting) {
    super(context);
    this.threadId     = threadId;
    this.uri          = uri;
    this.leftIsRecent = leftIsRecent;
    this.sorting      = sorting;
  }

  @Override
  public @Nullable Pair<Cursor, Integer> loadInBackground() {
    Cursor cursor = DatabaseFactory.getMediaDatabase(getContext()).getGalleryMediaForThread(threadId, sorting);

    while (cursor.moveToNext()) {
      AttachmentId attachmentId  = new AttachmentId(cursor.getLong(cursor.getColumnIndexOrThrow(AttachmentDatabase.ROW_ID)), cursor.getLong(cursor.getColumnIndexOrThrow(AttachmentDatabase.UNIQUE_ID)));
      Uri          attachmentUri = PartAuthority.getAttachmentDataUri(attachmentId);

      if (attachmentUri.equals(uri)) {
        return new Pair<>(cursor, leftIsRecent ? cursor.getPosition() : cursor.getCount() - 1 - cursor.getPosition());
      }
    }

    return null;
  }
}
