package com.elapse.democamerax.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.elapse.democamerax.R;

/**
 * author : Kevin.ning
 * e-mail :
 * date   : 2019/10/16 17:43
 * desc   :
 * version: 1.0
 */
public class PermissionFragment extends Fragment {
    private static int PERMISSIONS_REQUEST_CODE = 10;
    private String[] PERMISSIONS_REQUIRED = new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};

    public PermissionFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (! hasPermissions()){
            requestPermissions(PERMISSIONS_REQUIRED, PERMISSIONS_REQUEST_CODE);
        } else {
            //todo
            // If permissions have already been granted, proceed
            replaceFragment(new CameraFragment());

        }
    }

    private void replaceFragment(Fragment fragment) {
        FragmentManager manager = getFragmentManager();
        assert manager != null;
        FragmentTransaction transaction = manager.beginTransaction();
        transaction.replace(R.id.fragment_container,fragment);
//        transaction.addToBackStack(null);
        transaction.commit();
    }

    private boolean hasPermissions(){
        for (String permission : PERMISSIONS_REQUIRED){
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED){
                return false;
            }
        }
       return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Take the user to the success fragment when permission is granted
                Toast.makeText(getContext(), "Permission request granted", Toast.LENGTH_LONG).show();
                //todo
                replaceFragment(new CameraFragment());
//                Navigation.findNavController(requireActivity(), R.id.fragment_container)
//                        .navigate(R.id.action_permissions_to_camera, null,
//                                navOptions);
            } else {
                Toast.makeText(getContext(), "Permission request denied", Toast.LENGTH_LONG).show();
            }
        }
    }

}

