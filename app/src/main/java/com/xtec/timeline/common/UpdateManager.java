package com.xtec.timeline.common;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.util.Log;

import com.tbruyelle.rxpermissions.RxPermissions;
import com.xtec.timeline.service.UpdateService;
import com.xtec.timeline.utils.T;
import com.xtec.timeline.utils.Utils;
import com.xtec.timeline.widget.FastDialog;

import rx.functions.Action1;

/**
 * Created by 武昌丶鱼 on 2016/11/1.
 * Description:版本更新管理类
 */
public class UpdateManager {
    private static final String TAG = "UpdateManager";

    // 所需的全部权限
    static final String[] PERMISSIONS = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    private static String mUpdateUrl;

    public static void checkNewVersion(Context context,boolean isAutoCheck) {
        //通过网络去获取服务器端的版本号,也可以通过versionName判断
//        getVersionFromServer();
        int remoteVersion = 2;//版本号
        mUpdateUrl = "https://github.com/reyzarc/FastDevelopFrame/raw/master/app-debug.apk";//下载地址
        String feature = "1.这是第一个版本测试;\n2.有什么bug及时反馈;\n3.就算反馈了也不会修复的^_^";//更新描述
        int localVersion = Utils.getVersionCode(context);

        if (remoteVersion > localVersion) {//有新版本
            //弹出更新对话框
            //这里要先判断更新服务是否已经启动，如果已经在后台启动，则不要弹窗提示，否则会存在问题
            Log.e("reyzarc","服务是否在运行----->"+Utils.isServiceRunning(UpdateService.class,context));
            if (!Utils.isServiceRunning(UpdateService.class,context)) {
                showUpdateDialog(context,mUpdateUrl, feature);
            }
        }else if(!isAutoCheck){
            T.showShort(context,"当前已经是最新版!");
        }
    }

    private static void showUpdateDialog(final Context context, final String url, String feature) {
        new FastDialog(context)
                .setTitle("检查到新版本")
                .setContent(feature)
                .setPositiveButton("立即更新", new FastDialog.OnClickListener() {
                    @Override
                    public void onClick(FastDialog dialog) {
                        //需要判断是否有读写权限
                        if (Utils.hasPermissions(context, PERMISSIONS)) {//有权限
                            startUpdateService(context);
                            T.showShort(context, "即将更新...");
                        } else {//没有权限
                            showPermissionDialog(context);
                        }
                    }
                })
                .setNegativeButton("下次再说", new FastDialog.OnClickListener() {
                    @Override
                    public void onClick(FastDialog dialog) {
                        T.showShort(context, "走好不送...");
                    }
                })
                .setCancelable(false)
                .setCanceledOnTouchOutside(false)
                .create().show();

    }

    private static void showPermissionDialog(final Context context) {
        final RxPermissions rxPermissions = new RxPermissions((Activity) context);
        new FastDialog(context)
                .setTitle("提醒")
                .setContent("版本升级需要权限才能正常使用。\n点击下一步配置权限\n点击取消将无法正常升级")
                .setPositiveButton("下一步", new FastDialog.OnClickListener() {
                    @Override
                    public void onClick(FastDialog dialog) {
                        rxPermissions.request(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE)
                                .subscribe(new Action1<Boolean>() {
                                    @Override
                                    public void call(Boolean aBoolean) {
                                        if (!aBoolean) {//用户拒绝
                                            showPmsGuideDialog(context);
                                        } else {//有权限
                                            startUpdateService(context);
                                        }
                                    }
                                });
                    }
                })
                .setNegativeButton("取消", new FastDialog.OnClickListener() {
                    @Override
                    public void onClick(FastDialog dialog) {
                    }
                })
                .create().show();

    }

    private static void showPmsGuideDialog(final Context context) {
        new FastDialog(context)
                .setTitle("提醒")
                .setContent("权限缺失可能导致某些功能无法正常使用。\n请前往\"设置\"-\"权限管理\"-打开所需权限。")
                .setPositiveButton("设置", new FastDialog.OnClickListener() {
                    @Override
                    public void onClick(FastDialog dialog) {
                        startAppSetting(context);
                    }
                })
                .setNegativeButton("退出", new FastDialog.OnClickListener() {
                    @Override
                    public void onClick(FastDialog dialog) {
                    }
                })
                .create().show();
    }

    private static void startAppSetting(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    private static void startUpdateService(Context context) {
        Intent intent = new Intent(context, UpdateService.class);
        intent.putExtra("url", mUpdateUrl);
        context.startService(intent);
    }
}
