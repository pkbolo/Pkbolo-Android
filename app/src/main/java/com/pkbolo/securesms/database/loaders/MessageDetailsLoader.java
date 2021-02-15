/**
 * Copyright (C) 2015 Open Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.pkbolo.securesms.database.loaders;

import android.content.Context;
import android.database.Cursor;

import com.pkbolo.securesms.database.DatabaseFactory;
import com.pkbolo.securesms.database.MmsSmsDatabase;
import com.pkbolo.securesms.util.AbstractCursorLoader;

public class MessageDetailsLoader extends AbstractCursorLoader {
  private final String type;
  private final long   messageId;

  public MessageDetailsLoader(Context context, String type, long messageId) {
    super(context);
    this.type      = type;
    this.messageId = messageId;
  }

  @Override
  public Cursor getCursor() {
    switch (type) {
      case MmsSmsDatabase.SMS_TRANSPORT:
        return DatabaseFactory.getSmsDatabase(context).getMessageCursor(messageId);
      case MmsSmsDatabase.MMS_TRANSPORT:
        return DatabaseFactory.getMmsDatabase(context).getMessage(messageId);
      default:
        throw new AssertionError("no valid message type specified");
    }
  }
}
