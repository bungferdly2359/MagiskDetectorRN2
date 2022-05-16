package com.magiskdetectorrn2;

import com.facebook.react.ReactActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;

public class MainActivity extends ReactActivity {

  /**
   * Returns the name of the main component registered from JavaScript. This is used to schedule
   * rendering of the component.
   */
  @Override
  protected String getMainComponentName() {
    return "MagiskDetectorRN2";
  }

  private static final String TAG = "MagiskDetector";
  private final ServiceConnection connection = new ServiceConnection() {
      @Override
      public void onServiceConnected(ComponentName name, IBinder binder) {
          showToast("RemoteService running");
          IRemoteService service = IRemoteService.Stub.asInterface(binder);
          try {
              setCard1(service.haveSu());
          } catch (RemoteException e) {
              Log.e(TAG, "RemoteException", e);
          }
      }

      @Override
      public void onServiceDisconnected(ComponentName name) {
          Log.w(TAG, name.getPackageName());
      }

      @Override
      public void onNullBinding(ComponentName name) {
          Log.w(TAG, name.getPackageName());
      }
  };

  @Override
  protected void onStart() {
      super.onStart();
      Intent intent = new Intent(getApplicationContext(), RemoteService.class);
      if (!bindService(intent, connection, BIND_AUTO_CREATE)) showToast("Bind service failed");
       setCard2(Native.haveMagicMount());
       setCard3(Native.findMagiskdSocket());
       setCard4(Native.haveSu() == 0);
       setCard5(Native.testIoctl());
       setCard6(props());
  }

  @Override
  protected void onStop() {
      super.onStop();
      unbindService(connection);
  }

  private int props() {
      SharedPreferences sp;
      try {
          MasterKey masterKey = new MasterKey.Builder(this)
                  .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                  .build();
          sp = EncryptedSharedPreferences.create(
                  this,
                  getPackageName(),
                  masterKey,
                  EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                  EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
          );
      } catch (GeneralSecurityException | IOException e) {
          Log.e(TAG, "Unable to open SharedPreferences.", e);
          return -1;
      }

      String spFingerprint = sp.getString("fingerprint", "");
      String fingerprint = Build.FINGERPRINT;
      Log.i(TAG, "spFingerprint=" + spFingerprint + " \n  fingerprint=" + fingerprint);
      String spBootId = sp.getString("boot_id", "");
      String bootId = getBootId();
      Log.i(TAG, "spBootId=" + spBootId + " \n  bootId=" + bootId);
      String spPropsHash = sp.getString("props_hash", "");
      if (spFingerprint.equals(fingerprint) && spBootId.length() > 0 && spPropsHash.length() > 0) {
          if (!spBootId.equals(bootId)) {
              return spPropsHash.equals(Native.getPropsHash()) ? 0 : 1;
          } else return 2;
      } else {
          SharedPreferences.Editor editor = sp.edit();
          editor.putString("fingerprint", fingerprint);
          editor.putString("boot_id", bootId);
          editor.putString("props_hash", Native.getPropsHash());
          editor.apply();
          return 2;
      }
  }

  private String getBootId() {
      String bootId = "";
      try (InputStream is = new FileInputStream("/proc/sys/kernel/random/boot_id")) {
          Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
          bootId = new BufferedReader(reader).readLine().trim();
      } catch (IOException e) {
          Log.w(TAG, "Can't read boot_id.", e);
      }
      if (bootId.length() == 0) {
          bootId = String.valueOf((System.currentTimeMillis() - SystemClock.elapsedRealtime()) / 10);
      }
      return bootId;
  }

  private void setCard1(boolean havesu) {
      if (havesu) {
          showToast("Root detected: SuperUser (RemoteService)");
      }
  }

  private void setCard2(int haveMagicMount) {
      switch (haveMagicMount) {
          case 0:
              break;
          case 1:
              showToast("Root detected: MagicMount");
              break;
          default:
              break;
      }
  }

  private void setCard3(int magiskdSocket) {
      switch (magiskdSocket) {
          case 0:
              break;
          case -1:
              break;
          case -2:
              break;
          case -3:
              break;
          default:
              showToast("Root detected: MagiskDSocket");
      }
  }

  private void setCard4(boolean havesu) {
      if (havesu) {
          showToast("Root detected: SuperUser");
      }
  }

  private void setCard5(int testIoctl) {
      switch (testIoctl) {
          case -1:
          default:
              break;
          case 0:
              break;
          case 1:
              break;
          case 2:
              showToast("Root detected: TestIoctl");
              break;
      }
  }

  private void setCard6(int props) {
      switch (props) {
          case -1:
          default:
              break;
          case 0:
              break;
          case 1:
              showToast("Root detected: PropsHash");
              break;
          case 2:
              break;
      }
  }

  private void showToast(String label) {
      Toast.makeText(getApplicationContext(),label,Toast.LENGTH_SHORT).show();
  }
}
