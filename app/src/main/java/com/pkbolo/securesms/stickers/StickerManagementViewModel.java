package com.pkbolo.securesms.stickers;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import android.database.ContentObserver;
import android.os.Handler;
import androidx.annotation.NonNull;

import com.pkbolo.securesms.database.DatabaseContentProviders;

final class StickerManagementViewModel extends ViewModel {

  private final Application                 application;
  private final StickerManagementRepository repository;
  private final MutableLiveData<StickerManagementRepository.PackResult> packs;
  private final ContentObserver             observer;

  private StickerManagementViewModel(@NonNull Application application, @NonNull StickerManagementRepository repository) {
    this.application = application;
    this.repository  = repository;
    this.packs       = new MutableLiveData<>();
    this.observer    = new ContentObserver(new Handler()) {
      @Override
      public void onChange(boolean selfChange) {
        repository.deleteOrphanedStickerPacks();
        repository.getStickerPacks(packs::postValue);
      }
    };

    application.getContentResolver().registerContentObserver(DatabaseContentProviders.StickerPack.CONTENT_URI, true, observer);
  }

  void init() {
    repository.deleteOrphanedStickerPacks();
    repository.fetchUnretrievedReferencePacks();
  }

  void onVisible() {
    repository.deleteOrphanedStickerPacks();
  }

  @NonNull LiveData<StickerManagementRepository.PackResult> getStickerPacks() {
    repository.getStickerPacks(packs::postValue);
    return packs;
  }

  void onStickerPackUninstallClicked(@NonNull String packId, @NonNull String packKey) {
    repository.uninstallStickerPack(packId, packKey);
  }

  void onStickerPackInstallClicked(@NonNull String packId, @NonNull String packKey) {
    repository.installStickerPack(packId, packKey, false);
  }

  @Override
  protected void onCleared() {
    application.getContentResolver().unregisterContentObserver(observer);
  }

  static class Factory extends ViewModelProvider.NewInstanceFactory {

    private final Application                 application;
    private final StickerManagementRepository repository;

    Factory(@NonNull Application application, @NonNull StickerManagementRepository repository) {
      this.application = application;
      this.repository  = repository;
    }

    @Override
    public @NonNull <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      //noinspection ConstantConditions
      return modelClass.cast(new StickerManagementViewModel(application, repository));
    }
  }
}
