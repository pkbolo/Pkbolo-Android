package com.pkbolo.securesms.database.documents;

import java.util.List;

public interface Document<T> {

  public int size();
  public List<T> getList();

}
