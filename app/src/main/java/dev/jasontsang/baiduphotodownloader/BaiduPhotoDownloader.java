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

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import dalvik.system.DexFile;
import dalvik.system.PathClassLoader;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.callbacks.XCallback;

import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findConstructorExact;

public class BaiduPhotoDownloader implements IXposedHookLoadPackage {
    private static boolean DYNAMIC = false;

    private static List<String> TARGET = Arrays.asList(
            "com.baidu.youavideo"
    );

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (TARGET.contains(lpparam.packageName)) {
            Constructor<?> dexFileConstructor = findConstructorExact(DexFile.class, ByteBuffer[].class, ClassLoader.class, Class.forName("[Ldalvik.system.DexPathList$Element;", false, lpparam.classLoader));
            XposedBridge.hookMethod(dexFileConstructor, new XC_MethodHook(XCallback.PRIORITY_HIGHEST) {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    ByteBuffer[] bufs = (ByteBuffer[]) param.args[0];
                    for (ByteBuffer buf : bufs) {
                        digest.update(buf.array());
                    }
                    String hex = Hex.encodeHexString(digest.digest());

                    log(lpparam.packageName + " dump " + hex);

                    File baseDir = new File("/data/data/" + lpparam.packageName + "/dump");
                    baseDir.mkdirs();
                    File dexFile = new File(baseDir, hex + ".dex");
                    if (!dexFile.exists()) {
                        FileOutputStream os = new FileOutputStream(dexFile, true);
                        for (ByteBuffer buf : bufs) {
                            os.write(buf.array());
                        }
                        os.flush();
                        os.close();
                    }
                }
            });

            File baseDir = new File("/data/data/" + lpparam.packageName + "/xposed");
            FileUtils.deleteDirectory(baseDir);
            baseDir.mkdirs();

            findAndHookMethod("android.app.Application", lpparam.classLoader, "attach", Context.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    Context context = (Context) param.args[0];

                    log(lpparam.packageName + " attach ---> " + context + ", " + context.getClassLoader() + "@code" + context.getClassLoader().hashCode());

                    if (DYNAMIC) {
                        String path = context.createPackageContext("dev.jasontsang.elderdriver", Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY).getPackageCodePath();
                        PathClassLoader pathClassLoader = new PathClassLoader(path, XposedBridge.BOOTCLASSLOADER);
                        Class<?> undressClass = Class.forName("dev.jasontsang.elderdriver.ElderDriver", true, pathClassLoader);
                        Object undress = undressClass.newInstance();
                        Method handleLoadPackageMethod = undressClass.getDeclaredMethod("handleLoadPackage", lpparam.getClass(), Context.class);
                        handleLoadPackageMethod.setAccessible(true);
                        handleLoadPackageMethod.invoke(undress, lpparam, context);
                    } else {
                        handleLoadPackage(lpparam, context);
                    }
                }
            });
        }
    }

    private void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam, final Context context) throws Throwable {
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

    private void youaVideo(final XC_LoadPackage.LoadPackageParam lpparam, final Context context) throws Throwable {
        final ClassLoader classLoader = context.getClassLoader();

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
