package com.example.myapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.StrictMode;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;

public class AppUpdater {

    private static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/seangritthy/vdomov-apks/releases/latest";

    public static void checkForUpdates(final Activity activity, final boolean showNoUpdateToast) {
        new CheckUpdateTask(activity, showNoUpdateToast).execute();
    }

    private static class CheckUpdateTask extends AsyncTask<Void, Void, JSONObject> {
        private final Activity activity;
        private final boolean showNoUpdateToast;

        CheckUpdateTask(Activity activity, boolean showNoUpdateToast) {
            this.activity = activity;
            this.showNoUpdateToast = showNoUpdateToast;
        }

        @Override
        protected JSONObject doInBackground(Void... params) {
            try {
                URL url = new URL(GITHUB_RELEASES_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
                conn.setRequestProperty("User-Agent", "VDomovAndroidApp");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    reader.close();
                    return new JSONObject(sb.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            if (activity == null || activity.isFinishing()) return;

            if (result == null) {
                return;
            }

            try {
                String latestTag = result.optString("tag_name", "").replace("v", "").trim();
                String releaseNotes = result.optString("body", "Bug fixes and performance improvements.");
                String downloadUrl = null;

                JSONArray assets = result.optJSONArray("assets");
                if (assets != null) {
                    for (int i = 0; i < assets.length(); i++) {
                        JSONObject asset = assets.getJSONObject(i);
                        if (asset.optString("name", "").toLowerCase().endsWith(".apk")) {
                            downloadUrl = asset.optString("browser_download_url", null);
                            break;
                        }
                    }
                }

                String currentVersion = getCurrentVersionName(activity);

                if (isNewerVersion(currentVersion, latestTag) && downloadUrl != null) {
                    showUpdateDialog(activity, latestTag, releaseNotes, downloadUrl);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String getCurrentVersionName(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionName.replace("v", "").trim();
        } catch (PackageManager.NameNotFoundException e) {
            return "1.0.0";
        }
    }

    private static boolean isNewerVersion(String current, String latest) {
        if (current == null || latest == null) return false;
        String[] currParts = current.split("\\.");
        String[] lateParts = latest.split("\\.");

        int length = Math.max(currParts.length, lateParts.length);
        for (int i = 0; i < length; i++) {
            int c = i < currParts.length ? parseSafeInt(currParts[i]) : 0;
            int l = i < lateParts.length ? parseSafeInt(lateParts[i]) : 0;
            if (l > c) return true;
            if (c > l) return false;
        }
        return false;
    }

    private static int parseSafeInt(String val) {
        try {
            return Integer.parseInt(val.replaceAll("[^0-9]", ""));
        } catch (Exception e) {
            return 0;
        }
    }

    private static void showUpdateDialog(final Activity activity, final String newVersion, String releaseNotes, final String downloadUrl) {
        new AlertDialog.Builder(activity)
                .setTitle("Update Available (v" + newVersion + ")")
                .setMessage("A new version of vdomov is available!\n\nWhat's New:\n" + releaseNotes)
                .setPositiveButton("Install Update", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        downloadAndInstallApk(activity, downloadUrl);
                    }
                })
                .setNegativeButton("Later", null)
                .setCancelable(true)
                .show();
    }

    private static void downloadAndInstallApk(final Activity activity, final String downloadUrl) {
        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setTitle("Downloading Update");
        progressDialog.setMessage("Please wait while downloading the latest APK...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.setMax(100);
        progressDialog.show();

        new AsyncTask<Void, Integer, File>() {
            @Override
            protected File doInBackground(Void... params) {
                try {
                    URL url = new URL(downloadUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setInstanceFollowRedirects(true);
                    conn.connect();

                    int fileLength = conn.getContentLength();
                    File apkFile = new File(activity.getExternalFilesDir(null), "update.apk");
                    if (apkFile.exists()) {
                        apkFile.delete();
                    }

                    InputStream input = conn.getInputStream();
                    FileOutputStream output = new FileOutputStream(apkFile);

                    byte[] data = new byte[4096];
                    long total = 0;
                    int count;
                    while ((count = input.read(data)) != -1) {
                        total += count;
                        if (fileLength > 0) {
                            publishProgress((int) (total * 100 / fileLength));
                        }
                        output.write(data, 0, count);
                    }

                    output.flush();
                    output.close();
                    input.close();
                    return apkFile;
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                progressDialog.setProgress(values[0]);
            }

            @Override
            protected void onPostExecute(File apkFile) {
                progressDialog.dismiss();
                if (apkFile != null && apkFile.exists()) {
                    installApk(activity, apkFile);
                }
            }
        }.execute();
    }

    private static void installApk(Activity activity, File apkFile) {
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            }
        } catch (Exception ignored) {
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        activity.startActivity(intent);
    }
}
