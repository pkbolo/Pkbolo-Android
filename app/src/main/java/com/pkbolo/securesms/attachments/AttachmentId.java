package com.pkbolo.securesms.attachments;

import androidx.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pkbolo.securesms.util.Util;

public class AttachmentId {

  @JsonProperty
  private final long rowId;

  @JsonProperty
  private final long uniqueId;

  public AttachmentId(@JsonProperty("rowId") long rowId, @JsonProperty("uniqueId") long uniqueId) {
    this.rowId    = rowId;
    this.uniqueId = uniqueId;
  }

  public long getRowId() {
    return rowId;
  }

  public long getUniqueId() {
    return uniqueId;
  }

  public String[] toStrings() {
    return new String[] {String.valueOf(rowId), String.valueOf(uniqueId)};
  }

  public @NonNull String toString() {
    return "AttachmentId::(" + rowId + ", " + uniqueId + ")";
  }

  public boolean isValid() {
    return rowId >= 0 && uniqueId >= 0;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    AttachmentId attachmentId = (AttachmentId)o;

    if (rowId != attachmentId.rowId) return false;
    return uniqueId == attachmentId.uniqueId;
  }

  @Override
  public int hashCode() {
    return Util.hashCode(rowId, uniqueId);
  }
}
