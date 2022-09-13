package com.tutorial.messageme.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.tutorial.messageme.R
import com.tutorial.messageme.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var binding:ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        appBarConfiguration = AppBarConfiguration(setOf(
            R.id.recentChatsFragment,
            R.id.friendsFragment,
            R.id.userProfileFragment
        ))

        val fragHost = supportFragmentManager.findFragmentById(R.id.fragHost) as NavHostFragment
        navController = fragHost.findNavController()

        navController.addOnDestinationChangedListener{_,destination,_->
            when(destination.id){
                //TODO fix--me
                R.id.loginFragment ->supportActionBar?.hide()
                else->supportActionBar?.show()
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.isVisible = appBarConfiguration.topLevelDestinations.contains(destination.id)

        }


        setupActionBarWithNavController(navController,appBarConfiguration)
        binding.bottomNav.setupWithNavController(navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

}