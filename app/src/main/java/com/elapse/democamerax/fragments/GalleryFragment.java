package com.elapse.democamerax.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.DisplayCutout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.elapse.democamerax.BuildConfig;
import com.elapse.democamerax.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * author : Kevin.ning
 * e-mail :
 * date   : 2019/10/16 17:42
 * desc   :
 * version: 1.0
 */
public class GalleryFragment extends Fragment {
    static final String KEY_ROOT_DIRECTORY = "root_folder";
//    public static final String[] EXTENSION_WHITELIST = new String[]{"JPG"};
    private int FLAGS_FULLSCREEN =
            View.SYSTEM_UI_FLAG_LOW_PROFILE |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    public GalleryFragment() {

    }

    private List<File> mediaList;
    private ViewPager mediaViewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Mark this as a retain fragment, so the lifecycle does not get restarted on config change
        setRetainInstance(true);
        Bundle arguments = getArguments();
        if (arguments != null) {
            File rootDirectory = new File(Objects.requireNonNull(arguments.getString(KEY_ROOT_DIRECTORY)));
            mediaList = new ArrayList<>(Arrays.asList(Objects.requireNonNull(rootDirectory.listFiles())));
            //将图片按拍摄时间逆序排列，处理仓促，方法有待改进
            Collections.sort(mediaList, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    String s1 = f1.getName().substring(0, f1.getName().lastIndexOf("."));
                    String s2 = f2.getName().substring(0, f2.getName().lastIndexOf("."));
                    try {
                        Long l1 = Long.valueOf(s1);
                        Long l2 = Long.valueOf(s2);
                        return (int) (l2 - l1);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return 0;
                    }
                }
            });
            // Populate the ViewPager and implement a cache of two media items
            mediaViewPager = view.findViewById(R.id.photo_view_pager);
            mediaViewPager.setOffscreenPageLimit(2);
            mediaViewPager.setAdapter(new MediaPagerAdapter(getChildFragmentManager()));
        }

        // Make sure that the cutout "safe area" avoids the screen notch if any
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // Use extension method to pad "inside" view containing UI using display cutout's bounds
            View view1 = view.findViewById(R.id.cutout_safe_area);
            WindowInsets insets = view1.getRootWindowInsets();
            if (insets != null) {
                DisplayCutout cutout = insets.getDisplayCutout();
                if (cutout != null) {
                    doPadding(view1, cutout);
                }
            }
        }

        // Handle back button press
        view.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentManager fragmentManager = getFragmentManager();
                if (fragmentManager != null) {
                    fragmentManager.popBackStack();
                }
            }
        });

        // Handle share button press
        view.findViewById(R.id.share_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Make sure that we have a file to share
                File mediaFile = mediaList.get(mediaViewPager.getCurrentItem());
                if (mediaFile != null) {
                    Context applicationContext = requireContext().getApplicationContext();
                    Intent intent = new Intent();
                    String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                            mediaFile.getName().substring(mediaFile.getName().lastIndexOf(".")));
                    Uri uri = FileProvider.getUriForFile(
                            applicationContext, BuildConfig.APPLICATION_ID + ".provider", mediaFile);
                    intent.putExtra(Intent.EXTRA_STREAM, uri);
                    intent.setType(mimeType);
                    intent.setAction(Intent.ACTION_SEND);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    // Launch the intent letting the user choose which app to share with
                    startActivity(Intent.createChooser(intent, getString(R.string.share_hint)));
                }
            }
        });

        // Handle delete button press
        view.findViewById(R.id.delete_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = requireContext();
                AlertDialog alertDialog = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog)
                        .setTitle(getString(R.string.delete_title))
                        .setMessage(getString(R.string.delete_dialog))
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                File mediaFile = mediaList.get(mediaViewPager.getCurrentItem());
                                if (mediaFile != null) {
                                    mediaFile.delete();

                                    mediaList.remove(mediaViewPager.getCurrentItem());

                                    mediaViewPager.getAdapter().notifyDataSetChanged();
                                    if (mediaList.isEmpty()) {
                                        getFragmentManager().popBackStack();
                                    }
                                }
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();
                Window window = alertDialog.getWindow();
                if (window != null) {
                    window.setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                    window.getDecorView().setSystemUiVisibility(FLAGS_FULLSCREEN);
                }
                // Show the dialog while still in immersive mode
                alertDialog.show();
                // Set the dialog to focusable again
                window.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.P)
    private void doPadding(View v, DisplayCutout cutout) {
        v.setPadding(cutout.getSafeInsetLeft(),
                cutout.getSafeInsetTop(),
                cutout.getSafeInsetRight(),
                cutout.getSafeInsetBottom());
    }

    class MediaPagerAdapter extends FragmentStatePagerAdapter {

        private static final String TAG = "MediaPagerAdapter";
        MediaPagerAdapter(@NonNull FragmentManager fm) {
            super(fm);
        }

        @NonNull
        @Override
        public Fragment getItem(int position) {
            File file = mediaList.get(position);
            Log.e(TAG, "getItem: "+file.getAbsolutePath());
            return PhotoFragment.create(file);
        }

        @Override
        public int getCount() {
            return mediaList.size();
        }

        @Override
        public int getItemPosition(@NonNull Object object) {
            return POSITION_NONE;
        }
    }
}
