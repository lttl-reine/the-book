package com.example.thebook.ui.home

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.thebook.R
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeFragment : Fragment() {
    private lateinit var navController: NavController
    private lateinit var bottomNavigationView: BottomNavigationView // Khai báo biến này

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val navHostFragment = childFragmentManager.findFragmentById(R.id.bottom_nav_host) as NavHostFragment
        navController = navHostFragment.navController

        bottomNavigationView = view.findViewById<BottomNavigationView>(R.id.bottom_navigation) // Gán giá trị
        bottomNavigationView.setupWithNavController(navController)

        // Hide bottom navigation menu for some fragment
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.addBookFragment,
                R.id.bookDetailFragment,
                R.id.readerFragment -> {
                    hideBottomNavigationView()
                }
                else -> {
                    showBottomNavigationView()
                }
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            if (navController.currentDestination?.id != R.id.homeTabFragment) {
                navController.navigate(R.id.homeTabFragment)
            } else {
                requireActivity().finish()
            }
        }
    }

    // Hide BottomNavigationView
    private fun hideBottomNavigationView() {
        if (bottomNavigationView.visibility == View.VISIBLE) {
            bottomNavigationView.animate()
                .translationY(bottomNavigationView.height.toFloat())
                .setDuration(200)
                .withEndAction {
                    bottomNavigationView.visibility = View.GONE
                }
                .start()
        }
    }

    // Hàm để hiện BottomNavigationView
    private fun showBottomNavigationView() {
        if (bottomNavigationView.visibility == View.GONE) {
            bottomNavigationView.visibility = View.VISIBLE
            bottomNavigationView.animate()
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }
}