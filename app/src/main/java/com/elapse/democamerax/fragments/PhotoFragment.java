package com.elapse.democamerax.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;

import java.io.File;
import java.util.Objects;

/**
 * author : Kevin.ning
 * e-mail :
 * date   : 2019/10/16 17:43
 * desc   :viewpager页面，用于展示图片
 * version: 1.0
 */
public class PhotoFragment extends Fragment {

    private static final String FILE_NAME_KEY = "file_name";

    public PhotoFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return new ImageView(getContext());
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle arguments = getArguments();
        if (arguments != null){
            File file = new File(Objects.requireNonNull(arguments.getString(FILE_NAME_KEY)));
            Log.e("PhotoFragment", "getItem: "+file.getAbsolutePath() );
            Glide.with(this).load(file).into((ImageView) view);
        }
    }

    static PhotoFragment create(File image){
        PhotoFragment fragment = new PhotoFragment();
        Bundle arguments = new Bundle();
        arguments.putString(FILE_NAME_KEY,image.getAbsolutePath());
        Log.e("create", ""+image.getAbsolutePath());
        fragment.setArguments(arguments);
        return fragment;
    }
}
