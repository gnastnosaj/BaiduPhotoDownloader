package dev.jasontsang.baiduphotodownloader;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Arrays;
import java.util.List;

import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;

public class BaiduPhotoDownloader implements IXposedHookLoadPackage {
    private static boolean DYNAMIC = false;

    private static List<String> TARGET = Arrays.asList(
            "com.baidu.youavideo"
    );

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (TARGET.contains(lpparam.packageName)) {

            final File baseDir = new File("/data/data/" + lpparam.packageName + "/xposed");
            FileUtils.deleteDirectory(baseDir);
            baseDir.mkdirs();

            final XC_MethodHook xc_methodHook = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[2];

                    if (DYNAMIC) {
                        String path = context.createPackageContext("dev.jasontsang.baiduphotodownloader", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY).getPackageCodePath();
                        PathClassLoader pathClassLoader = new PathClassLoader(path, XposedBridge.BOOTCLASSLOADER);
                        Class<?> undressClass = Class.forName("dev.jasontsang.baiduphotodownloader.BaiduPhotoDownloader", true, pathClassLoader);
                        Object undress = undressClass.newInstance();
                        Method handleLoadPackageMethod = undressClass.getDeclaredMethod("handleLoadPackage", lpparam.getClass(), Context.class);
                        handleLoadPackageMethod.setAccessible(true);
                        handleLoadPackageMethod.invoke(undress, lpparam, context);
                    } else {
                        handleLoadPackage(lpparam, context);
                    }
                }
            };

            try {
                findAndHookMethod("android.app.Instrumentation", lpparam.classLoader, "newApplication", ClassLoader.class, String.class, Context.class, xc_methodHook);
            } catch (Throwable throwable) {
                log(throwable.getMessage());
            }

            try {
                findAndHookMethod("android.app.Application", lpparam.classLoader, "attach", Context.class, xc_methodHook);
            } catch (Throwable throwable) {
                log(throwable.getMessage());
            }
        }
    }

    private void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam,
                                   final Context context) throws Throwable {
        File stamp = new File("/data/data/" + lpparam.packageName + "/xposed/" + context);
        RandomAccessFile raf = new RandomAccessFile(stamp, "rw");
        FileChannel channel = raf.getChannel();
        FileLock lock = channel.lock();

        if (channel.size() == 0) {
            log(lpparam.packageName + " handleLoadPackage ---> " + context + ", " + context.getClassLoader() + "@code" + context.getClassLoader().hashCode());

            switch (lpparam.packageName) {
                case "com.baidu.youavideo":
                    youaVideo(lpparam, context);
                    break;
            }

            channel.write(ByteBuffer.wrap((context.getClassLoader() + "@code" + context.getClassLoader().hashCode()).getBytes()));
        }

        lock.release();
        channel.close();
        raf.close();
    }

    private void youaVideo(final XC_LoadPackage.LoadPackageParam lpparam,
                           final Context context) throws Throwable {
        final ClassLoader classLoader = context.getClassLoader();

        Toast.makeText(context, "hooked", Toast.LENGTH_LONG).show();

        findAndHookMethod("com.google.android.material.bottomsheet.BottomSheetDialog", classLoader, "wrapInBottomSheet", int.class, View.class, ViewGroup.LayoutParams.class, new XC_MethodHook() {
            @SuppressLint("ResourceType")
            @Override
            protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                final LinearLayout container = (LinearLayout) ((ViewGroup) ((View) param.getResult()).findViewById(2131296445).findViewById(2131296476)).getChildAt(0);
                final View download = container.getChildAt(0);
                container.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        download.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                try {
                                    Object homeActivity = ((ContextThemeWrapper) ((Dialog) param.thisObject).getContext()).getBaseContext();
                                    Field currentFragmentField = homeActivity.getClass().getDeclaredField("currentFragment");
                                    currentFragmentField.setAccessible(true);
                                    Object currentFragment = currentFragmentField.get(homeActivity);
                                    Field timelineViewModelField = currentFragment.getClass().getDeclaredField("timelineViewModel");
                                    timelineViewModelField.setAccessible(true);
                                    Object timelineViewModel = timelineViewModelField.get(currentFragment);

                                    Object businessViewModelFactoryCompanion = Class.forName("com.baidu.mars.united.business.core.BusinessViewModelFactory", false, classLoader).getDeclaredField("Companion").get(null);
                                    Object businessViewModelFactory = businessViewModelFactoryCompanion.getClass().getDeclaredMethod("getInstance", Class.forName("com.baidu.mars.united.business.core.BaseApplication", false, classLoader)).invoke(businessViewModelFactoryCompanion, ((Activity) homeActivity).getApplication());
                                    Object viewModelProvider = Class.forName("androidx.lifecycle.v", false, classLoader).getDeclaredMethod("a", Class.forName("androidx.fragment.app.FragmentActivity", false, classLoader), Class.forName("androidx.lifecycle.ViewModelProvider$Factory", false, classLoader)).invoke(null, homeActivity, businessViewModelFactory);
                                    Object downloadViewModel = viewModelProvider.getClass().getDeclaredMethod("a", Class.class).invoke(viewModelProvider, Class.forName("com.baidu.youavideo.download.viewmodel.DownloadViewModel", false, classLoader));
                                    Method downloadMethod = downloadViewModel.getClass().getDeclaredMethod("download", Class.forName("androidx.lifecycle.LifecycleOwner", false, classLoader), Context.class, int.class, String.class, String.class, String.class, int.class, String.class, int.class, long.class, Class.forName("kotlin.jvm.functions.Function0", false, classLoader));
                                    //Method addDownloadTaskProgressListenerMethod = downloadViewModel.getClass().getDeclaredMethod("addDownloadTaskProgressListener", Class.forName("androidx.fragment.app.FragmentActivity", false, classLoader), Class.forName("androidx.lifecycle.LifecycleOwner", false, classLoader), String.class, Class.forName("kotlin.jvm.functions.Function0", false, classLoader));
                                    Object nothing = Class.forName("com.baidu.youavideo.preview.ui.MaterialPreviewActivity$downloadFile$1$1", false, classLoader).getDeclaredField("INSTANCE").get(null);

                                    List selectedData = (List) timelineViewModel.getClass().getDeclaredMethod("getSelectedData").invoke(timelineViewModel);
                                    for (Object timeLineMedia : selectedData) {
                                        String serverPath = (String) XposedHelpers.callMethod(timeLineMedia, "getServerPath");
                                        Field localIdField = timeLineMedia.getClass().getDeclaredField("localId");
                                        localIdField.setAccessible(true);
                                        Long localId = (Long) localIdField.get(timeLineMedia);
                                        if (localId == 0) {
                                            downloadMethod.invoke(downloadViewModel, homeActivity, homeActivity,
                                                    1,
                                                    String.valueOf(XposedHelpers.callMethod(timeLineMedia, "getFsid")),
                                                    serverPath.substring(serverPath.lastIndexOf("/") + 1),
                                                    "",
                                                    1,
                                                    XposedHelpers.callMethod(timeLineMedia, "getPcsMd5"),
                                                    XposedHelpers.callMethod(timeLineMedia, "getCategory"),
                                                    XposedHelpers.callMethod(timeLineMedia, "getDateTaken"),
                                                    nothing
                                            );
                                            //addDownloadTaskProgressListenerMethod.invoke(downloadViewModel, homeActivity, homeActivity, String.valueOf(XposedHelpers.callMethod(timeLineMedia, "getFsid")), nothing);
                                        }
                                    }

                                    ((Dialog) param.thisObject).dismiss();
                                    Method exitSelectableModeMethod = currentFragment.getClass().getDeclaredMethod("exitSelectableMode");
                                    exitSelectableModeMethod.setAccessible(true);
                                    exitSelectableModeMethod.invoke(currentFragment);
                                    Toast.makeText(context, context.getString(2131689967, Class.forName("com.baidu.mars.united.business.core.util.file.FileExtKt", false, classLoader).getDeclaredMethod("getDownloadNormalFileDir").invoke(null)), Toast.LENGTH_LONG).show();
                                } catch (Throwable throwable) {
                                    Toast.makeText(context, "Error --> " + throwable.getMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                        download.setVisibility(View.VISIBLE);
                    }
                }, 100);
            }
        });
    }

    private void log(String message) {
        if (DYNAMIC) {
            XposedBridge.log(message);
        }
    }
}
