package com.project.dimediaryapp.activities

import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.TextView
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.project.dimediaryapp.R
import com.project.dimediaryapp.databinding.ActivityNavigationDrawerBinding
import com.project.dimediaryapp.util.FirestoreUtil
import com.project.dimediaryapp.util.PreferenceHelper

class NavigationDrawerActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityNavigationDrawerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNavigationDrawerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarNavigationDrawer.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_navigation_drawer)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_expenses, R.id.nav_budget
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Fetch and set user data in the navigation header
        val headerView: View = navView.getHeaderView(0)
        val nameTextView: TextView = headerView.findViewById(R.id.nav_header_name)
        val emailTextView: TextView = headerView.findViewById(R.id.nav_header_email)

        // Get the user ID from shared preferences
        val userId = PreferenceHelper.getUserId(this)

        if (!userId.isNullOrEmpty()) {
            val firestoreUtil = FirestoreUtil(this)
            firestoreUtil.readDocument("users", userId, { document ->
                if (document != null) {
                    val name = document["displayName"] as? String
                    val email = document["email"] as? String

                    // Set the name and email to the TextViews
                    nameTextView.text = name
                    emailTextView.text = email
                }
            }, { e ->
                // Handle the error
            })
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.navigation_drawer, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_navigation_drawer)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
