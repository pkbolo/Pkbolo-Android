package com.pkbolo.securesms.util;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.core.graphics.drawable.IconCompat;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.pkbolo.securesms.color.MaterialColor;

import com.pkbolo.securesms.R;

import com.pkbolo.securesms.contacts.avatars.ContactColors;
import com.pkbolo.securesms.contacts.avatars.GeneratedContactPhoto;
import com.pkbolo.securesms.contacts.avatars.ProfileContactPhoto;
import com.pkbolo.securesms.mms.GlideApp;
import com.pkbolo.securesms.mms.GlideRequest;
import com.pkbolo.securesms.recipients.Recipient;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.concurrent.ExecutionException;

public final class AvatarUtil {

  private AvatarUtil() {
  }

  public static void loadIconIntoImageView(@NonNull Recipient recipient, @NonNull ImageView target) {
    Context  context  = target.getContext();

    request(GlideApp.with(context).asDrawable(), context, recipient).into(target);
  }

  @WorkerThread
  public static IconCompat getIconForNotification(@NonNull Context context, @NonNull Recipient recipient) {
    try {
      return IconCompat.createWithBitmap(request(GlideApp.with(context).asBitmap(), context, recipient).submit().get());
    } catch (ExecutionException | InterruptedException e) {
      return null;
    }
  }

  private static <T> GlideRequest<T> request(@NonNull GlideRequest<T> glideRequest, @NonNull Context context, @NonNull Recipient recipient) {
    return glideRequest.load(new ProfileContactPhoto(recipient.getId(), String.valueOf(TextSecurePreferences.getProfileAvatarId(context))))
                       .error(getFallback(context, recipient))
                       .circleCrop()
                       .diskCacheStrategy(DiskCacheStrategy.ALL);
  }

  private static Drawable getFallback(@NonNull Context context, @NonNull Recipient recipient) {
    String        name          = Optional.fromNullable(recipient.getDisplayName(context)).or(Optional.fromNullable(TextSecurePreferences.getProfileName(context))).or("");
    MaterialColor fallbackColor = recipient.getColor();

    if (fallbackColor == ContactColors.UNKNOWN_COLOR && !TextUtils.isEmpty(name)) {
      fallbackColor = ContactColors.generateFor(name);
    }

    return new GeneratedContactPhoto(name, R.drawable.ic_profile_outline_40).asDrawable(context, fallbackColor.toAvatarColor(context));
  }
}
