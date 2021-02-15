package com.pkbolo.securesms.conversationlist;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import android.app.Application;
import android.database.ContentObserver;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.pkbolo.securesms.conversationlist.model.SearchResult;
import com.pkbolo.securesms.database.DatabaseContentProviders;
import com.pkbolo.securesms.search.SearchRepository;
import com.pkbolo.securesms.util.Debouncer;
import com.pkbolo.securesms.util.Util;
import com.pkbolo.securesms.dependencies.ApplicationDependencies;

class ConversationListViewModel extends ViewModel {

  private final Application                   application;
  private final MutableLiveData<SearchResult> searchResult;
  private final SearchRepository searchRepository;
  private final Debouncer debouncer;
  private final ContentObserver               observer;

  private String lastQuery;

  private ConversationListViewModel(@NonNull Application application, @NonNull SearchRepository searchRepository) {
    this.application      = application;
    this.searchResult     = new MutableLiveData<>();
    this.searchRepository = searchRepository;
    this.debouncer        = new Debouncer(300);
    this.observer         = new ContentObserver(new Handler()) {
      @Override
      public void onChange(boolean selfChange) {
        if (!TextUtils.isEmpty(getLastQuery())) {
          searchRepository.query(getLastQuery(), searchResult::postValue);
        }
      }
    };

    application.getContentResolver().registerContentObserver(DatabaseContentProviders.ConversationList.CONTENT_URI, true, observer);
  }

  @NonNull LiveData<SearchResult> getSearchResult() {
    return searchResult;
  }

  void updateQuery(String query) {
    lastQuery = query;
    debouncer.publish(() -> searchRepository.query(query, result -> {
      Util.runOnMain(() -> {
        if (query.equals(lastQuery)) {
          searchResult.setValue(result);
        }
      });
    }));
  }

  private @NonNull String getLastQuery() {
    return lastQuery == null ? "" : lastQuery;
  }

  @Override
  protected void onCleared() {
    debouncer.clear();
    application.getContentResolver().unregisterContentObserver(observer);
  }

  public static class Factory extends ViewModelProvider.NewInstanceFactory {
    @Override
    public @NonNull<T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      //noinspection ConstantConditions
      return modelClass.cast(new ConversationListViewModel(ApplicationDependencies.getApplication(), new SearchRepository()));
    }
  }
}
