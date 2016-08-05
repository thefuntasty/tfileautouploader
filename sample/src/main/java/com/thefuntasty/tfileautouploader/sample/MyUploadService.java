package com.thefuntasty.tfileautouploader.sample;

import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.thefuntasty.tfileautouploader.BaseFileUploadService;
import com.thefuntasty.tfileautouploader.FileHolder;
import com.thefuntasty.tfileautouploader.FileUploadManager;
import com.thefuntasty.tfileautouploader.ItemUpdate;
import com.thefuntasty.tfileautouploader.Status;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

public class MyUploadService extends BaseFileUploadService {

	public MyUploadService() {
		super("MyUploadService");
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override public NotificationCompat.Builder createNotification() {
		return new NotificationCompat.Builder(this)
				.setContentTitle("Uploading")
				.setProgress(100, 0, false)
				.setSmallIcon(R.drawable.ic_dollar_1_normal);
	}

	@Override public void updateNotification(NotificationCompat.Builder builder, int photoCount, int currentPhoto) {
		builder.setContentText("Uploading photos: " + currentPhoto + "/" + photoCount);
	}

	@Override protected void uploadFileAndSave(Uri uri) {
		final FileUploadManager<Photo> uploadManager = com.thefuntasty.tfileautouploader.sample.MyUploadManager.get();
		final FileHolder<Photo> image = uploadManager.getImage(uri);

		// image removed - do not upload
		if (image == null) {
			decreaseFileCount();
			return;
		}

		image.status.statusType = Status.UPLOADING;

		Observable.interval(50, TimeUnit.MILLISECONDS)
				.take(101)
				.filter(new Func1<Long, Boolean>() {
					@Override public Boolean call(Long aLong) {
						return aLong % 10 == 0;
					}
				})
				.toBlocking()
				.subscribe(new Subscriber<Long>() {
					@Override public void onCompleted() { }
					@Override public void onError(Throwable e) { }
					@Override public void onNext(Long aLong) {
						if (image.status.statusType != Status.REMOVED) {
							image.status.progress = aLong.intValue();
							uploadManager.updateItem(image, ItemUpdate.PROGRESS);
							showNotificationProgress(aLong.intValue());
						} else {
							unsubscribe();
						}
					}
				});

		Observable.just("Fin")
				.toBlocking()
				.subscribe(new Subscriber<String>() {
					@Override public void onCompleted() { }
					@Override public void onError(Throwable e) { }
					@Override public void onNext(String s) {
						if (image.status.statusType != Status.REMOVED) {
							image.status.statusType = Status.UPLOADED;
							uploadManager.updateItem(image, ItemUpdate.STATUS);
						} else {
							unsubscribe();
						}
					}
				});
	}

	public static Intent getStarterIntent(FileHolder<Photo> image) {
		Intent intent = new Intent(com.thefuntasty.tfileautouploader.sample.App.context(), MyUploadService.class);
		intent.putExtra("uri", image.path);
		return intent;
	}
}
