package com.pkbolo.securesms.database.loaders;

import android.content.Context;
import android.database.Cursor;

import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.util.AbstractCursorLoader;

public class BlockedContactsLoader extends AbstractCursorLoader {

  public BlockedContactsLoader(Context context) {
    super(context);
  }

  @Override
  public Cursor getCursor() {
    return DatabaseFactory.getRecipientDatabase(getContext()).getBlocked();
  }

}
