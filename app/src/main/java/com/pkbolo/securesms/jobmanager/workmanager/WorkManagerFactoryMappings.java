package com.pkbolo.securesms.jobmanager.workmanager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pkbolo.securesms.jobs.AttachmentDownloadJob;
import com.pkbolo.securesms.jobs.AttachmentUploadJob;
import com.pkbolo.securesms.jobs.AvatarDownloadJob;
import com.pkbolo.securesms.jobs.CleanPreKeysJob;
import com.pkbolo.securesms.jobs.CreateSignedPreKeyJob;
import com.pkbolo.securesms.jobs.DirectoryRefreshJob;
import com.pkbolo.securesms.jobs.FailingJob;
import com.pkbolo.securesms.jobs.FcmRefreshJob;
import com.pkbolo.securesms.jobs.LocalBackupJob;
import com.pkbolo.securesms.jobs.MmsDownloadJob;
import com.pkbolo.securesms.jobs.MmsReceiveJob;
import com.pkbolo.securesms.jobs.MmsSendJob;
import com.pkbolo.securesms.jobs.MultiDeviceBlockedUpdateJob;
import com.pkbolo.securesms.jobs.MultiDeviceConfigurationUpdateJob;
import com.pkbolo.securesms.jobs.MultiDeviceContactUpdateJob;
import com.pkbolo.securesms.jobs.MultiDeviceGroupUpdateJob;
import com.pkbolo.securesms.jobs.MultiDeviceProfileKeyUpdateJob;
import com.pkbolo.securesms.jobs.MultiDeviceReadUpdateJob;
import com.pkbolo.securesms.jobs.MultiDeviceVerifiedUpdateJob;
import com.pkbolo.securesms.jobs.PushDecryptMessageJob;
import com.pkbolo.securesms.jobs.PushGroupSendJob;
import com.pkbolo.securesms.jobs.PushGroupUpdateJob;
import com.pkbolo.securesms.jobs.PushMediaSendJob;
import com.pkbolo.securesms.jobs.PushNotificationReceiveJob;
import com.pkbolo.securesms.jobs.PushTextSendJob;
import com.pkbolo.securesms.jobs.RefreshAttributesJob;
import com.pkbolo.securesms.jobs.RefreshPreKeysJob;
import com.pkbolo.securesms.jobs.RequestGroupInfoJob;
import com.pkbolo.securesms.jobs.RetrieveProfileAvatarJob;
import com.pkbolo.securesms.jobs.RetrieveProfileJob;
import com.pkbolo.securesms.jobs.RotateCertificateJob;
import com.pkbolo.securesms.jobs.RotateProfileKeyJob;
import com.pkbolo.securesms.jobs.RotateSignedPreKeyJob;
import com.pkbolo.securesms.jobs.SendDeliveryReceiptJob;
import com.pkbolo.securesms.jobs.SendReadReceiptJob;
import com.pkbolo.securesms.jobs.ServiceOutageDetectionJob;
import com.pkbolo.securesms.jobs.SmsReceiveJob;
import com.pkbolo.securesms.jobs.SmsSendJob;
import com.pkbolo.securesms.jobs.SmsSentJob;
import com.pkbolo.securesms.jobs.TrimThreadJob;
import com.pkbolo.securesms.jobs.TypingSendJob;
import com.pkbolo.securesms.jobs.UpdateApkJob;

import java.util.HashMap;
import java.util.Map;

public class WorkManagerFactoryMappings {

  private static final Map<String, String> FACTORY_MAP = new HashMap<String, String>() {{
    put(AttachmentDownloadJob.class.getName(), AttachmentDownloadJob.KEY);
    put(AttachmentUploadJob.class.getName(), AttachmentUploadJob.KEY);
    put(AvatarDownloadJob.class.getName(), AvatarDownloadJob.KEY);
    put(CleanPreKeysJob.class.getName(), CleanPreKeysJob.KEY);
    put(CreateSignedPreKeyJob.class.getName(), CreateSignedPreKeyJob.KEY);
    put(DirectoryRefreshJob.class.getName(), DirectoryRefreshJob.KEY);
    put(FcmRefreshJob.class.getName(), FcmRefreshJob.KEY);
    put(LocalBackupJob.class.getName(), LocalBackupJob.KEY);
    put(MmsDownloadJob.class.getName(), MmsDownloadJob.KEY);
    put(MmsReceiveJob.class.getName(), MmsReceiveJob.KEY);
    put(MmsSendJob.class.getName(), MmsSendJob.KEY);
    put(MultiDeviceBlockedUpdateJob.class.getName(), MultiDeviceBlockedUpdateJob.KEY);
    put(MultiDeviceConfigurationUpdateJob.class.getName(), MultiDeviceConfigurationUpdateJob.KEY);
    put(MultiDeviceContactUpdateJob.class.getName(), MultiDeviceContactUpdateJob.KEY);
    put(MultiDeviceGroupUpdateJob.class.getName(), MultiDeviceGroupUpdateJob.KEY);
    put(MultiDeviceProfileKeyUpdateJob.class.getName(), MultiDeviceProfileKeyUpdateJob.KEY);
    put(MultiDeviceReadUpdateJob.class.getName(), MultiDeviceReadUpdateJob.KEY);
    put(MultiDeviceVerifiedUpdateJob.class.getName(), MultiDeviceVerifiedUpdateJob.KEY);
    put("PushContentReceiveJob", FailingJob.KEY);
    put("PushDecryptJob", PushDecryptMessageJob.KEY);
    put(PushGroupSendJob.class.getName(), PushGroupSendJob.KEY);
    put(PushGroupUpdateJob.class.getName(), PushGroupUpdateJob.KEY);
    put(PushMediaSendJob.class.getName(), PushMediaSendJob.KEY);
    put(PushNotificationReceiveJob.class.getName(), PushNotificationReceiveJob.KEY);
    put(PushTextSendJob.class.getName(), PushTextSendJob.KEY);
    put(RefreshAttributesJob.class.getName(), RefreshAttributesJob.KEY);
    put(RefreshPreKeysJob.class.getName(), RefreshPreKeysJob.KEY);
    put("RefreshUnidentifiedDeliveryAbilityJob", FailingJob.KEY);
    put(RequestGroupInfoJob.class.getName(), RequestGroupInfoJob.KEY);
    put(RetrieveProfileAvatarJob.class.getName(), RetrieveProfileAvatarJob.KEY);
    put(RetrieveProfileJob.class.getName(), RetrieveProfileJob.KEY);
    put(RotateCertificateJob.class.getName(), RotateCertificateJob.KEY);
    put(RotateProfileKeyJob.class.getName(), RotateProfileKeyJob.KEY);
    put(RotateSignedPreKeyJob.class.getName(), RotateSignedPreKeyJob.KEY);
    put(SendDeliveryReceiptJob.class.getName(), SendDeliveryReceiptJob.KEY);
    put(SendReadReceiptJob.class.getName(), SendReadReceiptJob.KEY);
    put(ServiceOutageDetectionJob.class.getName(), ServiceOutageDetectionJob.KEY);
    put(SmsReceiveJob.class.getName(), SmsReceiveJob.KEY);
    put(SmsSendJob.class.getName(), SmsSendJob.KEY);
    put(SmsSentJob.class.getName(), SmsSentJob.KEY);
    put(TrimThreadJob.class.getName(), TrimThreadJob.KEY);
    put(TypingSendJob.class.getName(), TypingSendJob.KEY);
    put(UpdateApkJob.class.getName(), UpdateApkJob.KEY);
  }};

  public static @Nullable String getFactoryKey(@NonNull String workManagerClass) {
    return FACTORY_MAP.get(workManagerClass);
  }
}
