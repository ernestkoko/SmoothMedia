package com.koko.smoothmedia.screens.homepage.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.koko.smoothmedia.R

/**
 * A simple [Fragment] subclass.
 * Use the [PermissionFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class PermissionFragment : Fragment() {
    private val TAG="PermissionFragment"

    // private val permissionViewModel: PermissionViewModel by activityViewModels()

    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    //private var fragmentTransaction:FragmentTransaction?= null
    companion object {
        const val PERMISSION_SUCCESSFUL = "PERMISSION_SUCCESSFUL"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.i(TAG,"onCreate: Called")


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val myView = inflater.inflate(R.layout.fragment_permission, container, false)



        requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()


        ) { isGranted: Boolean ->
            if (isGranted) {
                //permission granted. Continue with the ui flow
                //  permissionViewModel.setPermissionTrue()
                navigate()


            } else {
                // permissionViewModel.setPermissionFalse()
            }


        }

        requestPermission()

        return myView
    }

    private fun navigate() {
        val navController = this.findNavController()

       // navController.navigate(R.id.action_permissionFragment_to_view_pager)
    }


    private fun requestPermission() {
        val permission = Manifest.permission.READ_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when {
                ContextCompat.checkSelfPermission(
                    requireContext(),
                    permission
                ) == PackageManager.PERMISSION_GRANTED -> {
                    //  permissionViewModel.setPermissionTrue()
                    navigate()


                }
                shouldShowRequestPermissionRationale(permission) -> {
                    //display an educational ui
                    // permissionViewModel.setPermissionFalse()
                }
                else -> {
                    //request permission
                    requestPermissionLauncher.launch(permission)
                }
            }
        }

    }


}